package com.jandi.band_backend.repository;

import com.jandi.band_backend.club.entity.Club;
import com.jandi.band_backend.club.repository.ClubRepository;
import com.jandi.band_backend.univ.entity.Region;
import com.jandi.band_backend.univ.entity.University;
import com.jandi.band_backend.univ.repository.RegionRepository;
import com.jandi.band_backend.univ.repository.UniversityRepository;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Club Repository 테스트")
class ClubRepositoryTest {

    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private UniversityRepository universityRepository;

    @Autowired
    private RegionRepository regionRepository;

    private University university;
    private Club club;

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

        // Club 생성
        club = new Club();
        club.setName("록밴드 동아리");
        club.setDescription("록 음악을 사랑하는 사람들의 모임");
        club.setUniversity(university);
        club.setChatroomUrl("https://example.com/chatroom");
        club.setInstagramId("@rockband_club");
        club = clubRepository.save(club);
    }

    @Test
    @DisplayName("모든 클럽 조회 - 성공")
    void findAll_Success() {
        // When
        List<Club> clubs = clubRepository.findAll();

        // Then
        assertThat(clubs).isNotEmpty();
        assertThat(clubs).hasSize(1);
        assertThat(clubs.get(0).getName()).isEqualTo("록밴드 동아리");
    }

    @Test
    @DisplayName("ID로 클럽 조회 - 성공")
    void findById_Success() {
        // When
        Optional<Club> foundClub = clubRepository.findById(club.getId());

        // Then
        assertThat(foundClub).isPresent();
        assertThat(foundClub.get().getName()).isEqualTo("록밴드 동아리");
        assertThat(foundClub.get().getUniversity()).isEqualTo(university);
    }

    @Test
    @DisplayName("ID로 클럽 조회 - 존재하지 않음")
    void findById_NotFound() {
        // When
        Optional<Club> foundClub = clubRepository.findById(99999);

        // Then
        assertThat(foundClub).isEmpty();
    }

    @Test
    @DisplayName("삭제되지 않은 모든 클럽 조회 - 성공")
    void findAllByDeletedAtIsNull_Success() {
        // When
        List<Club> clubs = clubRepository.findAllByDeletedAtIsNull();

        // Then
        assertThat(clubs).hasSize(1);
        assertThat(clubs.get(0).getName()).isEqualTo("록밴드 동아리");
        assertThat(clubs.get(0).getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("삭제되지 않은 클럽 페이징 조회 - 성공")
    void findAllByDeletedAtIsNullWithPaging_Success() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Club> clubs = clubRepository.findAllByDeletedAtIsNull(pageable);

        // Then
        assertThat(clubs).isNotNull();
        assertThat(clubs.getContent()).hasSize(1);
        assertThat(clubs.getContent().get(0).getName()).isEqualTo("록밴드 동아리");
        assertThat(clubs.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("ID와 삭제되지 않은 클럽 조회 - 성공")
    void findByIdAndDeletedAtIsNull_Success() {
        // When
        Optional<Club> foundClub = clubRepository.findByIdAndDeletedAtIsNull(club.getId());

        // Then
        assertThat(foundClub).isPresent();
        assertThat(foundClub.get().getName()).isEqualTo("록밴드 동아리");
        assertThat(foundClub.get().getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("ID와 삭제되지 않은 클럽 조회 - 존재하지 않음")
    void findByIdAndDeletedAtIsNull_NotFound() {
        // When
        Optional<Club> foundClub = clubRepository.findByIdAndDeletedAtIsNull(99999);

        // Then
        assertThat(foundClub).isEmpty();
    }

    @Test
    @DisplayName("Soft Delete 테스트 - 삭제된 클럽은 조회되지 않음")
    void softDelete_DeletedClubNotFound() {
        // Given
        club.setDeletedAt(LocalDateTime.now());
        clubRepository.save(club);

        // When
        Optional<Club> foundClub = clubRepository.findByIdAndDeletedAtIsNull(club.getId());
        List<Club> allClubs = clubRepository.findAllByDeletedAtIsNull();

        // Then
        assertThat(foundClub).isEmpty();
        assertThat(allClubs).isEmpty();
    }

    @Test
    @DisplayName("클럽 정보 업데이트 - 성공")
    void updateClubInfo_Success() {
        // Given
        String newDescription = "업데이트된 클럽 설명";
        String newInstagramId = "@updated_club";

        // When
        club.setDescription(newDescription);
        club.setInstagramId(newInstagramId);
        Club savedClub = clubRepository.save(club);

        // Then
        assertThat(savedClub.getDescription()).isEqualTo(newDescription);
        assertThat(savedClub.getInstagramId()).isEqualTo(newInstagramId);
        
        // 다시 조회해서 확인
        Optional<Club> foundClub = clubRepository.findByIdAndDeletedAtIsNull(club.getId());
        assertThat(foundClub).isPresent();
        assertThat(foundClub.get().getDescription()).isEqualTo(newDescription);
        assertThat(foundClub.get().getInstagramId()).isEqualTo(newInstagramId);
    }

    @Test
    @DisplayName("여러 클럽 생성 및 페이징 테스트")
    void multipleClubsAndPaging_Success() {
        // Given
        for (int i = 1; i <= 5; i++) {
            Club newClub = new Club();
            newClub.setName("테스트클럽" + i);
            newClub.setDescription("테스트 클럽 설명 " + i);
            newClub.setUniversity(university);
            clubRepository.save(newClub);
        }

        // When
        Pageable pageable = PageRequest.of(0, 3);
        Page<Club> clubs = clubRepository.findAllByDeletedAtIsNull(pageable);

        // Then
        assertThat(clubs.getContent()).hasSize(3);
        assertThat(clubs.getTotalElements()).isEqualTo(6); // 기존 1개 + 새로 생성한 5개
        assertThat(clubs.getTotalPages()).isEqualTo(2);
        assertThat(clubs.hasNext()).isTrue();
    }

    @Test
    @DisplayName("클럽 저장 시 생성일시와 수정일시 자동 설정")
    void saveClub_AutoSetTimestamps() {
        // Given
        Club newClub = new Club();
        newClub.setName("신규 클럽");
        newClub.setUniversity(university);

        // When
        Club savedClub = clubRepository.save(newClub);

        // Then
        assertThat(savedClub.getCreatedAt()).isNotNull();
        assertThat(savedClub.getUpdatedAt()).isNotNull();
        assertThat(savedClub.getCreatedAt()).isEqualTo(savedClub.getUpdatedAt());
    }

    @Test
    @DisplayName("클럽 수정 시 수정일시 업데이트")
    void updateClub_UpdateTimestamp() throws InterruptedException {
        // Given
        LocalDateTime originalUpdatedAt = club.getUpdatedAt();
        Thread.sleep(1000); // 시간 차이를 만들기 위해 1초 대기

        // When
        club.setDescription("수정된 설명");
        club.setUpdatedAt(LocalDateTime.now().plusSeconds(1)); // 명시적으로 미래 시간 설정
        Club updatedClub = clubRepository.save(club);

        // Then
        assertThat(updatedClub.getUpdatedAt()).isAfter(originalUpdatedAt);
        assertThat(updatedClub.getCreatedAt()).isEqualTo(club.getCreatedAt()); // 생성일시는 변경되지 않음
    }
}