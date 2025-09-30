package com.jandi.band_backend.user.service;

import com.jandi.band_backend.club.entity.Club;
import com.jandi.band_backend.club.entity.ClubMember;
import com.jandi.band_backend.club.entity.ClubPhoto;
import com.jandi.band_backend.club.repository.ClubMemberRepository;
import com.jandi.band_backend.club.repository.ClubPhotoRepository;
import com.jandi.band_backend.team.entity.Team;
import com.jandi.band_backend.team.entity.TeamMember;
import com.jandi.band_backend.team.repository.TeamMemberRepository;
import com.jandi.band_backend.univ.entity.University;
import com.jandi.band_backend.user.dto.MyClubRespDTO;
import com.jandi.band_backend.user.dto.MyTeamRespDTO;
import com.jandi.band_backend.user.entity.Users;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MyPageService 마이페이지 테스트")
class MyPageServiceTest {

    @Mock
    private ClubMemberRepository clubMemberRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private ClubPhotoRepository clubPhotoRepository;

    @InjectMocks
    private MyPageService myPageService;

    @Test
    @DisplayName("내가 참가한 동아리 목록 조회 성공")
    void getMyClubs_Success() {
        // Given
        Integer userId = 1;
        
        University university = createMockUniversity();
        Club club = createMockClub(university);
        Users user = createMockUser();
        ClubMember clubMember = createMockClubMember(club, user);
        ClubPhoto clubPhoto = createMockClubPhoto(club);
        
        when(clubMemberRepository.findByUserIdAndClubDeletedAtIsNullAndDeletedAtIsNullOrderByJoinedAtDesc(userId))
                .thenReturn(Arrays.asList(clubMember));
        when(clubPhotoRepository.findByClubIdAndIsCurrentTrueAndDeletedAtIsNull(club.getId()))
                .thenReturn(Optional.of(clubPhoto));
        when(clubMemberRepository.countByClubIdAndDeletedAtIsNull(club.getId()))
                .thenReturn(10);

        // When
        List<MyClubRespDTO> result = myPageService.getMyClubs(userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        
        MyClubRespDTO clubResp = result.get(0);
        assertThat(clubResp.getId()).isEqualTo(club.getId());
        assertThat(clubResp.getName()).isEqualTo("테스트 동아리");
        assertThat(clubResp.getUniversityName()).isEqualTo("테스트 대학교");
        assertThat(clubResp.getMemberCount()).isEqualTo(10);
        assertThat(clubResp.getPhotoUrl()).isEqualTo("https://example.com/club-photo.jpg");
        assertThat(clubResp.getMyRole()).isEqualTo(ClubMember.MemberRole.MEMBER);

        verify(clubMemberRepository).findByUserIdAndClubDeletedAtIsNullAndDeletedAtIsNullOrderByJoinedAtDesc(userId);
        verify(clubPhotoRepository).findByClubIdAndIsCurrentTrueAndDeletedAtIsNull(club.getId());
        verify(clubMemberRepository).countByClubIdAndDeletedAtIsNull(club.getId());
    }

    @Test
    @DisplayName("내가 참가한 동아리 목록 조회 - 빈 목록")
    void getMyClubs_EmptyList() {
        // Given
        Integer userId = 1;
        
        when(clubMemberRepository.findByUserIdAndClubDeletedAtIsNullAndDeletedAtIsNullOrderByJoinedAtDesc(userId))
                .thenReturn(Collections.emptyList());

        // When
        List<MyClubRespDTO> result = myPageService.getMyClubs(userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        verify(clubMemberRepository).findByUserIdAndClubDeletedAtIsNullAndDeletedAtIsNullOrderByJoinedAtDesc(userId);
        verify(clubPhotoRepository, never()).findByClubIdAndIsCurrentTrueAndDeletedAtIsNull(anyInt());
        verify(clubMemberRepository, never()).countByClubIdAndDeletedAtIsNull(anyInt());
    }

    @Test
    @DisplayName("내가 참가한 동아리 목록 조회 - 사진 없는 동아리")
    void getMyClubs_NoPhoto() {
        // Given
        Integer userId = 1;
        
        University university = createMockUniversity();
        Club club = createMockClub(university);
        Users user = createMockUser();
        ClubMember clubMember = createMockClubMember(club, user);
        
        when(clubMemberRepository.findByUserIdAndClubDeletedAtIsNullAndDeletedAtIsNullOrderByJoinedAtDesc(userId))
                .thenReturn(Arrays.asList(clubMember));
        when(clubPhotoRepository.findByClubIdAndIsCurrentTrueAndDeletedAtIsNull(club.getId()))
                .thenReturn(Optional.empty()); // 사진 없음
        when(clubMemberRepository.countByClubIdAndDeletedAtIsNull(club.getId()))
                .thenReturn(5);

        // When
        List<MyClubRespDTO> result = myPageService.getMyClubs(userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        
        MyClubRespDTO clubResp = result.get(0);
        assertThat(clubResp.getPhotoUrl()).isNull(); // 사진이 없으면 null

        verify(clubPhotoRepository).findByClubIdAndIsCurrentTrueAndDeletedAtIsNull(club.getId());
    }

    @Test
    @DisplayName("내가 참가한 팀 목록 조회 성공")
    void getMyTeams_Success() {
        // Given
        Integer userId = 1;
        
        University university = createMockUniversity();
        Club club = createMockClub(university);
        Users user = createMockUser();
        Users creator = createMockCreator();
        Team team = createMockTeam(club, creator);
        TeamMember teamMember = createMockTeamMember(team, user);
        
        when(teamMemberRepository.findByUserIdAndTeamDeletedAtIsNullAndDeletedAtIsNullOrderByJoinedAtDesc(userId))
                .thenReturn(Arrays.asList(teamMember));
        when(teamMemberRepository.countByTeamIdAndDeletedAtIsNull(team.getId()))
                .thenReturn(4);

        // When
        List<MyTeamRespDTO> result = myPageService.getMyTeams(userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        
        MyTeamRespDTO teamResp = result.get(0);
        assertThat(teamResp.getId()).isEqualTo(team.getId());
        assertThat(teamResp.getName()).isEqualTo("테스트 팀");
        assertThat(teamResp.getClubId()).isEqualTo(club.getId());
        assertThat(teamResp.getClubName()).isEqualTo("테스트 동아리");
        assertThat(teamResp.getCreatorId()).isEqualTo(creator.getId());
        assertThat(teamResp.getCreatorName()).isEqualTo("팀 생성자");
        assertThat(teamResp.getMemberCount()).isEqualTo(4);

        verify(teamMemberRepository).findByUserIdAndTeamDeletedAtIsNullAndDeletedAtIsNullOrderByJoinedAtDesc(userId);
        verify(teamMemberRepository).countByTeamIdAndDeletedAtIsNull(team.getId());
    }

    @Test
    @DisplayName("내가 참가한 팀 목록 조회 - 빈 목록")
    void getMyTeams_EmptyList() {
        // Given
        Integer userId = 1;
        
        when(teamMemberRepository.findByUserIdAndTeamDeletedAtIsNullAndDeletedAtIsNullOrderByJoinedAtDesc(userId))
                .thenReturn(Collections.emptyList());

        // When
        List<MyTeamRespDTO> result = myPageService.getMyTeams(userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        verify(teamMemberRepository).findByUserIdAndTeamDeletedAtIsNullAndDeletedAtIsNullOrderByJoinedAtDesc(userId);
        verify(teamMemberRepository, never()).countByTeamIdAndDeletedAtIsNull(anyInt());
    }

    @Test
    @DisplayName("내가 참가한 팀 목록 조회 - 여러 팀")
    void getMyTeams_MultipleTeams() {
        // Given
        Integer userId = 1;
        
        University university = createMockUniversity();
        Club club = createMockClub(university);
        Users user = createMockUser();
        Users creator = createMockCreator();
        
        // 두 개의 팀 생성
        Team team1 = createMockTeam(club, creator);
        team1.setId(1);
        team1.setName("첫 번째 팀");
        
        Team team2 = createMockTeam(club, creator);
        team2.setId(2);
        team2.setName("두 번째 팀");
        
        TeamMember teamMember1 = createMockTeamMember(team1, user);
        TeamMember teamMember2 = createMockTeamMember(team2, user);
        
        when(teamMemberRepository.findByUserIdAndTeamDeletedAtIsNullAndDeletedAtIsNullOrderByJoinedAtDesc(userId))
                .thenReturn(Arrays.asList(teamMember1, teamMember2));
        when(teamMemberRepository.countByTeamIdAndDeletedAtIsNull(team1.getId()))
                .thenReturn(3);
        when(teamMemberRepository.countByTeamIdAndDeletedAtIsNull(team2.getId()))
                .thenReturn(5);

        // When
        List<MyTeamRespDTO> result = myPageService.getMyTeams(userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        
        assertThat(result.get(0).getName()).isEqualTo("첫 번째 팀");
        assertThat(result.get(0).getMemberCount()).isEqualTo(3);
        assertThat(result.get(1).getName()).isEqualTo("두 번째 팀");
        assertThat(result.get(1).getMemberCount()).isEqualTo(5);

        verify(teamMemberRepository).countByTeamIdAndDeletedAtIsNull(team1.getId());
        verify(teamMemberRepository).countByTeamIdAndDeletedAtIsNull(team2.getId());
    }

    // Helper methods for creating mock objects
    private University createMockUniversity() {
        University university = new University();
        university.setId(1);
        university.setName("테스트 대학교");
        return university;
    }

    private Club createMockClub(University university) {
        Club club = new Club();
        club.setId(1);
        club.setName("테스트 동아리");
        club.setDescription("테스트 동아리 설명");
        club.setUniversity(university);
        return club;
    }

    private Users createMockUser() {
        Users user = new Users();
        user.setId(1);
        user.setNickname("테스트 사용자");
        return user;
    }

    private Users createMockCreator() {
        Users creator = new Users();
        creator.setId(2);
        creator.setNickname("팀 생성자");
        return creator;
    }

    private ClubMember createMockClubMember(Club club, Users user) {
        ClubMember clubMember = new ClubMember();
        clubMember.setId(1);
        clubMember.setClub(club);
        clubMember.setUser(user);
        clubMember.setRole(ClubMember.MemberRole.MEMBER);
        clubMember.setJoinedAt(LocalDateTime.now());
        return clubMember;
    }

    private ClubPhoto createMockClubPhoto(Club club) {
        ClubPhoto clubPhoto = new ClubPhoto();
        clubPhoto.setId(1);
        clubPhoto.setClub(club);
        clubPhoto.setImageUrl("https://example.com/club-photo.jpg");
        clubPhoto.setIsCurrent(true);
        return clubPhoto;
    }

    private Team createMockTeam(Club club, Users creator) {
        Team team = new Team();
        team.setId(1);
        team.setName("테스트 팀");
        team.setDescription("테스트 팀 설명");
        team.setClub(club);
        team.setCreator(creator);
        team.setCreatedAt(LocalDateTime.now());
        return team;
    }

    private TeamMember createMockTeamMember(Team team, Users user) {
        TeamMember teamMember = new TeamMember();
        teamMember.setId(1);
        teamMember.setTeam(team);
        teamMember.setUser(user);
        teamMember.setJoinedAt(LocalDateTime.now());
        return teamMember;
    }
}