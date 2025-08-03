package com.jandi.band_backend.notice.service;

import com.jandi.band_backend.global.exception.ResourceNotFoundException;
import com.jandi.band_backend.global.exception.InvalidAccessException;
import com.jandi.band_backend.global.exception.BadRequestException;
import com.jandi.band_backend.notice.dto.NoticeReqDTO;
import com.jandi.band_backend.notice.dto.NoticeUpdateReqDTO;
import com.jandi.band_backend.notice.dto.NoticeDetailRespDTO;
import com.jandi.band_backend.notice.dto.NoticeRespDTO;
import com.jandi.band_backend.notice.entity.Notice;
import com.jandi.band_backend.notice.repository.NoticeRepository;
import com.jandi.band_backend.image.S3Service;
import com.jandi.band_backend.user.entity.Users;
import com.jandi.band_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final UserRepository userRepository;
    private final S3Service s3Service;

    private static final String S3_DIRNAME = "notice-photo";

    private Users validateAdminPermissionAndGetUser(Integer userId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));

        if (user.getAdminRole() != Users.AdminRole.ADMIN) {
            throw new InvalidAccessException("관리자만 접근할 수 있습니다.");
        }

        return user;
    }

    private void validateDateTimeRange(LocalDateTime startDatetime, LocalDateTime endDatetime) {
        if (!endDatetime.isAfter(startDatetime)) {
            throw new BadRequestException("종료 시각은 시작 시각보다 늦어야 합니다.");
        }
    }

    private void validateUpdateRequest(NoticeUpdateReqDTO request, Notice notice) {
        // 제목이 빈 문자열인지 검증
        if (request.getTitle() != null && request.getTitle().trim().isEmpty()) {
            throw new BadRequestException("제목은 비어있을 수 없습니다.");
        }

        // 이미지 파일 검증
        if (request.getImage() != null && !request.getImage().isEmpty()) {
            // 파일 크기 검증 (예: 10MB 제한)
            long maxSize = 10 * 1024 * 1024; // 10MB
            if (request.getImage().getSize() > maxSize) {
                throw new BadRequestException("이미지 파일 크기는 10MB를 초과할 수 없습니다.");
            }

            // 파일 타입 검증
            String contentType = request.getImage().getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new BadRequestException("이미지 파일만 업로드 가능합니다.");
            }
        }
    }

    private String sanitizeTitle(String title) {
        // 로깅용 제목 간소화 (길이 제한 및 민감정보 보호)
        if (title == null) return "[null]";
        return title.length() > 50 ? title.substring(0, 50) + "..." : title;
    }

    public List<NoticeRespDTO> getActiveNotices() {
        List<Notice> activeNotices = noticeRepository.findActiveNotices(LocalDateTime.now());
        return activeNotices.stream()
                .map(NoticeRespDTO::new)
                .collect(Collectors.toList());
    }

    public Page<NoticeRespDTO> getAllNotices(Integer userId, Pageable pageable) {
        validateAdminPermissionAndGetUser(userId);

        Page<Notice> notices = noticeRepository.findAllByDeletedAtIsNullOrderByCreatedAtDesc(pageable);
        return notices.map(NoticeRespDTO::new);
    }

    public NoticeDetailRespDTO getNoticeDetail(Integer noticeId, Integer userId) {
        validateAdminPermissionAndGetUser(userId);

        Notice notice = noticeRepository.findByIdAndDeletedAtIsNull(noticeId)
                .orElseThrow(() -> new ResourceNotFoundException("공지사항을 찾을 수 없습니다."));

        return new NoticeDetailRespDTO(notice);
    }

    @Transactional
    public NoticeDetailRespDTO createNotice(NoticeReqDTO request, Integer creatorId) {
        Users creator = validateAdminPermissionAndGetUser(creatorId);

        validateDateTimeRange(request.getStartDatetime(), request.getEndDatetime());

        Notice notice = new Notice();
        notice.setCreator(creator);
        notice.setTitle(request.getTitle());
        notice.setContent(request.getContent());
        notice.setStartDatetime(request.getStartDatetime());
        notice.setEndDatetime(request.getEndDatetime());
        notice.setIsPaused(request.getIsPaused());

        // 이미지 업로드 처리
        String imageUrl = null;
        if (request.getImage() != null && !request.getImage().isEmpty()) {
            imageUrl = uploadImage(request.getImage());
        }
        notice.setImageUrl(imageUrl);

        try {
            Notice savedNotice = noticeRepository.save(notice);
            log.info("공지사항 생성 완료 - ID: {}, 제목: {}", savedNotice.getId(), sanitizeTitle(savedNotice.getTitle()));
            return new NoticeDetailRespDTO(savedNotice);
        } catch (Exception e) {
            // DB 저장 실패 시 업로드된 이미지 삭제 (롤백 처리)
            if (imageUrl != null) {
                deleteImage(imageUrl);
            }
            throw new RuntimeException("DB 저장 실패: " + e.getMessage(), e);
        }
    }

    @Transactional
    public NoticeDetailRespDTO updateNotice(Integer noticeId, NoticeUpdateReqDTO request, Integer userId) {
        validateAdminPermissionAndGetUser(userId);

        Notice notice = noticeRepository.findByIdAndDeletedAtIsNull(noticeId)
                .orElseThrow(() -> new ResourceNotFoundException("공지사항을 찾을 수 없습니다."));

        // 입력값 검증
        validateUpdateRequest(request, notice);

        // 시작/종료 시각 유효성 검사
        if (request.getStartDatetime() != null || request.getEndDatetime() != null) {
            LocalDateTime startTime = request.getStartDatetime() != null ? request.getStartDatetime() : notice.getStartDatetime();
            LocalDateTime endTime = request.getEndDatetime() != null ? request.getEndDatetime() : notice.getEndDatetime();
            validateDateTimeRange(startTime, endTime);

            // 과거 날짜 검증
            LocalDateTime now = LocalDateTime.now();
            if (endTime.isBefore(now)) {
                log.warn("종료 시각이 현재보다 과거로 설정됨 - noticeId: {}, endTime: {}", noticeId, endTime);
            }
        }

        // 부분 업데이트 - null이 아닌 값만 업데이트
        if (request.getTitle() != null) {
            notice.setTitle(request.getTitle());
        }
        if (request.getContent() != null) {
            notice.setContent(request.getContent());
        }
        if (request.getStartDatetime() != null) {
            notice.setStartDatetime(request.getStartDatetime());
        }
        if (request.getEndDatetime() != null) {
            notice.setEndDatetime(request.getEndDatetime());
        }

        // 이미지 처리 로직 개선
        String oldImageUrl = notice.getImageUrl();
        String newImageUrl = null;
        boolean shouldDeleteOldImage = false;

        // 이미지 삭제와 업로드가 동시에 요청된 경우 업로드를 우선시
        if (request.getImage() != null && !request.getImage().isEmpty()) {
            // 새 이미지 업로드
            try {
                newImageUrl = uploadImage(request.getImage());
                notice.setImageUrl(newImageUrl);
                shouldDeleteOldImage = (oldImageUrl != null);
            } catch (Exception e) {
                log.error("이미지 업로드 실패 - noticeId: {}", noticeId, e);
                throw new BadRequestException("이미지 업로드에 실패했습니다. 파일 크기나 형식을 확인해주세요.");
            }
        } else if (request.getDeleteImage() != null && request.getDeleteImage()) {
            // 이미지 삭제만 요청된 경우
            notice.setImageUrl(null);
            shouldDeleteOldImage = (oldImageUrl != null);
        }

        try {
            Notice updatedNotice = noticeRepository.save(notice);

            // DB 저장 성공 후 기존 이미지 삭제
            if (shouldDeleteOldImage) {
                deleteImage(oldImageUrl);
            }

            log.info("공지사항 수정 완료 - ID: {}, 제목: {}", updatedNotice.getId(), sanitizeTitle(updatedNotice.getTitle()));
            return new NoticeDetailRespDTO(updatedNotice);
        } catch (Exception e) {
            // DB 저장 실패 시 업로드된 새 이미지 삭제 (롤백)
            if (newImageUrl != null) {
                deleteImage(newImageUrl);
            }
            log.error("공지사항 수정 실패 - noticeId: {}", noticeId, e);
            throw new BadRequestException("공지사항 수정에 실패했습니다. 다시 시도해주세요.");
        }
    }

    @Transactional
    public void deleteNotice(Integer noticeId, Integer userId) {
        validateAdminPermissionAndGetUser(userId);

        Notice notice = noticeRepository.findByIdAndDeletedAtIsNull(noticeId)
                .orElseThrow(() -> new ResourceNotFoundException("공지사항을 찾을 수 없습니다."));

        String imageUrl = notice.getImageUrl();

        // soft delete 처리
        try {
            notice.setDeletedAt(LocalDateTime.now());
            noticeRepository.save(notice);
        } catch (Exception e) {
            throw new RuntimeException("DB 삭제 실패", e);
        }

        // DB 반영 후 S3 이미지 삭제
        if (imageUrl != null) {
            deleteImage(imageUrl);
        }

        log.info("공지사항 삭제 완료 - ID: {}, 제목: {}", notice.getId(), sanitizeTitle(notice.getTitle()));
    }

    @Transactional
    public NoticeRespDTO toggleNoticeStatus(Integer noticeId, Integer userId) {
        validateAdminPermissionAndGetUser(userId);

        Notice notice = noticeRepository.findByIdAndDeletedAtIsNull(noticeId)
                .orElseThrow(() -> new ResourceNotFoundException("공지사항을 찾을 수 없습니다."));

        notice.setIsPaused(!notice.getIsPaused());
        Notice updatedNotice = noticeRepository.save(notice);

        log.info("공지사항 상태 변경 완료 - ID: {}, 일시정지: {}", updatedNotice.getId(), updatedNotice.getIsPaused());

        return new NoticeRespDTO(updatedNotice);
    }

    /// S3 이미지 처리 관련
    private String uploadImage(MultipartFile file){
        try {
            return s3Service.uploadImage(file, S3_DIRNAME);
        } catch (IOException e) {
            throw new RuntimeException("이미지 업로드 실패: " + e.getMessage(), e);
        }
    }

    private void deleteImage(String imageUrl){
        try {
            if (imageUrl != null) {
                s3Service.deleteImage(imageUrl);
            }
        } catch (Exception e) {
            log.warn("기존 이미지 삭제 실패: {}", imageUrl, e);
        }
    }
}
