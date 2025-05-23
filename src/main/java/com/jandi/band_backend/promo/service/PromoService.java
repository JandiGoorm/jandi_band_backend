package com.jandi.band_backend.promo.service;

import com.jandi.band_backend.club.entity.Club;
import com.jandi.band_backend.club.repository.ClubRepository;
import com.jandi.band_backend.global.exception.ResourceNotFoundException;
import com.jandi.band_backend.image.S3Service;
import com.jandi.band_backend.promo.dto.PromoRequest;
import com.jandi.band_backend.promo.dto.PromoResponse;
import com.jandi.band_backend.promo.entity.Promo;
import com.jandi.band_backend.promo.entity.PromoPhoto;
import com.jandi.band_backend.promo.repository.PromoRepository;
import com.jandi.band_backend.user.entity.Users;
import com.jandi.band_backend.user.repository.UserRepository;
import com.jandi.band_backend.club.repository.ClubMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PromoService {

    private final PromoRepository promoRepository;
    private final ClubRepository clubRepository;
    private final UserRepository userRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final S3Service s3Service;
    private static final String PROMO_PHOTO_DIR = "promo-photo";

    // 공연 홍보 목록 조회
    public Page<PromoResponse> getPromos(Pageable pageable) {
        return promoRepository.findAllNotDeleted(pageable)
                .map(PromoResponse::from);
    }

    // 클럽별 공연 홍보 목록 조회
    public Page<PromoResponse> getPromosByClub(Integer clubId, Pageable pageable) {
        return promoRepository.findAllByClubId(clubId, pageable)
                .map(PromoResponse::from);
    }

    // 공연 홍보 상세 조회
    @Transactional
    public PromoResponse getPromo(Integer promoId) {
        Promo promo = promoRepository.findByIdAndNotDeleted(promoId);
        if (promo == null) {
            throw new ResourceNotFoundException("공연 홍보를 찾을 수 없습니다.");
        }
        
        // 조회수 증가
        promo.setViewCount(promo.getViewCount() + 1);
        return PromoResponse.from(promo);
    }

    // 공연 홍보 생성
    @Transactional
    public PromoResponse createPromo(PromoRequest request, Integer creatorId) {
        Club club = clubRepository.findById(request.getClubId())
                .orElseThrow(() -> new ResourceNotFoundException("클럽을 찾을 수 없습니다."));
        
        Users creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));

        // 클럽 멤버십 검증 추가
        if (!clubMemberRepository.existsByClubAndUser(club, creator)) {
            throw new IllegalStateException("클럽 멤버만 공연 홍보를 생성할 수 있습니다.");
        }

        Promo promo = new Promo();
        promo.setClub(club);
        promo.setCreator(creator);
        promo.setTitle(request.getTitle());
        promo.setAdmissionFee(request.getAdmissionFee());
        promo.setEventDatetime(request.getEventDatetime());
        promo.setLocation(request.getLocation());
        promo.setAddress(request.getAddress());
        promo.setDescription(request.getDescription());
        promo.setStatus(request.getStatus() != null ? request.getStatus() : Promo.PromoStatus.UPCOMING);

        return PromoResponse.from(promoRepository.save(promo));
    }

    // 공연 홍보 수정
    @Transactional
    public PromoResponse updatePromo(Integer promoId, PromoRequest request, Integer userId) {
        Promo promo = promoRepository.findByIdAndNotDeleted(promoId);
        if (promo == null) {
            throw new ResourceNotFoundException("공연 홍보를 찾을 수 없습니다.");
        }

        // 권한 체크
        if (!promo.getCreator().getId().equals(userId)) {
            throw new IllegalStateException("공연 홍보를 수정할 권한이 없습니다.");
        }

        // 클럽 변경 시 새로운 클럽 조회
        if (!promo.getClub().getId().equals(request.getClubId())) {
            Club newClub = clubRepository.findById(request.getClubId())
                    .orElseThrow(() -> new ResourceNotFoundException("클럽을 찾을 수 없습니다."));
            promo.setClub(newClub);
        }

        promo.setTitle(request.getTitle());
        promo.setAdmissionFee(request.getAdmissionFee());
        promo.setEventDatetime(request.getEventDatetime());
        promo.setLocation(request.getLocation());
        promo.setAddress(request.getAddress());
        promo.setDescription(request.getDescription());
        if (request.getStatus() != null) {
            promo.setStatus(request.getStatus());
        }

        return PromoResponse.from(promo);
    }

    // 공연 홍보 삭제 (소프트 삭제)
    @Transactional
    public void deletePromo(Integer promoId, Integer userId) {
        Promo promo = promoRepository.findByIdAndNotDeleted(promoId);
        if (promo == null) {
            throw new ResourceNotFoundException("공연 홍보를 찾을 수 없습니다.");
        }

        // 권한 체크
        if (!promo.getCreator().getId().equals(userId)) {
            throw new IllegalStateException("공연 홍보를 삭제할 권한이 없습니다.");
        }

        promo.setDeletedAt(LocalDateTime.now());
    }

    // 공연 홍보 이미지 업로드
    @Transactional
    public String uploadPromoImage(Integer promoId, MultipartFile image, Integer userId) throws IOException {
        Promo promo = promoRepository.findByIdAndNotDeleted(promoId);
        if (promo == null) {
            throw new ResourceNotFoundException("공연 홍보를 찾을 수 없습니다.");
        }

        // 권한 체크
        if (!promo.getCreator().getId().equals(userId)) {
            throw new IllegalStateException("공연 홍보 이미지를 업로드할 권한이 없습니다.");
        }

        // S3에 이미지 업로드
        String imageUrl = s3Service.uploadImage(image, PROMO_PHOTO_DIR);

        // PromoPhoto 엔티티 생성 및 저장
        PromoPhoto photo = new PromoPhoto();
        photo.setPromo(promo);
        photo.setUploader(promo.getCreator());
        photo.setImageUrl(imageUrl);
        photo.setIsCurrent(true);

        // 기존 현재 이미지가 있다면 isCurrent를 false로 변경
        promo.getPhotos().stream()
                .filter(p -> p.getIsCurrent())
                .forEach(p -> p.setIsCurrent(false));

        promo.getPhotos().add(photo);
        return imageUrl;
    }

    // 공연 홍보 이미지 삭제
    @Transactional
    public void deletePromoImage(Integer promoId, String imageUrl, Integer userId) {
        Promo promo = promoRepository.findByIdAndNotDeleted(promoId);
        if (promo == null) {
            throw new ResourceNotFoundException("공연 홍보를 찾을 수 없습니다.");
        }

        // 권한 체크
        if (!promo.getCreator().getId().equals(userId)) {
            throw new IllegalStateException("공연 홍보 이미지를 삭제할 권한이 없습니다.");
        }

        // 이미지 찾기
        PromoPhoto photo = promo.getPhotos().stream()
                .filter(p -> p.getImageUrl().equals(imageUrl))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("이미지를 찾을 수 없습니다."));

        // S3에서 이미지 삭제
        s3Service.deleteImage(imageUrl);

        // DB에서 이미지 정보 삭제 (소프트 삭제)
        photo.setDeletedAt(LocalDateTime.now());
    }

    // 공연 상태 자동 업데이트 (스케줄러로 주기적 실행)
    @Scheduled(cron = "0 0 * * * *")  // 매시 정각에 실행
    @Transactional
    public void updatePromoStatuses() {
        LocalDateTime now = LocalDateTime.now();
        
        // 진행 중인 공연 업데이트
        List<Promo> ongoingPromos = promoRepository.findByStatusAndEventDatetimeBefore(
            Promo.PromoStatus.UPCOMING, now);
        for (Promo promo : ongoingPromos) {
            promo.setStatus(Promo.PromoStatus.ONGOING);
        }
        
        // 완료된 공연 업데이트 (공연 종료 후 3시간 경과)
        List<Promo> completedPromos = promoRepository.findByStatusAndEventDatetimeBefore(
            Promo.PromoStatus.ONGOING, now.minus(3, ChronoUnit.HOURS));
        for (Promo promo : completedPromos) {
            promo.setStatus(Promo.PromoStatus.COMPLETED);
        }
    }

    // 공연 홍보 검색
    public Page<PromoResponse> searchPromos(String keyword, Pageable pageable) {
        return promoRepository.searchByKeyword(keyword, pageable)
                .map(PromoResponse::from);
    }

    // 공연 홍보 필터링
    public Page<PromoResponse> filterPromos(
            Promo.PromoStatus status,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Integer clubId,
            Pageable pageable) {
        return promoRepository.filterPromos(status, startDate, endDate, clubId, pageable)
                .map(PromoResponse::from);
    }
} 
