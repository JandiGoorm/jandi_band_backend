package com.jandi.band_backend.club.service;

import com.jandi.band_backend.club.dto.ClubDetailRespDTO;
import com.jandi.band_backend.club.dto.ClubMembersRespDTO;
import com.jandi.band_backend.club.dto.ClubReqDTO;
import com.jandi.band_backend.club.dto.ClubRespDTO;
import com.jandi.band_backend.club.entity.Club;
import com.jandi.band_backend.club.entity.ClubMember;
import com.jandi.band_backend.club.entity.ClubPhoto;
import com.jandi.band_backend.club.repository.ClubMemberRepository;
import com.jandi.band_backend.club.repository.ClubPhotoRepository;
import com.jandi.band_backend.club.repository.ClubRepository;
import com.jandi.band_backend.global.exception.UniversityNotFoundException;
import com.jandi.band_backend.global.util.EntityValidationUtil;
import com.jandi.band_backend.global.util.UserValidationUtil;
import com.jandi.band_backend.univ.entity.University;
import com.jandi.band_backend.univ.repository.UniversityRepository;
import com.jandi.band_backend.user.entity.Users;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClubService 단위 테스트")
class ClubServiceTest {

    @Mock
    private ClubRepository clubRepository;

    @Mock
    private ClubMemberRepository clubMemberRepository;

    @Mock
    private ClubPhotoRepository clubPhotoRepository;

    @Mock
    private UniversityRepository universityRepository;

    @Mock
    private EntityValidationUtil entityValidationUtil;

    @Mock
    private UserValidationUtil userValidationUtil;

    @InjectMocks
    private ClubService clubService;

    @Test
    @DisplayName("클럽 생성 성공")
    void createClub_Success() {
        // Given
        Integer userId = 1;
        ClubReqDTO request = ClubReqDTO.builder()
                .name("테스트 밴드")
                .description("테스트 밴드 설명")
                .chatroomUrl("https://openchat.kakao.com/test")
                .instagramId("testband")
                .universityId(1)
                .build();

        Users user = new Users();
        user.setId(userId);
        user.setNickname("테스트사용자");

        University university = new University();
        university.setId(1);
        university.setName("테스트대학교");

        Club savedClub = new Club();
        savedClub.setId(1);
        savedClub.setName(request.getName());
        savedClub.setDescription(request.getDescription());
        savedClub.setChatroomUrl(request.getChatroomUrl());
        savedClub.setInstagramId(request.getInstagramId());
        savedClub.setUniversity(university);
        savedClub.setCreatedAt(LocalDateTime.now());

        ClubPhoto clubPhoto = new ClubPhoto();
        clubPhoto.setId(1);
        clubPhoto.setClub(savedClub);
        clubPhoto.setImageUrl("default-photo-url");
        clubPhoto.setIsCurrent(true);

        ClubMember clubMember = new ClubMember();
        clubMember.setId(1);
        clubMember.setClub(savedClub);
        clubMember.setUser(user);
        clubMember.setRole(ClubMember.MemberRole.REPRESENTATIVE);

        when(userValidationUtil.getUserById(userId)).thenReturn(user);
        when(universityRepository.findById(1)).thenReturn(Optional.of(university));
        when(clubRepository.save(any(Club.class))).thenReturn(savedClub);
        when(clubPhotoRepository.save(any(ClubPhoto.class))).thenReturn(clubPhoto);
        when(clubMemberRepository.save(any(ClubMember.class))).thenReturn(clubMember);

        // When
        ClubDetailRespDTO result = clubService.createClub(request, userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("테스트 밴드");
        assertThat(result.getDescription()).isEqualTo("테스트 밴드 설명");
        assertThat(result.getChatroomUrl()).isEqualTo("https://openchat.kakao.com/test");
        assertThat(result.getInstagramId()).isEqualTo("testband");

        verify(userValidationUtil).getUserById(userId);
        verify(universityRepository).findById(1);
        verify(clubRepository).save(any(Club.class));
        verify(clubPhotoRepository).save(any(ClubPhoto.class));
        verify(clubMemberRepository).save(any(ClubMember.class));
    }

    @Test
    @DisplayName("클럽 생성 실패 - 존재하지 않는 대학")
    void createClub_UniversityNotFound() {
        // Given
        Integer userId = 1;
        ClubReqDTO request = ClubReqDTO.builder()
                .name("테스트 밴드")
                .description("테스트 밴드 설명")
                .chatroomUrl("https://openchat.kakao.com/test")
                .instagramId("testband")
                .universityId(999)
                .build();

        Users user = new Users();
        user.setId(userId);
        user.setNickname("테스트사용자");

        when(userValidationUtil.getUserById(userId)).thenReturn(user);
        when(universityRepository.findById(999)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> clubService.createClub(request, userId))
                .isInstanceOf(UniversityNotFoundException.class)
                .hasMessageContaining("대학을 찾을 수 없습니다");

        verify(userValidationUtil).getUserById(userId);
        verify(universityRepository).findById(999);
        verify(clubRepository, never()).save(any(Club.class));
    }

    @Test
    @DisplayName("클럽 목록 조회 성공")
    void getClubList_Success() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        University university = new University();
        university.setId(1);
        university.setName("테스트대학교");

        Club club1 = new Club();
        club1.setId(1);
        club1.setName("밴드 1");
        club1.setDescription("설명 1");
        club1.setUniversity(university);
        club1.setCreatedAt(LocalDateTime.now());

        Club club2 = new Club();
        club2.setId(2);
        club2.setName("밴드 2");
        club2.setDescription("설명 2");
        club2.setUniversity(university);
        club2.setCreatedAt(LocalDateTime.now());

        List<Club> clubs = List.of(club1, club2);
        Page<Club> clubPage = new PageImpl<>(clubs, pageable, clubs.size());

        ClubPhoto photo1 = new ClubPhoto();
        photo1.setClub(club1);
        photo1.setImageUrl("photo1.jpg");
        photo1.setIsCurrent(true);

        ClubPhoto photo2 = new ClubPhoto();
        photo2.setClub(club2);
        photo2.setImageUrl("photo2.jpg");
        photo2.setIsCurrent(true);

        when(clubRepository.findAllByDeletedAtIsNull(pageable)).thenReturn(clubPage);
        when(clubPhotoRepository.findByClubIdAndIsCurrentTrueAndDeletedAtIsNull(1))
                .thenReturn(Optional.of(photo1));
        when(clubPhotoRepository.findByClubIdAndIsCurrentTrueAndDeletedAtIsNull(2))
                .thenReturn(Optional.of(photo2));
        when(clubMemberRepository.countByClubIdAndDeletedAtIsNull(1)).thenReturn(5);
        when(clubMemberRepository.countByClubIdAndDeletedAtIsNull(2)).thenReturn(8);

        // When
        Page<ClubRespDTO> result = clubService.getClubList(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getName()).isEqualTo("밴드 1");
        assertThat(result.getContent().get(1).getName()).isEqualTo("밴드 2");

        verify(clubRepository).findAllByDeletedAtIsNull(pageable);
        verify(clubPhotoRepository).findByClubIdAndIsCurrentTrueAndDeletedAtIsNull(1);
        verify(clubPhotoRepository).findByClubIdAndIsCurrentTrueAndDeletedAtIsNull(2);
        verify(clubMemberRepository).countByClubIdAndDeletedAtIsNull(1);
        verify(clubMemberRepository).countByClubIdAndDeletedAtIsNull(2);
    }

    @Test
    @DisplayName("클럽 상세 조회 성공")
    void getClubDetail_Success() {
        // Given
        Integer clubId = 1;

        University university = new University();
        university.setId(1);
        university.setName("테스트대학교");

        Club club = new Club();
        club.setId(clubId);
        club.setName("테스트 밴드");
        club.setDescription("테스트 밴드 설명");
        club.setChatroomUrl("https://openchat.kakao.com/test");
        club.setInstagramId("testband");
        club.setUniversity(university);
        club.setCreatedAt(LocalDateTime.now());

        ClubPhoto clubPhoto = new ClubPhoto();
        clubPhoto.setClub(club);
        clubPhoto.setImageUrl("club-photo.jpg");
        clubPhoto.setIsCurrent(true);

        Users representative = new Users();
        representative.setId(1);
        representative.setNickname("대표");

        ClubMember clubMember = new ClubMember();
        clubMember.setUser(representative);
        clubMember.setRole(ClubMember.MemberRole.REPRESENTATIVE);

        when(entityValidationUtil.validateClubExists(clubId)).thenReturn(club);
        when(clubPhotoRepository.findByClubIdAndIsCurrentTrueAndDeletedAtIsNull(clubId))
                .thenReturn(Optional.of(clubPhoto));
        when(clubMemberRepository.countByClubIdAndDeletedAtIsNull(clubId)).thenReturn(10);

        // When
        ClubDetailRespDTO result = clubService.getClubDetail(clubId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("테스트 밴드");
        assertThat(result.getDescription()).isEqualTo("테스트 밴드 설명");
        assertThat(result.getChatroomUrl()).isEqualTo("https://openchat.kakao.com/test");
        assertThat(result.getInstagramId()).isEqualTo("testband");

        verify(entityValidationUtil).validateClubExists(clubId);
        verify(clubPhotoRepository).findByClubIdAndIsCurrentTrueAndDeletedAtIsNull(clubId);
        verify(clubMemberRepository).countByClubIdAndDeletedAtIsNull(clubId);
    }

    @Test
    @DisplayName("클럽 멤버 목록 조회 성공")
    void getClubMembers_Success() {
        // Given
        Integer clubId = 1;

        Club club = new Club();
        club.setId(clubId);
        club.setName("테스트 밴드");

        Users user1 = new Users();
        user1.setId(1);
        user1.setNickname("사용자1");
        user1.setPosition(Users.Position.GUITAR);

        Users user2 = new Users();
        user2.setId(2);
        user2.setNickname("사용자2");
        user2.setPosition(Users.Position.DRUM);

        ClubMember member1 = new ClubMember();
        member1.setId(1);
        member1.setClub(club);
        member1.setUser(user1);
        member1.setRole(ClubMember.MemberRole.REPRESENTATIVE);
        member1.setJoinedAt(LocalDateTime.now());

        ClubMember member2 = new ClubMember();
        member2.setId(2);
        member2.setClub(club);
        member2.setUser(user2);
        member2.setRole(ClubMember.MemberRole.MEMBER);
        member2.setJoinedAt(LocalDateTime.now());

        List<ClubMember> members = List.of(member1, member2);

        when(entityValidationUtil.validateClubExists(clubId)).thenReturn(club);
        when(clubMemberRepository.findByClubIdAndDeletedAtIsNull(clubId)).thenReturn(members);

        // When
        ClubMembersRespDTO result = clubService.getClubMembers(clubId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getMembers()).hasSize(2);
        assertThat(result.getMembers().get(0).getName()).isEqualTo("사용자1");
        assertThat(result.getMembers().get(1).getName()).isEqualTo("사용자2");

        verify(entityValidationUtil).validateClubExists(clubId);
        verify(clubMemberRepository).findByClubIdAndDeletedAtIsNull(clubId);
    }
}