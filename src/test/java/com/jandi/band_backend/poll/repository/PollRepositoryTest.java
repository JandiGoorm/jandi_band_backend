package com.jandi.band_backend.poll.repository;

import com.jandi.band_backend.club.entity.Club;
import com.jandi.band_backend.club.repository.ClubRepository;
import com.jandi.band_backend.poll.entity.Poll;
import com.jandi.band_backend.univ.entity.Region;
import com.jandi.band_backend.univ.entity.University;
import com.jandi.band_backend.univ.repository.RegionRepository;
import com.jandi.band_backend.univ.repository.UniversityRepository;
import com.jandi.band_backend.user.entity.Users;
import com.jandi.band_backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("PollRepository 데이터베이스 테스트")
class PollRepositoryTest {

    @Autowired
    private PollRepository pollRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private UniversityRepository universityRepository;

    @Autowired
    private RegionRepository regionRepository;

    private University university;
    private Users user;
    private Club club;
    private Poll poll;

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

        // User 생성
        user = new Users();
        user.setKakaoOauthId("12345");
        user.setNickname("testuser");
        user.setUniversity(university);
        user = userRepository.save(user);

        // Club 생성
        club = new Club();
        club.setName("테스트 밴드");
        club.setDescription("테스트용 밴드입니다");
        club.setUniversity(university);
        club = clubRepository.save(club);

        // Poll 생성
        poll = new Poll();
        poll.setTitle("곡 투표");
        poll.setClub(club);
        poll.setCreator(user);
        poll.setStartDatetime(LocalDateTime.now());
        poll.setEndDatetime(LocalDateTime.now().plusDays(7));
        poll = pollRepository.save(poll);
    }

    @Test
    @DisplayName("동아리별 투표 목록 조회 - 페이징 처리")
    void findAllByClubAndDeletedAtIsNullOrderByCreatedAtDesc_ShouldReturnPagedPolls() {
        // Given
        Poll poll2 = new Poll();
        poll2.setTitle("곡 투표 2");
        poll2.setClub(club);
        poll2.setCreator(user);
        poll2.setStartDatetime(LocalDateTime.now());
        poll2.setEndDatetime(LocalDateTime.now().plusDays(14));
        pollRepository.save(poll2);

        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Poll> result = pollRepository.findAllByClubAndDeletedAtIsNullOrderByCreatedAtDesc(club, pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent().get(0).getTitle()).isIn("곡 투표", "곡 투표 2");
    }

    @Test
    @DisplayName("삭제된 투표는 조회되지 않음")
    void findAllByClubAndDeletedAtIsNullOrderByCreatedAtDesc_ShouldNotReturnDeletedPolls() {
        // Given
        poll.setDeletedAt(LocalDateTime.now());
        pollRepository.save(poll);

        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Poll> result = pollRepository.findAllByClubAndDeletedAtIsNullOrderByCreatedAtDesc(club, pageable);

        // Then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("투표 ID로 투표 상세 조회")
    void findByIdAndDeletedAtIsNull_ShouldReturnPoll() {
        // When
        Optional<Poll> result = pollRepository.findByIdAndDeletedAtIsNull(poll.getId());

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getTitle()).isEqualTo("곡 투표");
        assertThat(result.get().getClub().getId()).isEqualTo(club.getId());
    }

    @Test
    @DisplayName("존재하지 않는 투표 ID로 조회시 빈 결과 반환")
    void findByIdAndDeletedAtIsNull_WithInvalidId_ShouldReturnEmpty() {
        // When
        Optional<Poll> result = pollRepository.findByIdAndDeletedAtIsNull(999);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("마감일 이후 투표 조회 테스트")
    void findAllByClubAndEndDatetimeAfterAndDeletedAtIsNullOrderByEndDatetimeAsc_ShouldReturnActivePolls() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        
        // 마감된 투표
        Poll expiredPoll = new Poll();
        expiredPoll.setTitle("마감된 투표");
        expiredPoll.setClub(club);
        expiredPoll.setCreator(user);
        expiredPoll.setStartDatetime(now.minusDays(10));
        expiredPoll.setEndDatetime(now.minusDays(1)); // 어제 마감
        pollRepository.save(expiredPoll);

        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Poll> result = pollRepository.findAllByClubAndEndDatetimeAfterAndDeletedAtIsNullOrderByEndDatetimeAsc(club, now, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1); // 현재 활성화된 투표만 반환
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("곡 투표");
        assertThat(result.getContent().get(0).getEndDatetime()).isAfter(now);
    }

    @Test
    @DisplayName("투표 생성자 정보 페치 조인 테스트")
    void findByIdAndDeletedAtIsNull_ShouldFetchCreator() {
        // When
        Optional<Poll> result = pollRepository.findByIdAndDeletedAtIsNull(poll.getId());

        // Then
        assertThat(result).isPresent();
        Poll foundPoll = result.get();
        
        // LazyInitializationException 발생하지 않고 creator 정보에 접근 가능해야 함
        assertThat(foundPoll.getCreator()).isNotNull();
        assertThat(foundPoll.getCreator().getNickname()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("투표 소프트 삭제 테스트")
    void softDeletePoll_ShouldSetDeletedAt() {
        // Given
        assertThat(poll.getDeletedAt()).isNull();

        // When
        poll.setDeletedAt(LocalDateTime.now());
        Poll savedPoll = pollRepository.save(poll);

        // Then
        assertThat(savedPoll.getDeletedAt()).isNotNull();
        
        // 삭제된 투표는 조회되지 않아야 함
        Optional<Poll> result = pollRepository.findByIdAndDeletedAtIsNull(poll.getId());
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("투표 생성자 익명화 테스트 - 예외 확인")
    void anonymizeByCreatorId_ShouldThrowExceptionForNonExistentUser() {
        // Given
        Integer nonExistentUserId = 99999;
        
        // When & Then
        // 존재하지 않는 사용자 ID로 익명화 시도시 업데이트된 행이 0개여야 함
        int updatedCount = pollRepository.anonymizeByCreatorId(nonExistentUserId);
        assertThat(updatedCount).isEqualTo(0);
        
        // 기존 투표는 변경되지 않아야 함
        Optional<Poll> unchangedPoll = pollRepository.findByIdAndDeletedAtIsNull(poll.getId());
        assertThat(unchangedPoll).isPresent();
        assertThat(unchangedPoll.get().getCreator().getId()).isEqualTo(user.getId());
    }

    @Test
    @DisplayName("동일한 동아리의 여러 투표 정렬 테스트")
    void findAllByClubAndDeletedAtIsNullOrderByCreatedAtDesc_ShouldOrderByCreatedAtDesc() {
        // Given
        Poll oldPoll = new Poll();
        oldPoll.setTitle("오래된 투표");
        oldPoll.setClub(club);
        oldPoll.setCreator(user);
        oldPoll.setStartDatetime(LocalDateTime.now().minusDays(5));
        oldPoll.setEndDatetime(LocalDateTime.now().plusDays(2));
        pollRepository.save(oldPoll);
        
        Poll newPoll = new Poll();
        newPoll.setTitle("새로운 투표");
        newPoll.setClub(club);
        newPoll.setCreator(user);
        newPoll.setStartDatetime(LocalDateTime.now());
        newPoll.setEndDatetime(LocalDateTime.now().plusDays(10));
        pollRepository.save(newPoll);

        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Poll> result = pollRepository.findAllByClubAndDeletedAtIsNullOrderByCreatedAtDesc(club, pageable);

        // Then
        assertThat(result.getContent()).hasSize(3); // 기존 + 2개 추가
        // 최신 생성순으로 정렬되어야 함
        assertThat(result.getContent().get(0).getCreatedAt())
                .isAfterOrEqualTo(result.getContent().get(1).getCreatedAt());
    }
}