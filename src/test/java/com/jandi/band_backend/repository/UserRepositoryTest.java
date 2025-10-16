package com.jandi.band_backend.repository;

import com.jandi.band_backend.univ.entity.Region;
import com.jandi.band_backend.univ.entity.University;
import com.jandi.band_backend.user.entity.Users;
import com.jandi.band_backend.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("UserRepository 테스트")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("ID로 사용자 조회 성공")
    void findById_Success() {
        // Given
        Users user = createUser("testuser", "test123");
        entityManager.persistAndFlush(user);

        // When
        Optional<Users> found = userRepository.findById(user.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getNickname()).isEqualTo("testuser");
        assertThat(found.get().getKakaoOauthId()).isEqualTo("test123");
    }

    @Test
    @DisplayName("ID로 사용자 조회 - 존재하지 않음")
    void findById_NotFound() {
        // When
        Optional<Users> found = userRepository.findById(99999);

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Kakao OAuth ID로 사용자 조회 성공")
    void findByKakaoOauthId_Success() {
        // Given
        Users user = createUser("testuser", "test123");
        entityManager.persistAndFlush(user);

        // When
        Optional<Users> found = userRepository.findByKakaoOauthId("test123");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getNickname()).isEqualTo("testuser");
        assertThat(found.get().getKakaoOauthId()).isEqualTo("test123");
    }

    @Test
    @DisplayName("Kakao OAuth ID로 사용자 조회 - 존재하지 않음")
    void findByKakaoOauthId_NotFound() {
        // When
        Optional<Users> found = userRepository.findByKakaoOauthId("nonexistent");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Kakao OAuth ID로 삭제되지 않은 사용자 조회 성공")
    void findByKakaoOauthIdAndDeletedAtIsNull_Success() {
        // Given
        Users user = createUser("testuser", "test123");
        entityManager.persistAndFlush(user);

        // When
        Optional<Users> found = userRepository.findByKakaoOauthIdAndDeletedAtIsNull("test123");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getNickname()).isEqualTo("testuser");
        assertThat(found.get().getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("Kakao OAuth ID로 삭제되지 않은 사용자 조회 - 삭제된 사용자는 조회되지 않음")
    void findByKakaoOauthIdAndDeletedAtIsNull_DeletedUser() {
        // Given
        Users user = createUser("testuser", "test123");
        user.setDeletedAt(LocalDateTime.now());
        entityManager.persistAndFlush(user);

        // When
        Optional<Users> found = userRepository.findByKakaoOauthIdAndDeletedAtIsNull("test123");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("지정한 날짜 이전에 삭제된 사용자 목록 조회 성공")
    void findAllByDeletedAtBefore_Success() {
        // Given
        LocalDateTime baseTime = LocalDateTime.now();
        
        Users user1 = createUser("user1", "kakao1");
        user1.setDeletedAt(baseTime.minusDays(10)); // 10일 전 삭제
        
        Users user2 = createUser("user2", "kakao2");
        user2.setDeletedAt(baseTime.minusDays(5)); // 5일 전 삭제
        
        Users user3 = createUser("user3", "kakao3");
        user3.setDeletedAt(baseTime.plusDays(1)); // 내일 삭제 (미래)
        
        Users activeUser = createUser("active", "active123");
        // 삭제되지 않은 사용자
        
        entityManager.persistAndFlush(user1);
        entityManager.persistAndFlush(user2);
        entityManager.persistAndFlush(user3);
        entityManager.persistAndFlush(activeUser);

        // When
        List<Users> deletedUsers = userRepository.findAllByDeletedAtBefore(baseTime.minusDays(7));

        // Then
        assertThat(deletedUsers).hasSize(1);
        assertThat(deletedUsers.get(0).getNickname()).isEqualTo("user1");
        assertThat(deletedUsers.get(0).getDeletedAt()).isBefore(baseTime.minusDays(3));
    }

    @Test
    @DisplayName("지정한 날짜 이전에 삭제된 사용자 목록 조회 - 결과 없음")
    void findAllByDeletedAtBefore_NoResults() {
        // Given
        Users activeUser = createUser("active", "active123");
        entityManager.persistAndFlush(activeUser);

        // When
        List<Users> deletedUsers = userRepository.findAllByDeletedAtBefore(LocalDateTime.now());

        // Then
        assertThat(deletedUsers).isEmpty();
    }

    @Test
    @DisplayName("사용자 저장 성공")
    void saveUser_Success() {
        // Given
        Region region = createRegion("Test Region");
        University university = createUniversity("Test University", region);
        Users user = new Users();
        user.setNickname("newuser");
        user.setKakaoOauthId("new123");
        user.setUniversity(university);
        user.setIsRegistered(true);

        // When
        Users saved = entityManager.persistAndFlush(user); // entityManager를 통해 저장하여 @PrePersist 우회

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getNickname()).isEqualTo("newuser");
        assertThat(saved.getKakaoOauthId()).isEqualTo("new123");
        assertThat(saved.getUniversity()).isEqualTo(university);
        assertThat(saved.getIsRegistered()).isTrue();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("사용자 정보 업데이트 성공")
    void updateUser_Success() {
        // Given
        Users user = createUser("original", "original123");
        entityManager.persistAndFlush(user);

        // When
        user.setNickname("updated");
        user.setIsRegistered(true);
        Users updated = userRepository.save(user);

        // Then
        assertThat(updated.getNickname()).isEqualTo("updated");
        assertThat(updated.getIsRegistered()).isTrue();
        
        // 다시 조회해서 확인
        Optional<Users> found = userRepository.findById(user.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getNickname()).isEqualTo("updated");
        assertThat(found.get().getIsRegistered()).isTrue();
    }

    @Test
    @DisplayName("사용자 소프트 삭제 성공")
    void softDeleteUser_Success() {
        // Given
        Users user = createUser("testuser", "test123");
        entityManager.persistAndFlush(user);

        // When
        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);

        // Then
        Optional<Users> foundDeleted = userRepository.findByKakaoOauthIdAndDeletedAtIsNull("test123");
        assertThat(foundDeleted).isEmpty();
        
        // 하지만 실제로는 데이터베이스에 존재
        Optional<Users> foundWithDeleted = userRepository.findByKakaoOauthId("test123");
        assertThat(foundWithDeleted).isPresent();
        assertThat(foundWithDeleted.get().getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("사용자 삭제 성공")
    void deleteUser_Success() {
        // Given
        Users user = createUser("testuser", "test123");
        entityManager.persistAndFlush(user);

        // When
        userRepository.delete(user);

        // Then
        Optional<Users> found = userRepository.findById(user.getId());
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("모든 사용자 조회 성공")
    void findAll_Success() {
        // Given
        Users user1 = createUser("user1", "kakao1");
        Users user2 = createUser("user2", "kakao2");
        entityManager.persistAndFlush(user1);
        entityManager.persistAndFlush(user2);

        // When
        List<Users> allUsers = userRepository.findAll();

        // Then
        assertThat(allUsers).hasSize(2);
        assertThat(allUsers).extracting(Users::getNickname).containsExactlyInAnyOrder("user1", "user2");
    }

    // Helper methods
    private Users createUser(String nickname, String kakaoOauthId) {
        Users user = new Users();
        user.setNickname(nickname);
        user.setKakaoOauthId(kakaoOauthId);
        user.setIsRegistered(false);
        return user;
    }

    private Region createRegion(String regionName) {
        Region region = new Region();
        region.setName(regionName);
        region.setCode("TEST");
        return entityManager.persistAndFlush(region);
    }

    private University createUniversity(String universityName, Region region) {
        University university = new University();
        university.setName(universityName);
        university.setRegion(region);
        university.setUniversityCode("T" + String.valueOf(System.currentTimeMillis()).substring(7));
        university.setAddress("테스트 주소");
        return entityManager.persistAndFlush(university);
    }
}