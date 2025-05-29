package com.jandi.band_backend.promo.service;

import com.jandi.band_backend.global.exception.ResourceNotFoundException;

import com.jandi.band_backend.promo.dto.PromoReqDTO;
import com.jandi.band_backend.promo.dto.PromoRespDTO;
import com.jandi.band_backend.promo.entity.Promo;
import com.jandi.band_backend.promo.entity.PromoPhoto;
import com.jandi.band_backend.promo.repository.PromoRepository;
import com.jandi.band_backend.promo.repository.PromoPhotoRepository;
import com.jandi.band_backend.user.entity.Users;
import com.jandi.band_backend.global.util.PermissionValidationUtil;
import com.jandi.band_backend.global.util.UserValidationUtil;
import com.jandi.band_backend.global.util.S3FileManagementUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PromoService {

    private final PromoRepository promoRepository;
    private final PromoPhotoRepository promoPhotoRepository;
    private final PromoLikeService promoLikeService;
    private final PermissionValidationUtil permissionValidationUtil;
    private final UserValidationUtil userValidationUtil;
    private final S3FileManagementUtil s3FileManagementUtil;
    private static final String PROMO_PHOTO_DIR = "promo-photo";

    // 공연 홍보 목록 조회
    public Page<PromoRespDTO> getPromos(Pageable pageable) {
        return promoRepository.findAllNotDeleted(pageable)
                .map(PromoRespDTO::from);
    }

    // 공연 홍보 목록 조회 (사용자별 좋아요 상태 포함)
    public Page<PromoRespDTO> getPromos(Integer userId, Pageable pageable) {
        return promoRepository.findAllNotDeleted(pageable)
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
    public PromoRespDTO createPromo(PromoReqDTO request, Integer creatorId) {
        Users creator = userValidationUtil.getUserById(creatorId);

        Promo promo = new Promo();
        promo.setTeamName(request.getTeamName());
        promo.setCreator(creator);
        promo.setTitle(request.getTitle());
        promo.setAdmissionFee(request.getAdmissionFee());
        promo.setEventDatetime(request.getEventDatetime());
        promo.setLocation(request.getLocation());
        promo.setAddress(request.getAddress());
        promo.setDescription(request.getDescription());

        Promo savedPromo = promoRepository.save(promo);

        // 이미지 처리
        if (request.getImage() != null && !request.getImage().isEmpty()) {
            processImage(savedPromo, request.getImage(), creator);
        }

        return PromoRespDTO.from(savedPromo);
    }

    // 공연 홍보 수정
    @Transactional
    public PromoRespDTO updatePromo(Integer promoId, PromoReqDTO request, Integer userId) {
        Promo promo = promoRepository.findByIdAndNotDeleted(promoId);
        if (promo == null) {
            throw new ResourceNotFoundException("공연 홍보를 찾을 수 없습니다.");
        }

        // 권한 체크
        permissionValidationUtil.validateContentOwnership(promo.getCreator().getId(), userId, "공연 홍보를 수정할 권한이 없습니다.");

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
        if (request.getDescription() != null) {
            promo.setDescription(request.getDescription());
        }

        // 기존 이미지 삭제 처리
        if (request.getDeleteImageUrl() != null && !request.getDeleteImageUrl().trim().isEmpty()) {
            deleteImage(promo, request.getDeleteImageUrl());
        }

        // 새 이미지 추가
        if (request.getImage() != null && !request.getImage().isEmpty()) {
            // 기존 이미지가 있으면 먼저 삭제
            List<PromoPhoto> existingPhotos = promoPhotoRepository.findByPromoIdAndNotDeleted(promoId);
            for (PromoPhoto photo : existingPhotos) {
                photo.setDeletedAt(LocalDateTime.now());
                s3FileManagementUtil.deleteFileSafely(photo.getImageUrl());
                promoPhotoRepository.save(photo);
            }
            
            processImage(promo, request.getImage(), promo.getCreator());
        }

        return PromoRespDTO.from(promo);
    }

    // 공연 홍보 삭제 (소프트 삭제)
    @Transactional
    public void deletePromo(Integer promoId, Integer userId) {
        Promo promo = promoRepository.findByIdAndNotDeleted(promoId);
        if (promo == null) {
            throw new ResourceNotFoundException("공연 홍보를 찾을 수 없습니다.");
        }

        // 권한 체크
        permissionValidationUtil.validateContentOwnership(promo.getCreator().getId(), userId, "공연 홍보를 삭제할 권한이 없습니다.");

        // 관련 이미지들도 소프트 삭제
        List<PromoPhoto> photos = promoPhotoRepository.findByPromoIdAndNotDeleted(promoId);
        for (PromoPhoto photo : photos) {
            photo.setDeletedAt(LocalDateTime.now());
            // S3에서도 실제 파일 삭제
            s3FileManagementUtil.deleteFileSafely(photo.getImageUrl());
        }

        promo.setDeletedAt(LocalDateTime.now());
    }

    // 단일 이미지 처리 헬퍼 메소드
    private void processImage(Promo promo, MultipartFile image, Users uploader) {
        // S3에 이미지 업로드
        String imageUrl = s3FileManagementUtil.uploadFile(image, PROMO_PHOTO_DIR, "공연 홍보 이미지 업로드 실패");

        // PromoPhoto 엔티티 생성 및 저장
        PromoPhoto photo = new PromoPhoto();
        photo.setPromo(promo);
        photo.setUploader(uploader);
        photo.setImageUrl(imageUrl);
        photo.setIsCurrent(true); // 단일 이미지이므로 항상 대표 이미지

        promoPhotoRepository.save(photo);
    }

    // 단일 이미지 삭제 헬퍼 메소드
    private void deleteImage(Promo promo, String imageUrl) {
        PromoPhoto photo = promoPhotoRepository.findByPromoIdAndImageUrlAndNotDeleted(promo.getId(), imageUrl);
        if (photo != null) {
            photo.setDeletedAt(LocalDateTime.now());
            // S3에서도 실제 파일 삭제
            s3FileManagementUtil.deleteFileSafely(imageUrl);
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
} 
