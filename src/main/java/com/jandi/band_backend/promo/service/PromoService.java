package com.jandi.band_backend.promo.service;

import com.jandi.band_backend.global.exception.ResourceNotFoundException;
import com.jandi.band_backend.global.exception.BadRequestException;

import com.jandi.band_backend.promo.dto.PromoReqDTO;
import com.jandi.band_backend.promo.dto.PromoRespDTO;
import com.jandi.band_backend.promo.dto.PromoSimpleRespDTO;
import com.jandi.band_backend.promo.entity.Promo;
import com.jandi.band_backend.promo.entity.PromoPhoto;
import com.jandi.band_backend.promo.repository.PromoRepository;
import com.jandi.band_backend.promo.repository.PromoPhotoRepository;
import com.jandi.band_backend.user.entity.Users;
import com.jandi.band_backend.global.util.PermissionValidationUtil;
import com.jandi.band_backend.global.util.UserValidationUtil;
import com.jandi.band_backend.global.util.R2FileManagementUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PromoService {

    private final PromoRepository promoRepository;
    private final PromoPhotoRepository promoPhotoRepository;
    private final PromoLikeService promoLikeService;
    private final PermissionValidationUtil permissionValidationUtil;
    private final UserValidationUtil userValidationUtil;
    private final R2FileManagementUtil r2FileManagementUtil;
    private static final String PROMO_PHOTO_DIR = "promo-photo";

    // 공연 홍보 목록 조회
    public Page<PromoRespDTO> getPromos(Pageable pageable) {
        return promoRepository.findAllSortedByEventDatetime(pageable)
                .map(PromoRespDTO::from);
    }

    // 공연 홍보 목록 조회 (사용자별 좋아요 상태 포함)
    public Page<PromoRespDTO> getPromos(Integer userId, Pageable pageable) {
        return promoRepository.findAllSortedByEventDatetime(pageable)
                .map(promo -> {
                    Boolean isLikedByUser = userId != null ? 
                            promoLikeService.isLikedByUser(promo.getId(), userId) : null;
                    return PromoRespDTO.from(promo, isLikedByUser);
                });
    }

    // 공연 홍보 상세 조회
    @Transactional
    public PromoRespDTO getPromo(Integer promoId) {
        Promo promo = promoRepository.findByIdAndNotDeleted(promoId);
        if (promo == null) {
            throw new ResourceNotFoundException("공연 홍보를 찾을 수 없습니다.");
        }
        
        // 조회수 증가
        promo.setViewCount(promo.getViewCount() + 1);
        return PromoRespDTO.from(promo);
    }

    // 공연 홍보 상세 조회 (사용자별 좋아요 상태 포함)
    @Transactional
    public PromoRespDTO getPromo(Integer promoId, Integer userId) {
        Promo promo = promoRepository.findByIdAndNotDeleted(promoId);
        if (promo == null) {
            throw new ResourceNotFoundException("공연 홍보를 찾을 수 없습니다.");
        }
        
        // 조회수 증가
        promo.setViewCount(promo.getViewCount() + 1);
        
        // 사용자의 좋아요 상태 확인
        Boolean isLikedByUser = userId != null ? promoLikeService.isLikedByUser(promoId, userId) : null;
        
        return PromoRespDTO.from(promo, isLikedByUser);
    }

    // 공연 홍보 생성 (이미지 포함)
    @Transactional
    public PromoSimpleRespDTO createPromo(PromoReqDTO request, Integer creatorId) {
        Users creator = userValidationUtil.getUserById(creatorId);

        // 관람료 음수값 검증
        if (request.getAdmissionFee() != null && request.getAdmissionFee().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("관람료는 음수가 될 수 없습니다.");
        }

        Promo promo = new Promo();
        promo.setTeamName(request.getTeamName());
        promo.setCreator(creator);
        promo.setTitle(request.getTitle());
        promo.setAdmissionFee(request.getAdmissionFee());
        promo.setEventDatetime(request.getEventDatetime());
        promo.setLocation(request.getLocation());
        promo.setAddress(request.getAddress());
        promo.setDescription(request.getDescription());
        promo.setLatitude(request.getLatitude());
        promo.setLongitude(request.getLongitude());

        Promo savedPromo = promoRepository.save(promo);

        // 이미지 처리
        if (request.getImage() != null && !request.getImage().isEmpty()) {
            processImage(savedPromo, request.getImage(), creator);
        }

        return PromoSimpleRespDTO.of(savedPromo.getId());
    }

    // 공연 홍보 수정
    @Transactional
    public void updatePromo(Integer promoId, PromoReqDTO request, Integer userId) {
        Promo promo = promoRepository.findForUpdate(promoId);
        if (promo == null) {
            throw new ResourceNotFoundException("공연 홍보를 찾을 수 없습니다.");
        }

        // 권한 체크
        permissionValidationUtil.validateContentOwnership(promo.getCreator().getId(), userId, "공연 홍보를 수정할 권한이 없습니다.");

        // 관람료 음수값 검증 (수정 시에도 적용)
        if (request.getAdmissionFee() != null && request.getAdmissionFee().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("관람료는 음수가 될 수 없습니다.");
        }

        // 전송된 필드만 수정 (PATCH 방식)
        if (request.getTeamName() != null) {
            promo.setTeamName(request.getTeamName());
        }
        if (request.getTitle() != null) {
            promo.setTitle(request.getTitle());
        }
        if (request.getAdmissionFee() != null) {
            promo.setAdmissionFee(request.getAdmissionFee());
        }
        if (request.getEventDatetime() != null) {
            promo.setEventDatetime(request.getEventDatetime());
        }
        if (request.getLocation() != null) {
            promo.setLocation(request.getLocation());
        }
        if (request.getAddress() != null) {
            promo.setAddress(request.getAddress());
        }
        if (request.getLatitude() != null) {
            promo.setLatitude(request.getLatitude());
        }
        if (request.getLongitude() != null) {
            promo.setLongitude(request.getLongitude());
        }
        if (request.getDescription() != null) {
            promo.setDescription(request.getDescription());
        }

        // 기존 이미지 삭제 처리
        if (request.getDeleteImageUrl() != null && !request.getDeleteImageUrl().trim().isEmpty()) {
            deleteImage(promo, request.getDeleteImageUrl());
        }

        // 새 이미지 추가 또는 기존 이미지 업데이트
        if (request.getImage() != null && !request.getImage().isEmpty()) {
            processImage(promo, request.getImage(), promo.getCreator());
        }
    }

    // 공연 홍보 삭제 (소프트 삭제)
    @Transactional
    public void deletePromo(Integer promoId, Integer userId) {
        Promo promo = promoRepository.findForUpdate(promoId);
        if (promo == null) {
            throw new ResourceNotFoundException("공연 홍보를 찾을 수 없습니다.");
        }

        // 권한 체크
        permissionValidationUtil.validateContentOwnership(promo.getCreator().getId(), userId, "공연 홍보를 삭제할 권한이 없습니다.");

        // 관련 이미지들도 소프트 삭제
        List<PromoPhoto> photos = promoPhotoRepository.findByPromoIdAndNotDeleted(promoId);
        for (PromoPhoto photo : photos) {
            photo.setDeletedAt(LocalDateTime.now());
            // R2에서도 실제 파일 삭제
            r2FileManagementUtil.deleteFileSafely(photo.getImageUrl());
        }

        promo.setDeletedAt(LocalDateTime.now());
    }

    // 단일 이미지 처리 헬퍼 메소드 - 기존 레코드 업데이트 또는 새 레코드 생성
    private void processImage(Promo promo, MultipartFile image, Users uploader) {
        var currentPhoto = promoPhotoRepository.findByPromoIdAndIsCurrentTrue(promo.getId());

        String newImageUrl = r2FileManagementUtil.uploadFile(image, PROMO_PHOTO_DIR, "공연 홍보 이미지 업로드 실패");

        if (currentPhoto.isPresent()) {
            PromoPhoto existingPhoto = currentPhoto.get();
            String oldImageUrl = existingPhoto.getImageUrl();

            r2FileManagementUtil.deleteFileSafely(oldImageUrl);

            existingPhoto.setImageUrl(newImageUrl);
            existingPhoto.setDeletedAt(null);
            existingPhoto.setUploader(uploader);
            existingPhoto.setUploadedAt(LocalDateTime.now());
            promoPhotoRepository.save(existingPhoto);
        } else {
            PromoPhoto photo = new PromoPhoto();
            photo.setPromo(promo);
            photo.setUploader(uploader);
            photo.setImageUrl(newImageUrl);
            photo.setIsCurrent(true);
            promoPhotoRepository.save(photo);
        }
    }

    // 단일 이미지 삭제 헬퍼 메소드
    private void deleteImage(Promo promo, String imageUrl) {
        PromoPhoto photo = promoPhotoRepository.findByPromoIdAndImageUrlAndNotDeleted(promo.getId(), imageUrl);
        if (photo != null) {
            photo.setDeletedAt(LocalDateTime.now());
            // R2에서도 실제 파일 삭제
            r2FileManagementUtil.deleteFileSafely(imageUrl);
            promoPhotoRepository.save(photo);
        }
    }

    // 공연 홍보 검색
    public Page<PromoRespDTO> searchPromos(String keyword, Pageable pageable) {
        return promoRepository.searchByKeyword(keyword, pageable)
                .map(PromoRespDTO::from);
    }

    // 공연 홍보 검색 (사용자별 좋아요 상태 포함)
    public Page<PromoRespDTO> searchPromos(String keyword, Integer userId, Pageable pageable) {
        return promoRepository.searchByKeyword(keyword, pageable)
                .map(promo -> {
                    Boolean isLikedByUser = userId != null ? 
                            promoLikeService.isLikedByUser(promo.getId(), userId) : null;
                    return PromoRespDTO.from(promo, isLikedByUser);
                });
    }

    // 공연 홍보 필터링
    public Page<PromoRespDTO> filterPromos(
            LocalDateTime startDate,
            LocalDateTime endDate,
            String teamName,
            Pageable pageable) {
        return promoRepository.filterPromosByTeamName(startDate, endDate, teamName, pageable)
                .map(PromoRespDTO::from);
    }

    // 공연 홍보 필터링 (사용자별 좋아요 상태 포함)
    public Page<PromoRespDTO> filterPromos(
            LocalDateTime startDate,
            LocalDateTime endDate,
            String teamName,
            Integer userId,
            Pageable pageable) {
        return promoRepository.filterPromosByTeamName(startDate, endDate, teamName, pageable)
                .map(promo -> {
                    Boolean isLikedByUser = userId != null ? 
                            promoLikeService.isLikedByUser(promo.getId(), userId) : null;
                    return PromoRespDTO.from(promo, isLikedByUser);
                });
    }

    // 공연 홍보 필터링 (지도 기반 검색)
    public Page<PromoRespDTO> filterMapPromos(BigDecimal startLatitude, BigDecimal startLongitude, BigDecimal endLatitude, BigDecimal endLongitude, Integer userId, Pageable pageable) {
        // 작은 값이 start, 큰 값이 end가 되도록 정렬
        BigDecimal minLat = startLatitude.compareTo(endLatitude) < 0 ? startLatitude : endLatitude;
        BigDecimal maxLat = startLatitude.compareTo(endLatitude) > 0 ? startLatitude : endLatitude;
        BigDecimal minLng = startLongitude.compareTo(endLongitude) < 0 ? startLongitude : endLongitude;
        BigDecimal maxLng = startLongitude.compareTo(endLongitude) > 0 ? startLongitude : endLongitude;

        Page<Promo> promos = promoRepository.filterPromosInSpecArea(
                minLat, maxLat, minLng, maxLng, pageable
        );

        return promos.map(promo -> {
            Boolean isLikedByUser = userId != null ?
                    promoLikeService.isLikedByUser(promo.getId(), userId) : null;
            return PromoRespDTO.from(promo, isLikedByUser);
        });
    }

    // 공연 상태별 필터링
    public Page<PromoRespDTO> filterPromosByStatus(String status, String keyword, String teamName, Integer userId, Pageable pageable) {
        LocalDate today = LocalDateTime.now().toLocalDate();

        Page<Promo> promos;

        // 상태별 필터링
        switch (status.toLowerCase()) {
            case "ongoing":
                // 진행 중인 공연: 오늘 날짜(오늘 0시~24시 사이)와 일치하는 공연
                promos = promoRepository.findOngoingPromos(today, pageable);
                break;
            case "upcoming":
                // 예정된 공연: 오늘 이후(오늘 24시 이후)의 공연
                promos = promoRepository.findUpcomingPromos(today, pageable);
                break;
            case "ended":
                // 종료된 공연: 오늘 이전(오늘 0시 이전)의 공연
                promos = promoRepository.findEndedPromos(today, pageable);
                break;
            default:
                return Page.empty(pageable);
        }
        
        // 키워드나 팀명 필터링이 있는 경우 메모리에서 추가 필터링
        if ((keyword != null && !keyword.trim().isEmpty()) || (teamName != null && !teamName.trim().isEmpty())) {
            List<Promo> filteredContent = promos.getContent().stream()
                    .filter(promo -> {
                        boolean matchesKeyword = keyword == null || keyword.trim().isEmpty() ||
                                promo.getTitle().toLowerCase().contains(keyword.toLowerCase()) ||
                                (promo.getDescription() != null && promo.getDescription().toLowerCase().contains(keyword.toLowerCase())) ||
                                (promo.getLocation() != null && promo.getLocation().toLowerCase().contains(keyword.toLowerCase())) ||
                                promo.getTeamName().toLowerCase().contains(keyword.toLowerCase());
                        
                        boolean matchesTeamName = teamName == null || teamName.trim().isEmpty() ||
                                promo.getTeamName().toLowerCase().contains(teamName.toLowerCase());
                        
                        return matchesKeyword && matchesTeamName;
                    })
                    .collect(Collectors.toList());
            
            // 페이징 처리
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), filteredContent.size());
            List<Promo> pagedContent = filteredContent.subList(start, end);
            
            promos = new PageImpl<>(pagedContent, pageable, filteredContent.size());
        }
        
        return promos.map(promo -> {
            Boolean isLikedByUser = userId != null ?
                    promoLikeService.isLikedByUser(promo.getId(), userId) : null;
            return PromoRespDTO.from(promo, isLikedByUser);
        });
    }
}
