package com.jandi.band_backend.user.service;

import com.jandi.band_backend.club.repository.ClubMemberRepository;
import com.jandi.band_backend.image.S3Service;
import com.jandi.band_backend.team.repository.TeamMemberRepository;
import com.jandi.band_backend.user.entity.UserPhoto;
import com.jandi.band_backend.user.entity.Users;
import com.jandi.band_backend.user.repository.UserPhotoRepository;
import com.jandi.band_backend.user.repository.UserRepository;
import com.jandi.band_backend.user.repository.UserTimetableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserHardDeleteScheduler {

    private final UserRepository userRepository;
    private final UserTimetableRepository userTimetableRepository;
    private final UserPhotoRepository userPhotoRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final S3Service s3Service;

    @Value("${user-withdraw.days}")
    private int userWithdrawDays;

    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void hardDeleteUsers() {
        log.info("=== [UserHardDeleteScheduler] 유저 삭제 스케줄 시작 ===");

        LocalDateTime threshold = LocalDateTime.now().minusDays(userWithdrawDays);
        List<Users> toDelete = userRepository.findAllByDeletedAtBefore(threshold);

        log.info("[UserHardDeleteScheduler] 삭제 대상 유저: {}명 (기준일: {})", toDelete.size(), threshold);

        for (Users user : toDelete) {
            Integer userId = user.getId();
            log.info("\n--- [UserHardDelete] userId={} 삭제 처리 시작 ---", userId);

            // 시간표 삭제
            log.info(" - userId={} 의 시간표 삭제 처리 시작", userId);
            int deletedTimetables = userTimetableRepository.deleteAllByUser(user);
            log.info(" - 시간표 {}개 삭제 완료", deletedTimetables);

            // 프로필 사진 삭제
            log.info(" - userId={} 의 프로필 삭제 처리 시작", userId);
            UserPhoto userPhoto = userPhotoRepository.findByUser(user);
            if (userPhoto != null) {
                String imageUrl = userPhoto.getImageUrl();
                log.info(" - userId={} 의 프로필 삭제 처리 시작: imageUrl={}", userId, imageUrl);
                try {
                    s3Service.deleteImage(imageUrl);
                    userPhotoRepository.delete(userPhoto);
                    log.info(" - 프로필 사진 S3 및 DB 삭제 완료");
                } catch (Exception e) {
                    log.error(" - 프로필 사진 삭제 실패: {}", e.getMessage(), e);
                }
            } else {
                log.info(" - 프로필 사진 없음");
            }

            // 동아리, 팀 정보 삭제
            log.info(" - userId={} 의 동아리/팀 삭제 처리 시작", userId);
            int deletedClubMembers = clubMemberRepository.deleteAllByUser(user);
            int deletedTeamMembers = teamMemberRepository.deleteAllByUser(user);
            log.info(" - 동아리 구성원 {}개, 팀 구성원 {}개 삭제 완료", deletedClubMembers, deletedTeamMembers);

            // 유저 삭제
            userRepository.delete(user);
            log.info("--- [UserHardDelete] userId={} 삭제 완료 ---\n", userId);
        }

        log.info("=== [UserHardDeleteScheduler] 전체 삭제 작업 완료 ===");
    }
}