package com.jandi.band_backend.user.repository;

import com.jandi.band_backend.univ.entity.Region;
import com.jandi.band_backend.univ.entity.University;
import com.jandi.band_backend.univ.repository.RegionRepository;
import com.jandi.band_backend.univ.repository.UniversityRepository;
import com.jandi.band_backend.user.entity.Users;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("UserRepository 데이터베이스 테스트")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UniversityRepository universityRepository;

    @Autowired
    private RegionRepository regionRepository;

    private University university;
    private Users user1;
    private Users user2;

    @BeforeEach
    void setUp() {
        // Region 생성
        Region region = new Region();
        region.setCode("SEOUL");
        region.setName("서울");
        region = regionRepository.save(region);

        // University 생성
        university = new University();
        university.setUniversityCode("SNU0001");
        university.setName("서울대학교");
        university.setRegion(region);
        university = universityRepository.save(university);

        // User 1 생성
        user1 = new Users();
        user1.setKakaoOauthId("kakao123");
        user1.setNickname("테스트유저1");
        user1.setUniversity(university);
        user1 = userRepository.save(user1);

        // User 2 생성
        user2 = new Users();
        user2.setKakaoOauthId("kakao456");
        user2.setNickname("테스트유저2");
        user2.setUniversity(university);
        user2 = userRepository.save(user2);
    }

    @Test
    @DisplayName("카카오 OAuth ID로 사용자 조회")
    void findByKakaoOauthId_ShouldReturnUser() {
        // When
        Optional<Users> result = userRepository.findByKakaoOauthId("kakao123");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getNickname()).isEqualTo("테스트유저1");
    }

    @Test
    @DisplayName("존재하지 않는 카카오 OAuth ID로 조회시 빈 결과 반환")
    void findByKakaoOauthId_WithNonExistentId_ShouldReturnEmpty() {
        // When
        Optional<Users> result = userRepository.findByKakaoOauthId("nonexistent");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("카카오 OAuth ID와 삭제되지 않은 조건으로 조회")
    void findByKakaoOauthIdAndDeletedAtIsNull_ShouldReturnActiveUser() {
        // When
        Optional<Users> result = userRepository.findByKakaoOauthIdAndDeletedAtIsNull("kakao123");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getNickname()).isEqualTo("테스트유저1");
    }

    @Test
    @DisplayName("삭제된 사용자는 조회되지 않음")
    void findByKakaoOauthIdAndDeletedAtIsNull_ShouldNotReturnDeletedUser() {
        // Given
        user1.setDeletedAt(LocalDateTime.now());
        userRepository.save(user1);

        // When
        Optional<Users> result = userRepository.findByKakaoOauthIdAndDeletedAtIsNull("kakao123");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("사용자 ID로 조회")
    void findById_ShouldReturnUser() {
        // When
        Optional<Users> result = userRepository.findById(user1.getId());

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getNickname()).isEqualTo("테스트유저1");
        assertThat(result.get().getKakaoOauthId()).isEqualTo("kakao123");
    }

    @Test
    @DisplayName("사용자 정보 업데이트")
    void updateUserInfo_ShouldPersistChanges() {
        // Given
        Users user = userRepository.findById(user1.getId()).orElseThrow();
        
        // When
        user.setNickname("수정된닉네임");
        Users savedUser = userRepository.save(user);

        // Then
        assertThat(savedUser.getNickname()).isEqualTo("수정된닉네임");
        
        // 데이터베이스에서 다시 조회하여 확인
        Users reloadedUser = userRepository.findById(user1.getId()).orElseThrow();
        assertThat(reloadedUser.getNickname()).isEqualTo("수정된닉네임");
    }

    @Test
    @DisplayName("사용자 소프트 삭제")
    void softDeleteUser_ShouldSetDeletedAt() {
        // Given
        assertThat(user1.getDeletedAt()).isNull();

        // When
        user1.setDeletedAt(LocalDateTime.now());
        Users deletedUser = userRepository.save(user1);

        // Then
        assertThat(deletedUser.getDeletedAt()).isNotNull();
        
        // 삭제된 사용자는 findByKakaoOauthIdAndDeletedAtIsNull로 조회되지 않음
        Optional<Users> result = userRepository.findByKakaoOauthIdAndDeletedAtIsNull("kakao123");
        assertThat(result).isEmpty();
        
        // 하지만 findById나 findByKakaoOauthId로는 여전히 조회 가능
        Optional<Users> deletedUserResult = userRepository.findByKakaoOauthId("kakao123");
        assertThat(deletedUserResult).isPresent();
        assertThat(deletedUserResult.get().getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("중복 카카오 OAuth ID 제약 조건 테스트")
    void uniqueKakaoOauthId_ShouldPreventDuplicates() {
        // Given
        Users duplicateUser = new Users();
        duplicateUser.setKakaoOauthId("kakao123"); // 기존 user1과 동일
        duplicateUser.setNickname("중복테스트");
        duplicateUser.setUniversity(university);

        // When & Then
        // 실제로는 DataIntegrityViolationException이 발생해야 함
        org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.dao.DataIntegrityViolationException.class,
                () -> userRepository.saveAndFlush(duplicateUser)
        );
    }

    @Test
    @DisplayName("특정 시간 이전에 삭제된 사용자 조회")
    void findAllByDeletedAtBefore_ShouldReturnDeletedUsers() {
        // Given
        LocalDateTime cutoffTime = LocalDateTime.now();
        
        // 과거에 삭제된 사용자
        user1.setDeletedAt(cutoffTime.minusDays(1));
        userRepository.save(user1);
        
        // 미래에 삭제될 사용자 (테스트를 위한 가상 시나리오)
        user2.setDeletedAt(cutoffTime.plusDays(1));
        userRepository.save(user2);

        // When
        List<Users> result = userRepository.findAllByDeletedAtBefore(cutoffTime);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getNickname()).isEqualTo("테스트유저1");
    }

    @Test
    @DisplayName("모든 사용자 조회")
    void findAll_ShouldReturnAllUsers() {
        // When
        List<Users> result = userRepository.findAll();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Users::getNickname)
                .containsExactlyInAnyOrder("테스트유저1", "테스트유저2");
    }

    @Test
    @DisplayName("사용자 완전 삭제")
    void deleteUser_ShouldRemoveFromDatabase() {
        // Given
        Integer userId = user1.getId();
        
        // When
        userRepository.delete(user1);

        // Then
        Optional<Users> result = userRepository.findById(userId);
        assertThat(result).isEmpty();
        
        List<Users> allUsers = userRepository.findAll();
        assertThat(allUsers).hasSize(1);
        assertThat(allUsers.get(0).getNickname()).isEqualTo("테스트유저2");
    }
}