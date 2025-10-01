package com.jandi.band_backend.repository;

import com.jandi.band_backend.univ.entity.Region;
import com.jandi.band_backend.univ.entity.University;
import com.jandi.band_backend.univ.repository.RegionRepository;
import com.jandi.band_backend.univ.repository.UniversityRepository;
import com.jandi.band_backend.user.entity.UserPhoto;
import com.jandi.band_backend.user.entity.Users;
import com.jandi.band_backend.user.repository.UserPhotoRepository;
import com.jandi.band_backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("UserPhoto Repository 테스트")
class UserPhotoRepositoryTest {

    @Autowired
    private UserPhotoRepository userPhotoRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UniversityRepository universityRepository;

    @Autowired
    private RegionRepository regionRepository;

    private Users user;
    private UserPhoto userPhoto;

    @BeforeEach
    void setUp() {
        // 지역 생성
        Region region = new Region();
        region.setCode("SEOUL");
        region.setName("서울특별시");
        region = regionRepository.save(region);

        // 대학교 생성
        University university = new University();
        university.setUniversityCode("SNU0001");
        university.setName("서울대학교");
        university.setRegion(region);
        university = universityRepository.save(university);

        // 사용자 생성
        user = new Users();
        user.setNickname("테스트사용자");
        user.setKakaoOauthId("user_oauth_123");
        user.setUniversity(university);
        user = userRepository.save(user);

        // 사용자 프로필 사진 생성
        userPhoto = new UserPhoto();
        userPhoto.setUser(user);
        userPhoto.setImageUrl("https://example.com/profile.jpg");
        userPhoto.setIsCurrent(true);
        userPhoto = userPhotoRepository.save(userPhoto);
    }

    @Test
    @DisplayName("사용자별 프로필 사진 조회 성공")
    void findByUser_Success() {
        // when
        UserPhoto foundPhoto = userPhotoRepository.findByUser(user);

        // then
        assertThat(foundPhoto).isNotNull();
        assertThat(foundPhoto.getUser().getId()).isEqualTo(user.getId());
        assertThat(foundPhoto.getImageUrl()).isEqualTo("https://example.com/profile.jpg");
        assertThat(foundPhoto.getIsCurrent()).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 사용자의 프로필 사진 조회")
    void findByUser_NotFound() {
        // given
        Users otherUser = new Users();
        otherUser.setNickname("다른사용자");
        otherUser.setKakaoOauthId("other_oauth_123");
        otherUser.setUniversity(user.getUniversity());
        otherUser = userRepository.save(otherUser);

        // when
        UserPhoto foundPhoto = userPhotoRepository.findByUser(otherUser);

        // then
        assertThat(foundPhoto).isNull();
    }

    @Test
    @DisplayName("새 프로필 사진 저장 시 자동 타임스탬프 설정")
    void saveUserPhoto_AutoTimestamp() {
        // given
        UserPhoto newPhoto = new UserPhoto();
        newPhoto.setUser(user);
        newPhoto.setImageUrl("https://example.com/new-profile.jpg");
        newPhoto.setIsCurrent(false);

        // when
        UserPhoto savedPhoto = userPhotoRepository.save(newPhoto);

        // then
        assertThat(savedPhoto.getUploadedAt()).isNotNull();
        assertThat(savedPhoto.getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("사용자별 프로필 사진 소프트 삭제 성공")
    void softDeleteByUserId_Success() {
        // given
        LocalDateTime deletedAt = LocalDateTime.now();

        // when
        int deletedCount = userPhotoRepository.softDeleteByUserId(user.getId(), deletedAt);

        // then
        assertThat(deletedCount).isEqualTo(1);

        // 삭제 확인
        entityManager.refresh(userPhoto);
        assertThat(userPhoto.getDeletedAt()).isNotNull();
        assertThat(userPhoto.getDeletedAt()).isEqualToIgnoringNanos(deletedAt);
    }

    @Test
    @DisplayName("존재하지 않는 사용자의 소프트 삭제")
    void softDeleteByUserId_NoRows() {
        // given
        Integer nonExistentUserId = 99999;
        LocalDateTime deletedAt = LocalDateTime.now();

        // when
        int deletedCount = userPhotoRepository.softDeleteByUserId(nonExistentUserId, deletedAt);

        // then
        assertThat(deletedCount).isEqualTo(0);
    }

    @Test
    @DisplayName("이미 삭제된 사진의 소프트 삭제 시도")
    void softDeleteByUserId_AlreadyDeleted() {
        // given
        userPhoto.setDeletedAt(LocalDateTime.now().minusDays(1));
        userPhotoRepository.save(userPhoto);

        LocalDateTime newDeletedAt = LocalDateTime.now();

        // when
        int deletedCount = userPhotoRepository.softDeleteByUserId(user.getId(), newDeletedAt);

        // then
        assertThat(deletedCount).isEqualTo(0); // 이미 삭제된 사진은 업데이트되지 않음
    }

    @Test
    @DisplayName("현재 프로필 사진 여부 플래그 동작 확인")
    void isCurrentFlag_Operation() {
        // given
        assertThat(userPhoto.getIsCurrent()).isTrue();

        // when - 현재 사진을 비활성화
        userPhoto.setIsCurrent(false);
        UserPhoto updatedPhoto = userPhotoRepository.save(userPhoto);

        // then
        assertThat(updatedPhoto.getIsCurrent()).isFalse();
        
        // 다시 활성화
        updatedPhoto.setIsCurrent(true);
        UserPhoto reactivatedPhoto = userPhotoRepository.save(updatedPhoto);
        assertThat(reactivatedPhoto.getIsCurrent()).isTrue();
    }

    @Test
    @DisplayName("사용자별 프로필 사진 조회 - 현재 사진만 조회")
    void findCurrentPhoto_SingleResult() {
        // when
        UserPhoto currentPhoto = userPhotoRepository.findByUser(user);

        // then - 현재 설정된 사진만 조회되는지 확인
        assertThat(currentPhoto).isNotNull();
        assertThat(currentPhoto.getIsCurrent()).isTrue();
        assertThat(currentPhoto.getImageUrl()).isEqualTo("https://example.com/profile.jpg");
    }

    @Test
    @DisplayName("프로필 사진 삭제 후 사용자 조회로 확인")
    void deletePhoto_VerifyByUser() {
        // given
        Long photoId = userPhoto.getId().longValue();

        // when
        userPhotoRepository.deleteById(photoId);

        // then
        UserPhoto foundPhoto = userPhotoRepository.findByUser(user);
        assertThat(foundPhoto).isNull();
    }
}