package com.jandi.band_backend.repository;

import com.jandi.band_backend.club.entity.Club;
import com.jandi.band_backend.club.entity.ClubEvent;
import com.jandi.band_backend.club.repository.ClubEventRepository;
import com.jandi.band_backend.user.entity.Users;
import com.jandi.band_backend.univ.entity.University;
import com.jandi.band_backend.univ.entity.Region;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("ClubEvent Repository Tests")
class ClubEventRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ClubEventRepository clubEventRepository;

    private Users createUser(String nickname, String kakaoOauthId) {
        Users user = new Users();
        user.setNickname(nickname);
        user.setKakaoOauthId(kakaoOauthId);
        return entityManager.persistAndFlush(user);
    }

    private Region createRegion(String name) {
        Region region = new Region();
        region.setName(name);
        region.setCode("TEST");
        return entityManager.persistAndFlush(region);
    }

    private University createUniversity(String name, Region region) {
        University university = new University();
        university.setName(name);
        university.setRegion(region);
        university.setUniversityCode("TEST001");
        return entityManager.persistAndFlush(university);
    }

    private Club createClub(String name, University university) {
        Club club = new Club();
        club.setName(name);
        club.setDescription("Test club description");
        club.setUniversity(university);
        return entityManager.persistAndFlush(club);
    }

    private ClubEvent createClubEvent(String name, Club club, Users creator, LocalDateTime startDatetime, LocalDateTime endDatetime) {
        ClubEvent event = new ClubEvent();
        event.setName(name);
        event.setStartDatetime(startDatetime);
        event.setEndDatetime(endDatetime);
        event.setDescription("Test event description");
        event.setClub(club);
        event.setCreator(creator);
        return event;
    }

    @Test
    @DisplayName("동아리 ID로 삭제되지 않은 이벤트 목록 조회 성공")
    void findByClubIdAndDeletedAtIsNull_Success() {
        // Given
        Users creator = createUser("Creator", "creator123");
        Region region = createRegion("Test Region");
        University university = createUniversity("Test University", region);
        Club club = createClub("Test Club", university);

        ClubEvent event1 = createClubEvent("Event 1", club, creator, 
            LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(2));
        ClubEvent event2 = createClubEvent("Event 2", club, creator,
            LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(2).plusHours(3));
        
        entityManager.persistAndFlush(event1);
        entityManager.persistAndFlush(event2);

        // 삭제된 이벤트 생성
        ClubEvent deletedEvent = createClubEvent("Deleted Event", club, creator,
            LocalDateTime.now().plusDays(3), LocalDateTime.now().plusDays(3).plusHours(1));
        deletedEvent.setDeletedAt(LocalDateTime.now());
        entityManager.persistAndFlush(deletedEvent);

        // When
        List<ClubEvent> events = clubEventRepository.findByClubIdAndDeletedAtIsNull(club.getId());

        // Then
        assertThat(events).hasSize(2);
        assertThat(events).extracting(ClubEvent::getName)
            .containsExactlyInAnyOrder("Event 1", "Event 2");
    }

    @Test
    @DisplayName("동아리에 이벤트가 없는 경우 빈 목록 반환")
    void findByClubIdAndDeletedAtIsNull_Empty() {
        // Given
        Users creator = createUser("Creator", "creator123");
        Region region = createRegion("Test Region");
        University university = createUniversity("Test University", region);
        Club club = createClub("Test Club", university);

        // When
        List<ClubEvent> events = clubEventRepository.findByClubIdAndDeletedAtIsNull(club.getId());

        // Then
        assertThat(events).isEmpty();
    }

    @Test
    @DisplayName("날짜 범위로 겹치는 이벤트 조회")
    void findByClubIdAndOverlappingDate() {
        // Given
        Users creator = createUser("Creator", "creator123");
        Region region = createRegion("Test Region");
        University university = createUniversity("Test University", region);
        Club club = createClub("Test Club", university);

        LocalDateTime baseTime = LocalDateTime.of(2024, 1, 15, 10, 0);
        LocalDateTime startTime = baseTime.minusMinutes(30);
        LocalDateTime endTime = baseTime.plusMinutes(30);
        
        // 겹치는 이벤트
        ClubEvent overlappingEvent = createClubEvent("Overlapping Event", club, creator,
            baseTime.minusHours(1), baseTime.plusHours(1));
        entityManager.persistAndFlush(overlappingEvent);

        // 겹치지 않는 이벤트
        ClubEvent nonOverlappingEvent = createClubEvent("Non-overlapping Event", club, creator,
            baseTime.plusDays(1), baseTime.plusDays(1).plusHours(2));
        entityManager.persistAndFlush(nonOverlappingEvent);

        // When
        List<ClubEvent> events = clubEventRepository.findByClubIdAndOverlappingDate(club.getId(), startTime, endTime);

        // Then
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getName()).isEqualTo("Overlapping Event");
    }

    @Test
    @DisplayName("ID와 동아리로 삭제되지 않은 이벤트 조회 성공")
    void findByIdAndClubIdAndDeletedAtIsNull() {
        // Given
        Users creator = createUser("Creator", "creator123");
        Region region = createRegion("Test Region");
        University university = createUniversity("Test University", region);
        Club club = createClub("Test Club", university);

        ClubEvent event = createClubEvent("Test Event", club, creator,
            LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(2));
        ClubEvent savedEvent = entityManager.persistAndFlush(event);

        // When
        Optional<ClubEvent> found = clubEventRepository.findByIdAndClubIdAndDeletedAtIsNull(savedEvent.getId(), club.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Test Event");
    }

    @Test
    @DisplayName("존재하지 않는 ID로 이벤트 조회시 빈 Optional 반환")
    void findByIdAndClubIdAndDeletedAtIsNull_NotFound() {
        // Given
        Users creator = createUser("Creator", "creator123");
        Region region = createRegion("Test Region");
        University university = createUniversity("Test University", region);
        Club club = createClub("Test Club", university);

        // When
        Optional<ClubEvent> found = clubEventRepository.findByIdAndClubIdAndDeletedAtIsNull(999, club.getId());

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("삭제된 이벤트는 조회되지 않음")
    void findByIdAndClubIdAndDeletedAtIsNull_Deleted() {
        // Given
        Users creator = createUser("Creator", "creator123");
        Region region = createRegion("Test Region");
        University university = createUniversity("Test University", region);
        Club club = createClub("Test Club", university);

        ClubEvent event = createClubEvent("Deleted Event", club, creator,
            LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(2));
        event.setDeletedAt(LocalDateTime.now());
        ClubEvent savedEvent = entityManager.persistAndFlush(event);

        // When
        Optional<ClubEvent> found = clubEventRepository.findByIdAndClubIdAndDeletedAtIsNull(savedEvent.getId(), club.getId());

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("생성자 익명화 성공")
    void anonymizeByUserId() {
        // Given
        Users creator = createUser("Creator", "creator123");
        Users otherCreator = createUser("Other Creator", "other123");
        
        // Create dummy user with ID -1 for anonymization using native SQL
        entityManager.getEntityManager().createNativeQuery(
            "INSERT INTO users (user_id, kakao_oauth_id, nickname, admin_role, is_registered, created_at, updated_at) " +
            "VALUES (-1, 'dummy123', 'dummy', 'USER', false, NOW(), NOW())")
            .executeUpdate();
        
        Region region = createRegion("Test Region");
        University university = createUniversity("Test University", region);
        Club club = createClub("Test Club", university);

        ClubEvent event1 = createClubEvent("Event 1", club, creator,
            LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(2));
        ClubEvent event2 = createClubEvent("Event 2", club, otherCreator,
            LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(2).plusHours(3));
        
        entityManager.persistAndFlush(event1);
        entityManager.persistAndFlush(event2);

        // When
        int updated = clubEventRepository.anonymizeByUserId(creator.getId());

        // Then
        assertThat(updated).isEqualTo(1);
        entityManager.clear();
        
        // 익명화된 이벤트의 creator_user_id는 -1로 설정됨 (네이티브 쿼리 사용)
        List<ClubEvent> events = clubEventRepository.findByClubIdAndDeletedAtIsNull(club.getId());
        assertThat(events).hasSize(2);
    }

    @Test
    @DisplayName("존재하지 않는 생성자 ID로 익명화시 아무것도 업데이트되지 않음")
    void anonymizeByUserId_NoUpdate() {
        // Given
        Users creator = createUser("Creator", "creator123");
        Region region = createRegion("Test Region");
        University university = createUniversity("Test University", region);
        Club club = createClub("Test Club", university);

        ClubEvent event = createClubEvent("Event", club, creator,
            LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(2));
        entityManager.persistAndFlush(event);

        // When
        int updated = clubEventRepository.anonymizeByUserId(999);

        // Then
        assertThat(updated).isEqualTo(0);
    }

    @Test
    @DisplayName("이벤트 저장시 생성일시와 수정일시 자동 설정")
    void saveEvent_AutoTimestamp() {
        // Given
        Users creator = createUser("Creator", "creator123");
        Region region = createRegion("Test Region");
        University university = createUniversity("Test University", region);
        Club club = createClub("Test Club", university);

        ClubEvent event = createClubEvent("New Event", club, creator,
            LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(2));

        // When
        ClubEvent saved = clubEventRepository.save(event);
        entityManager.flush();

        // Then
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getName()).isEqualTo("New Event");
    }

    @Test
    @DisplayName("이벤트 수정시 수정일시만 업데이트")
    void updateEvent_AutoTimestamp() {
        // Given
        Users creator = createUser("Creator", "creator123");
        Region region = createRegion("Test Region");
        University university = createUniversity("Test University", region);
        Club club = createClub("Test Club", university);

        ClubEvent event = createClubEvent("Original Event", club, creator,
            LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(2));
        ClubEvent saved = entityManager.persistAndFlush(event);
        LocalDateTime originalCreatedAt = saved.getCreatedAt();
        LocalDateTime originalUpdatedAt = saved.getUpdatedAt();

        // When
        saved.setName("Updated Event");
        ClubEvent updated = clubEventRepository.save(saved);
        entityManager.flush();

        // Then
        assertThat(updated.getCreatedAt()).isEqualTo(originalCreatedAt);
        assertThat(updated.getUpdatedAt()).isAfter(originalUpdatedAt);
        assertThat(updated.getName()).isEqualTo("Updated Event");
    }

    @Test
    @DisplayName("이벤트 소프트 삭제")
    void softDeleteEvent() {
        // Given
        Users creator = createUser("Creator", "creator123");
        Region region = createRegion("Test Region");
        University university = createUniversity("Test University", region);
        Club club = createClub("Test Club", university);

        ClubEvent event = createClubEvent("Event to Delete", club, creator,
            LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(2));
        ClubEvent saved = entityManager.persistAndFlush(event);

        // When
        saved.setDeletedAt(LocalDateTime.now());
        clubEventRepository.save(saved);
        entityManager.flush();

        // Then
        List<ClubEvent> activeEvents = clubEventRepository.findByClubIdAndDeletedAtIsNull(club.getId());
        assertThat(activeEvents).isEmpty();
        
        Optional<ClubEvent> found = clubEventRepository.findById(saved.getId().longValue());
        assertThat(found).isPresent();
        assertThat(found.get().getDeletedAt()).isNotNull();
    }
}