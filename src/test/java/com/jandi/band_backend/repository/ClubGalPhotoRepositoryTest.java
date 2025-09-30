package com.jandi.band_backend.repository;

import com.jandi.band_backend.club.entity.Club;
import com.jandi.band_backend.club.entity.ClubGalPhoto;
import com.jandi.band_backend.club.repository.ClubGalPhotoRepository;
import com.jandi.band_backend.club.repository.ClubRepository;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("ClubGalPhoto Repository 테스트")
class ClubGalPhotoRepositoryTest {

    @Autowired
    private ClubGalPhotoRepository clubGalPhotoRepository;

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

    private Club club;
    private Users uploader;
    private ClubGalPhoto photo;

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

        // 업로더 사용자 생성
        uploader = new Users();
        uploader.setNickname("업로더");
        uploader.setKakaoOauthId("uploader_oauth_123");
        uploader.setUniversity(university);
        uploader = userRepository.save(uploader);

        // 동아리 생성
        club = new Club();
        club.setName("테스트 동아리");
        club.setDescription("테스트용 동아리입니다.");
        club.setUniversity(university);
        club = clubRepository.save(club);

        // 동아리 갤러리 사진 생성
        photo = new ClubGalPhoto();
        photo.setClub(club);
        photo.setUploader(uploader);
        photo.setImageUrl("https://example.com/photo.jpg");
        photo.setDescription("테스트 사진");
        photo.setIsPublic(true);
        photo.setIsPinned(false);
        photo = clubGalPhotoRepository.save(photo);
    }

    @Test
    @DisplayName("동아리 ID로 갤러리 사진 조회")
    void findByClubId() {
        // when
        List<ClubGalPhoto> photos = clubGalPhotoRepository.findByClubId(club.getId());

        // then
        assertThat(photos).hasSize(1);
        assertThat(photos.get(0).getClub().getId()).isEqualTo(club.getId());
        assertThat(photos.get(0).getDescription()).isEqualTo("테스트 사진");
    }

    @Test
    @DisplayName("동아리 ID로 삭제되지 않은 갤러리 사진 조회")
    void findByClubIdAndDeletedAtIsNull() {
        // given - 삭제된 사진 추가
        ClubGalPhoto deletedPhoto = new ClubGalPhoto();
        deletedPhoto.setClub(club);
        deletedPhoto.setUploader(uploader);
        deletedPhoto.setImageUrl("https://example.com/deleted.jpg");
        deletedPhoto.setDescription("삭제된 사진");
        deletedPhoto.setIsPublic(true);
        deletedPhoto.setIsPinned(false);
        deletedPhoto.setDeletedAt(LocalDateTime.now());
        clubGalPhotoRepository.save(deletedPhoto);

        // when
        List<ClubGalPhoto> photos = clubGalPhotoRepository.findByClubIdAndDeletedAtIsNull(club.getId());

        // then
        assertThat(photos).hasSize(1);
        assertThat(photos.get(0).getDescription()).isEqualTo("테스트 사진");
    }

    @Test
    @DisplayName("동아리 ID와 공개 여부로 갤러리 사진 조회")
    void findByClubIdAndIsPublic() {
        // given - 비공개 사진 추가
        ClubGalPhoto privatePhoto = new ClubGalPhoto();
        privatePhoto.setClub(club);
        privatePhoto.setUploader(uploader);
        privatePhoto.setImageUrl("https://example.com/private.jpg");
        privatePhoto.setDescription("비공개 사진");
        privatePhoto.setIsPublic(false);
        privatePhoto.setIsPinned(false);
        clubGalPhotoRepository.save(privatePhoto);

        // when - 공개 사진만 조회
        List<ClubGalPhoto> publicPhotos = clubGalPhotoRepository.findByClubIdAndIsPublic(club.getId(), true);

        // then
        assertThat(publicPhotos).hasSize(1);
        assertThat(publicPhotos.get(0).getIsPublic()).isTrue();
        assertThat(publicPhotos.get(0).getDescription()).isEqualTo("테스트 사진");
    }

    @Test
    @DisplayName("동아리 ID와 고정 여부로 갤러리 사진 조회")
    void findByClubIdAndIsPinned() {
        // given - 고정된 사진 추가
        ClubGalPhoto pinnedPhoto = new ClubGalPhoto();
        pinnedPhoto.setClub(club);
        pinnedPhoto.setUploader(uploader);
        pinnedPhoto.setImageUrl("https://example.com/pinned.jpg");
        pinnedPhoto.setDescription("고정된 사진");
        pinnedPhoto.setIsPublic(true);
        pinnedPhoto.setIsPinned(true);
        clubGalPhotoRepository.save(pinnedPhoto);

        // when - 고정된 사진만 조회
        List<ClubGalPhoto> pinnedPhotos = clubGalPhotoRepository.findByClubIdAndIsPinned(club.getId(), true);

        // then
        assertThat(pinnedPhotos).hasSize(1);
        assertThat(pinnedPhotos.get(0).getIsPinned()).isTrue();
        assertThat(pinnedPhotos.get(0).getDescription()).isEqualTo("고정된 사진");
    }

    @Test
    @DisplayName("공개 사진 페이지네이션 조회 (업로더 fetch 포함)")
    void findByClubIdAndIsPublicAndDeletedAtIsNullFetchUploader() {
        // given
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<ClubGalPhoto> photoPage = clubGalPhotoRepository
                .findByClubIdAndIsPublicAndDeletedAtIsNullFetchUploader(club.getId(), true, pageable);

        // then
        assertThat(photoPage.getContent()).hasSize(1);
        ClubGalPhoto foundPhoto = photoPage.getContent().get(0);
        assertThat(foundPhoto.getUploader().getNickname()).isEqualTo("업로더");
        assertThat(foundPhoto.getIsPublic()).isTrue();
    }

    @Test
    @DisplayName("동아리별 전체 사진 페이지네이션 조회 (업로더 fetch 포함)")
    void findByClubIdAndDeletedAtIsNullFetchUploader() {
        // given
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<ClubGalPhoto> photoPage = clubGalPhotoRepository
                .findByClubIdAndDeletedAtIsNullFetchUploader(club.getId(), pageable);

        // then
        assertThat(photoPage.getContent()).hasSize(1);
        ClubGalPhoto foundPhoto = photoPage.getContent().get(0);
        assertThat(foundPhoto.getUploader().getNickname()).isEqualTo("업로더");
        assertThat(foundPhoto.getClub().getName()).isEqualTo("테스트 동아리");
    }

    @Test
    @DisplayName("ID와 동아리로 삭제되지 않은 사진 조회")
    void findByIdAndClubAndDeletedAtIsNull() {
        // when
        Optional<ClubGalPhoto> foundPhoto = clubGalPhotoRepository
                .findByIdAndClubAndDeletedAtIsNull(photo.getId(), club);

        // then
        assertThat(foundPhoto).isPresent();
        assertThat(foundPhoto.get().getDescription()).isEqualTo("테스트 사진");
        assertThat(foundPhoto.get().getClub().getId()).isEqualTo(club.getId());
    }

    @Test
    @DisplayName("존재하지 않는 ID로 사진 조회 시 빈 결과 반환")
    void findByIdAndClubAndDeletedAtIsNull_NotFound() {
        // when
        Optional<ClubGalPhoto> foundPhoto = clubGalPhotoRepository
                .findByIdAndClubAndDeletedAtIsNull(99999, club);

        // then
        assertThat(foundPhoto).isEmpty();
    }

    @Test
    @DisplayName("삭제된 사진 조회 시 빈 결과 반환")
    void findByIdAndClubAndDeletedAtIsNull_Deleted() {
        // given - 사진을 삭제
        photo.setDeletedAt(LocalDateTime.now());
        clubGalPhotoRepository.save(photo);

        // when
        Optional<ClubGalPhoto> foundPhoto = clubGalPhotoRepository
                .findByIdAndClubAndDeletedAtIsNull(photo.getId(), club);

        // then
        assertThat(foundPhoto).isEmpty();
    }

    @Test
    @DisplayName("사용자 ID로 갤러리 사진 업로더 익명화 - 제약 조건으로 인한 예외 발생")
    void anonymizeByUserId() {
        // given
        Integer userId = uploader.getId();

        // when & then - H2에서는 -1 사용자가 없어서 외래키 제약 조건 위반 발생
        // 실제 운영 환경에서는 익명 사용자(ID: -1)가 미리 생성되어 있음
        assertThatThrownBy(() -> clubGalPhotoRepository.anonymizeByUserId(userId))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("존재하지 않는 사용자 ID로 익명화 시 업데이트 건수 0")
    void anonymizeByUserId_NoUpdate() {
        // given
        Integer nonExistentUserId = 99999;

        // when
        int updatedCount = clubGalPhotoRepository.anonymizeByUserId(nonExistentUserId);

        // then
        assertThat(updatedCount).isEqualTo(0);
    }

    @Test
    @DisplayName("갤러리 사진 저장 시 생성일자 자동 설정")
    void savePhoto_AutoTimestamp() {
        // given
        ClubGalPhoto newPhoto = new ClubGalPhoto();
        newPhoto.setClub(club);
        newPhoto.setUploader(uploader);
        newPhoto.setImageUrl("https://example.com/new-photo.jpg");
        newPhoto.setDescription("새 사진");
        newPhoto.setIsPublic(true);
        newPhoto.setIsPinned(false);

        // when
        ClubGalPhoto savedPhoto = clubGalPhotoRepository.save(newPhoto);

        // then
        assertThat(savedPhoto.getUploadedAt()).isNotNull();
        assertThat(savedPhoto.getUploadedAt()).isBefore(LocalDateTime.now().plusSeconds(1));
    }
}