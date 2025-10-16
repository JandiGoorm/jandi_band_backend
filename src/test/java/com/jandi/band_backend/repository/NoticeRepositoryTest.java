package com.jandi.band_backend.repository;

import com.jandi.band_backend.club.entity.Club;
import com.jandi.band_backend.club.repository.ClubRepository;
import com.jandi.band_backend.notice.entity.Notice;
import com.jandi.band_backend.notice.repository.NoticeRepository;
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
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Notice Repository 테스트")
class NoticeRepositoryTest {

    @Autowired
    private NoticeRepository noticeRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UniversityRepository universityRepository;

    @Autowired
    private RegionRepository regionRepository;

    private Notice notice;
    private Users creator;
    private Club club;
    private University university;
    private Region region;

    @BeforeEach
    void setUp() {
        // 지역 생성
        region = new Region();
        region.setCode("SEOUL");
        region.setName("서울특별시");
        region = regionRepository.save(region);

        // 대학교 생성
        university = new University();
        university.setUniversityCode("SNU0001");
        university.setName("서울대학교");
        university.setRegion(region);
        university = universityRepository.save(university);

        // 사용자 생성
        creator = new Users();
        creator.setNickname("공지작성자");
        creator.setKakaoOauthId("notice_creator_123");
        creator.setUniversity(university);
        creator = userRepository.save(creator);

        // 클럽 생성
        club = new Club();
        club.setName("음악 동아리");
        club.setDescription("음악을 사랑하는 동아리");
        club.setUniversity(university);
        club = clubRepository.save(club);

        // 공지사항 생성 (Club과의 관계가 없으므로 creator만 설정)
        notice = new Notice();
        notice.setTitle("첫 번째 공지사항");
        notice.setContent("공지사항 내용입니다.");
        notice.setCreator(creator);
        notice.setIsPaused(false);
        notice.setStartDatetime(LocalDateTime.now().minusDays(1));
        notice.setEndDatetime(LocalDateTime.now().plusDays(7));
        notice = noticeRepository.save(notice);
    }

    @Test
    @DisplayName("삭제되지 않은 공지사항 전체 조회 - 성공")
    void findAllByDeletedAtIsNullOrderByCreatedAtDesc_Success() {
        // given
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<Notice> result = noticeRepository.findAllByDeletedAtIsNullOrderByCreatedAtDesc(pageable);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("첫 번째 공지사항");
        assertThat(result.getContent().get(0).getCreator().getNickname()).isEqualTo("공지작성자");
    }

    @Test
    @DisplayName("삭제되지 않은 공지사항 전체 조회 - 소프트 삭제된 것은 제외")
    void findAllByDeletedAtIsNullOrderByCreatedAtDesc_ExcludeDeleted() {
        // given
        notice.setDeletedAt(LocalDateTime.now());
        noticeRepository.save(notice);
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<Notice> result = noticeRepository.findAllByDeletedAtIsNullOrderByCreatedAtDesc(pageable);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("ID로 삭제되지 않은 공지사항 조회 - 성공")
    void findByIdAndDeletedAtIsNull_Success() {
        // when
        Optional<Notice> result = noticeRepository.findByIdAndDeletedAtIsNull(notice.getId());

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getTitle()).isEqualTo("첫 번째 공지사항");
        assertThat(result.get().getCreator().getNickname()).isEqualTo("공지작성자");
    }

    @Test
    @DisplayName("ID로 삭제되지 않은 공지사항 조회 - 존재하지 않음")
    void findByIdAndDeletedAtIsNull_NotFound() {
        // when
        Optional<Notice> result = noticeRepository.findByIdAndDeletedAtIsNull(99999);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("ID로 삭제되지 않은 공지사항 조회 - 소프트 삭제됨")
    void findByIdAndDeletedAtIsNull_SoftDeleted() {
        // given
        notice.setDeletedAt(LocalDateTime.now());
        noticeRepository.save(notice);

        // when
        Optional<Notice> result = noticeRepository.findByIdAndDeletedAtIsNull(notice.getId());

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("현재 활성화된 공지사항 조회 - 성공")
    void findActiveNotices_Success() {
        // given
        LocalDateTime now = LocalDateTime.now();

        // when
        List<Notice> result = noticeRepository.findActiveNotices(now);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("첫 번째 공지사항");
        assertThat(result.get(0).getIsPaused()).isFalse();
    }

    @Test
    @DisplayName("현재 활성화된 공지사항 조회 - 일시정지된 공지는 제외")
    void findActiveNotices_ExcludePaused() {
        // given
        notice.setIsPaused(true);
        noticeRepository.save(notice);
        LocalDateTime now = LocalDateTime.now();

        // when
        List<Notice> result = noticeRepository.findActiveNotices(now);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("현재 활성화된 공지사항 조회 - 기간 외 공지는 제외")
    void findActiveNotices_ExcludeOutOfPeriod() {
        // given
        notice.setEndDatetime(LocalDateTime.now().minusDays(1)); // 어제 종료
        noticeRepository.save(notice);
        LocalDateTime now = LocalDateTime.now();

        // when
        List<Notice> result = noticeRepository.findActiveNotices(now);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("현재 활성화된 공지사항 조회 - 미래 시작 공지는 제외")
    void findActiveNotices_ExcludeFutureStart() {
        // given
        notice.setStartDatetime(LocalDateTime.now().plusDays(1)); // 내일 시작
        noticeRepository.save(notice);
        LocalDateTime now = LocalDateTime.now();

        // when
        List<Notice> result = noticeRepository.findActiveNotices(now);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("현재 활성화된 공지사항 조회 - 소프트 삭제된 공지는 제외")
    void findActiveNotices_ExcludeDeleted() {
        // given
        notice.setDeletedAt(LocalDateTime.now());
        noticeRepository.save(notice);
        LocalDateTime now = LocalDateTime.now();

        // when
        List<Notice> result = noticeRepository.findActiveNotices(now);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("여러 공지사항 생성 후 생성일시 내림차순 정렬 확인")
    void multipleNoticesOrderTest() throws InterruptedException {
        // given
        Thread.sleep(10); // 생성 시간 차이를 위해 잠시 대기

        Notice secondNotice = new Notice();
        secondNotice.setTitle("두 번째 공지사항");
        secondNotice.setContent("두 번째 공지사항 내용");
        secondNotice.setCreator(creator);
        secondNotice.setIsPaused(false);
        secondNotice.setStartDatetime(LocalDateTime.now().minusDays(1));
        secondNotice.setEndDatetime(LocalDateTime.now().plusDays(7));
        noticeRepository.save(secondNotice);

        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<Notice> result = noticeRepository.findAllByDeletedAtIsNullOrderByCreatedAtDesc(pageable);

        // then
        assertThat(result.getContent()).hasSize(2);
        // 최신순으로 정렬되어야 함
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("두 번째 공지사항");
        assertThat(result.getContent().get(1).getTitle()).isEqualTo("첫 번째 공지사항");
    }

    @Test
    @DisplayName("공지사항 저장 시 생성일시와 수정일시 자동 설정")
    void saveNotice_AutoSetTimestamps() {
        // given
        Notice newNotice = new Notice();
        newNotice.setTitle("새 공지사항");
        newNotice.setContent("새 공지사항 내용");
        newNotice.setCreator(creator);
        newNotice.setIsPaused(false);
        newNotice.setStartDatetime(LocalDateTime.now().minusDays(1));
        newNotice.setEndDatetime(LocalDateTime.now().plusDays(7));

        // when
        Notice savedNotice = noticeRepository.save(newNotice);

        // then
        assertThat(savedNotice.getCreatedAt()).isNotNull();
        assertThat(savedNotice.getUpdatedAt()).isNotNull();
        assertThat(savedNotice.getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("공지사항 업데이트 시 수정일시 자동 업데이트")
    void updateNotice_AutoUpdateTimestamp() throws InterruptedException {
        // given
        LocalDateTime originalUpdatedAt = notice.getUpdatedAt();
        Thread.sleep(100); // 시간 차이를 위해 대기

        // when
        notice.setTitle("수정된 공지사항");
        Notice updatedNotice = noticeRepository.save(notice);
        entityManager.flush(); // 강제로 DB에 반영
        entityManager.refresh(updatedNotice); // 엔티티 새로고침

        // then
        assertThat(updatedNotice.getUpdatedAt()).isAfter(originalUpdatedAt);
        assertThat(updatedNotice.getTitle()).isEqualTo("수정된 공지사항");
    }
}