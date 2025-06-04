package com.jandi.band_backend.club.service;

import com.jandi.band_backend.club.dto.ClubReqDTO;
import com.jandi.band_backend.club.dto.ClubDetailRespDTO;
import com.jandi.band_backend.club.dto.ClubRespDTO;
import com.jandi.band_backend.club.dto.ClubUpdateReqDTO;
import com.jandi.band_backend.club.dto.ClubMembersRespDTO;
import com.jandi.band_backend.club.entity.Club;
import com.jandi.band_backend.club.entity.ClubMember;
import com.jandi.band_backend.club.entity.ClubPhoto;
import com.jandi.band_backend.club.entity.ClubEvent;
import com.jandi.band_backend.club.repository.ClubMemberRepository;
import com.jandi.band_backend.club.repository.ClubPhotoRepository;
import com.jandi.band_backend.club.repository.ClubRepository;
import com.jandi.band_backend.club.repository.ClubEventRepository;
import com.jandi.band_backend.team.repository.TeamRepository;
import com.jandi.band_backend.team.repository.TeamMemberRepository;
import com.jandi.band_backend.team.repository.TeamEventRepository;
import com.jandi.band_backend.team.entity.Team;
import com.jandi.band_backend.team.entity.TeamMember;
import com.jandi.band_backend.team.entity.TeamEvent;
import com.jandi.band_backend.univ.dto.UniversityRespDTO;
import com.jandi.band_backend.univ.entity.University;
import com.jandi.band_backend.univ.repository.UniversityRepository;
import com.jandi.band_backend.user.entity.Users;
import com.jandi.band_backend.global.exception.ClubNotFoundException;
import com.jandi.band_backend.global.exception.ResourceNotFoundException;
import com.jandi.band_backend.global.exception.UniversityNotFoundException;
import com.jandi.band_backend.global.util.EntityValidationUtil;
import com.jandi.band_backend.global.util.S3FileManagementUtil;
import com.jandi.band_backend.global.util.PermissionValidationUtil;
import com.jandi.band_backend.global.util.UserValidationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClubService {

    private final ClubRepository clubRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final ClubPhotoRepository clubPhotoRepository;
    private final ClubEventRepository clubEventRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamEventRepository teamEventRepository;
    private final UniversityRepository universityRepository;
    private final EntityValidationUtil entityValidationUtil;
    private final S3FileManagementUtil s3FileManagementUtil;
    private final PermissionValidationUtil permissionValidationUtil;
    private final UserValidationUtil userValidationUtil;

    private static final String CLUB_PHOTO_DIR = "club-photo";
    private static final String DEFAULT_CLUB_PHOTO_URL = "https://jandi-rhythmeet.s3.ap-northeast-2.amazonaws.com/club-photo/rhythmeet.webp";

    @Transactional
    public ClubDetailRespDTO createClub(ClubReqDTO request, Integer userId) {
        // 사용자 확인
        Users user = userValidationUtil.getUserById(userId);

        // 동아리 생성
        Club club = new Club();
        club.setName(request.getName());
        club.setChatroomUrl(request.getChatroomUrl());
        club.setDescription(request.getDescription());
        club.setInstagramId(request.getInstagramId());
        club.setCreatedAt(LocalDateTime.now());
        club.setUpdatedAt(LocalDateTime.now());

        // 대학 정보 설정 (연합 동아리인 경우 null)
        if (request.getUniversityId() != null) {
            String errorMessage = "대학을 찾을 수 없습니다. ID: " + request.getUniversityId();
            University university = universityRepository.findById(request.getUniversityId())
                    .orElseThrow(() -> new UniversityNotFoundException(errorMessage));
            club.setUniversity(university);
        }

        Club savedClub = clubRepository.save(club);

        // 동아리 기본 사진 설정
        ClubPhoto defaultPhoto = new ClubPhoto();
        defaultPhoto.setClub(savedClub);
        defaultPhoto.setImageUrl(DEFAULT_CLUB_PHOTO_URL);
        defaultPhoto.setIsCurrent(true);
        clubPhotoRepository.save(defaultPhoto);

        // 동아리 멤버 추가 (생성자를 대표자로 설정)
        ClubMember clubMember = new ClubMember();
        clubMember.setClub(savedClub);
        clubMember.setUser(user);
        clubMember.setRole(ClubMember.MemberRole.REPRESENTATIVE);
        clubMember.setJoinedAt(LocalDateTime.now());
        clubMember.setUpdatedAt(LocalDateTime.now());

        clubMemberRepository.save(clubMember);

        return convertToClubDetailRespDTO(savedClub, DEFAULT_CLUB_PHOTO_URL, 1, userId);
    }

    @Transactional(readOnly = true)
    public Page<ClubRespDTO> getClubList(Pageable pageable) {
        // 동아리 목록 조회
        Page<Club> clubPage = clubRepository.findAllByDeletedAtIsNull(pageable);

        return clubPage.map(club -> {
            String photoUrl = getClubMainPhotoUrl(club.getId());
            int memberCount = clubMemberRepository.countByClubIdAndDeletedAtIsNull(club.getId());

            return convertToClubRespDTO(club, photoUrl, memberCount);
        });
    }

    @Transactional(readOnly = true)
    public ClubDetailRespDTO getClubDetail(Integer clubId) {
        Club club = entityValidationUtil.validateClubExists(clubId);

        String photoUrl = getClubMainPhotoUrl(club.getId());
        int memberCount = clubMemberRepository.countByClubIdAndDeletedAtIsNull(clubId);
        Integer representativeId = getClubRepresentativeId(clubId);

        return convertToClubDetailRespDTO(club, photoUrl, memberCount, representativeId);
    }

    @Transactional(readOnly = true)
    public ClubMembersRespDTO getClubMembers(Integer clubId) {
        Club club = entityValidationUtil.validateClubExists(clubId);

        // 동아리 멤버 목록 조회
        List<ClubMember> clubMembers = clubMemberRepository.findByClubIdAndDeletedAtIsNull(clubId);

        // 멤버 정보 변환
        List<ClubMembersRespDTO.MemberInfoDTO> memberInfos = clubMembers.stream()
            .map(this::convertToMemberInfoDTO)
            .toList();

        // 포지션별 카운트 계산
        Map<String, Long> positionCountMap = clubMembers.stream()
                .map(member -> member.getUser().getPosition())
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.groupingBy(
                        Enum::name,
                        Collectors.counting()
                ));

        // 포지션별 카운트 매핑 (기본값 0으로 설정)
        Map<String, Integer> positionCounts = Map.of(
                "VOCAL", positionCountMap.getOrDefault("VOCAL", 0L).intValue(),
                "GUITAR", positionCountMap.getOrDefault("GUITAR", 0L).intValue(),
                "KEYBOARD", positionCountMap.getOrDefault("KEYBOARD", 0L).intValue(),
                "BASS", positionCountMap.getOrDefault("BASS", 0L).intValue(),
                "DRUM", positionCountMap.getOrDefault("DRUM", 0L).intValue()
        );

        return ClubMembersRespDTO.builder()
                .id(club.getId())
                .members(memberInfos)
                .vocalCount(positionCounts.get("VOCAL"))
                .guitarCount(positionCounts.get("GUITAR"))
                .keyboardCount(positionCounts.get("KEYBOARD"))
                .bassCount(positionCounts.get("BASS"))
                .drumCount(positionCounts.get("DRUM"))
                .totalMemberCount(clubMembers.size())
                .build();
    }

    @Transactional
    public ClubDetailRespDTO updateClub(Integer clubId, ClubUpdateReqDTO request, Integer userId) {
        Club club = entityValidationUtil.validateClubExists(clubId);

        // 권한 확인 (동아리 회원이면 수정 가능)
        permissionValidationUtil.validateClubMemberAccess(clubId, userId, "동아리 정보 수정 권한이 없습니다.");

        // 동아리 정보 수정
        if (request.getName() != null) {
            club.setName(request.getName());
        }
        if (request.getChatroomUrl() != null) {
            club.setChatroomUrl(request.getChatroomUrl());
        }
        if (request.getDescription() != null) {
            club.setDescription(request.getDescription());
        }
        if (request.getInstagramId() != null) {
            club.setInstagramId(request.getInstagramId());
        }

        club.setUpdatedAt(LocalDateTime.now());

        Club updatedClub = clubRepository.save(club);
        String photoUrl = getClubMainPhotoUrl(clubId);
        int memberCount = clubMemberRepository.countByClubIdAndDeletedAtIsNull(clubId);
        Integer representativeId = getClubRepresentativeId(clubId);

        return convertToClubDetailRespDTO(updatedClub, photoUrl, memberCount, representativeId);
    }

    @Transactional
    public void transferRepresentative(Integer clubId, Integer currentUserId, Integer newRepresentativeUserId) {
        Club club = entityValidationUtil.validateClubExists(clubId);

        // 현재 사용자가 대표자인지 확인 (ADMIN 권한도 허용)
        permissionValidationUtil.validateClubRepresentativeAccess(clubId, currentUserId, "동아리 대표자만 권한을 위임할 수 있습니다.");

        // 새 대표자가 동아리 멤버인지 확인
        ClubMember newRepresentative = clubMemberRepository.findByClubIdAndUserIdAndDeletedAtIsNull(clubId, newRepresentativeUserId)
                .orElseThrow(() -> new ResourceNotFoundException("위임할 사용자가 해당 동아리의 멤버가 아닙니다."));

        // 자기 자신에게 위임하는 경우 예외 처리
        if (currentUserId.equals(newRepresentativeUserId)) {
            throw new IllegalArgumentException("자기 자신에게는 권한을 위임할 수 없습니다.");
        }

        // 기존 대표자 찾아서 해임
        ClubMember currentRepresentative = clubMemberRepository.findByClubIdAndDeletedAtIsNull(clubId).stream()
                .filter(member -> member.getRole() == ClubMember.MemberRole.REPRESENTATIVE)
                .findFirst()
                .orElse(null);

        if (currentRepresentative != null) {
            currentRepresentative.setRole(ClubMember.MemberRole.MEMBER);
            clubMemberRepository.save(currentRepresentative);
        }

        // 새 대표자 역할 설정
        newRepresentative.setRole(ClubMember.MemberRole.REPRESENTATIVE);
        clubMemberRepository.save(newRepresentative);
    }

    @Transactional
    public void deleteClub(Integer clubId, Integer userId) {
        Club club = entityValidationUtil.validateClubExists(clubId);

        // 권한 확인 (대표자만 삭제 가능)
        permissionValidationUtil.validateClubRepresentativeAccess(clubId, userId, "동아리 삭제 권한이 없습니다.");

        // 동아리에 속한 팀들 연쇄 삭제 처리
        LocalDateTime deletedTime = LocalDateTime.now();
        List<Team> teams = teamRepository.findAllByClubIdAndDeletedAtIsNull(clubId);
        for (Team team : teams) {
            // 팀 멤버들 소프트 삭제
            List<TeamMember> teamMembers = teamMemberRepository.findByTeamIdAndDeletedAtIsNull(team.getId());
            teamMembers.forEach(teamMember -> teamMember.setDeletedAt(deletedTime));
            teamMemberRepository.saveAll(teamMembers);

            // 팀 이벤트들 소프트 삭제
            List<TeamEvent> teamEvents = teamEventRepository.findAllByTeamIdAndDeletedAtIsNull(team.getId());
            teamEvents.forEach(teamEvent -> teamEvent.setDeletedAt(deletedTime));
            teamEventRepository.saveAll(teamEvents);

            // 팀 소프트 삭제
            team.setDeletedAt(deletedTime);
        }
        teamRepository.saveAll(teams);

        // 동아리 갤러리 사진들 S3 삭제 (정희님)

        // 동아리 갤러리 사진들 DB 레코드 소프트 삭제 (정희님)

        // 동아리 대표 사진 S3 삭제
        deleteClubPhoto(clubId, userId);

        // 동아리 대표 사진 DB 레코드 소프트 삭제
        clubPhotoRepository
                .findByClubIdAndIsCurrentTrueAndDeletedAtIsNull(clubId)
                .ifPresent(clubPhoto -> {
                    clubPhoto.setIsCurrent(false);
                    clubPhoto.setDeletedAt(deletedTime);
                    clubPhotoRepository.save(clubPhoto);
                });

        // 동아리 멤버 소프트 삭제
        clubMemberRepository.findByClubIdAndDeletedAtIsNull(clubId).forEach(clubMember -> {
            clubMember.setDeletedAt(deletedTime);
            clubMemberRepository.save(clubMember);
        });

        // 동아리 이벤트 소프트 삭제
        clubEventRepository.findByClubIdAndDeletedAtIsNull(clubId).forEach(clubEvent -> {
            clubEvent.setDeletedAt(deletedTime);
            clubEventRepository.save(clubEvent);
        });

        // 동아리 삭제 (소프트 삭제)
        club.setDeletedAt(deletedTime);
        clubRepository.save(club);
    }

    @Transactional
    public void leaveClub(Integer clubId, Integer currentUserId) {
        Club club = entityValidationUtil.validateClubExists(clubId);

        // 동아리 멤버 권한 확인
        ClubMember clubMember = clubMemberRepository.findByClubIdAndUserIdAndDeletedAtIsNull(clubId, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("해당 동아리의 멤버가 아닙니다."));

        // 대표자인 경우 탈퇴할 수 없음
        if (clubMember.getRole() == ClubMember.MemberRole.REPRESENTATIVE) {
            throw new IllegalStateException("동아리 대표자는 탈퇴할 수 없습니다. 먼저 다른 멤버에게 대표자 권한을 위임해주세요.");
        }

        // 동아리 멤버 소프트 삭제
        clubMember.setDeletedAt(LocalDateTime.now());
        clubMemberRepository.save(clubMember);
    }

    @Transactional
    public void kickMember(Integer clubId, Integer currentUserId, Integer targetUserId) {
        Club club = entityValidationUtil.validateClubExists(clubId);

        // 현재 사용자가 대표자인지 확인 (PermissionValidationUtil 사용)
        permissionValidationUtil.validateClubRepresentativeAccess(clubId, currentUserId, "동아리 대표자만 부원을 강퇴할 수 있습니다.");

        // 강퇴 대상이 동아리 멤버인지 확인
        ClubMember targetMember = clubMemberRepository.findByClubIdAndUserIdAndDeletedAtIsNull(clubId, targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("해당 사용자는 동아리 멤버가 아닙니다."));

        // 대표자를 강퇴할 수 없음
        if (targetMember.getRole() == ClubMember.MemberRole.REPRESENTATIVE) {
            throw new IllegalStateException("동아리 대표자를 강퇴할 수 없습니다. 먼저 권한을 위임해주세요.");
        }

        // 동아리 멤버 소프트 삭제 (강퇴 처리)
        targetMember.setDeletedAt(LocalDateTime.now());
        clubMemberRepository.save(targetMember);
    }

    @Transactional
    public String uploadClubPhoto(Integer clubId, MultipartFile image, Integer userId) {
        entityValidationUtil.validateClubExists(clubId);

        // 권한 확인 (동아리 회원이면 업로드 가능)
        permissionValidationUtil.validateClubMemberAccess(clubId, userId, "동아리 사진 업로드 권한이 없습니다.");

        // 이전 사진 URL 조회
        ClubPhoto clubPhoto = clubPhotoRepository
                .findByClubIdAndIsCurrentTrueAndDeletedAtIsNull(clubId)
                .orElseThrow(() -> new ResourceNotFoundException("이미지를 찾을 수 없습니다."));
        String originalUrl = clubPhoto.getImageUrl();

        // S3에서 이전 이미지 삭제 및 새로운 이미지 업로드 후 적용
        String newUrl = s3FileManagementUtil.uploadFile(image, CLUB_PHOTO_DIR, "동아리 사진 업로드 실패");
        s3FileManagementUtil.deleteFileIfNotDefault(originalUrl, DEFAULT_CLUB_PHOTO_URL);
        clubPhoto.setImageUrl(newUrl);
        clubPhoto.setUploadedAt(LocalDateTime.now());
        clubPhotoRepository.save(clubPhoto);

        return newUrl;
    }

    @Transactional
    public void deleteClubPhoto(Integer clubId, Integer userId) {
        entityValidationUtil.validateClubExists(clubId);

        // 권한 확인 (동아리 회원이면 삭제 가능)
        permissionValidationUtil.validateClubMemberAccess(clubId, userId, "동아리 사진 삭제 권한이 없습니다.");

        // 이전 사진 URL 조회
        ClubPhoto clubPhoto = clubPhotoRepository
                .findByClubIdAndIsCurrentTrueAndDeletedAtIsNull(clubId)
                .orElseThrow(() -> new ResourceNotFoundException("이미지를 찾을 수 없습니다."));
        String originalUrl = clubPhoto.getImageUrl();

        // S3에서 이전 이미지 삭제 및 기본 이미지 적용
        s3FileManagementUtil.deleteFileIfNotDefault(originalUrl, DEFAULT_CLUB_PHOTO_URL);
        clubPhoto.setImageUrl(DEFAULT_CLUB_PHOTO_URL);
        clubPhoto.setUploadedAt(LocalDateTime.now());
        clubPhotoRepository.save(clubPhoto);
    }

    private ClubDetailRespDTO convertToClubDetailRespDTO(Club club, String photoUrl, int memberCount, Integer representativeId) {
        boolean isUnionClub = (club.getUniversity() == null);

        UniversityRespDTO universityResp = null;
        if (!isUnionClub) {
            universityResp = UniversityRespDTO.builder()
                    .id(club.getUniversity().getId())
                    .name(club.getUniversity().getName())
                    .build();
        }

        return ClubDetailRespDTO.builder()
                .id(club.getId())
                .name(club.getName())
                .university(universityResp)
                .isUnionClub(isUnionClub)
                .chatroomUrl(club.getChatroomUrl())
                .description(club.getDescription())
                .instagramId(club.getInstagramId())
                .photoUrl(photoUrl)
                .memberCount(memberCount)
                .representativeId(representativeId)
                .createdAt(club.getCreatedAt())
                .updatedAt(club.getUpdatedAt())
                .build();
    }

    private ClubRespDTO convertToClubRespDTO(Club club, String photoUrl, int memberCount) {
        // 대학 정보와 연합 동아리 여부 설정
        String universityName = null;
        boolean isUnionClub = (club.getUniversity() == null);

        if (!isUnionClub) {
            universityName = club.getUniversity().getName();
        }

        return ClubRespDTO.builder()
                .id(club.getId())
                .name(club.getName())
                .universityName(universityName)
                .isUnionClub(isUnionClub)
                .photoUrl(photoUrl)
                .memberCount(memberCount)
                .build();
    }

    // 동아리 대표 사진 URL 조회 헬퍼 메서드
    private String getClubMainPhotoUrl(Integer clubId) {
        return clubPhotoRepository.findByClubIdAndIsCurrentTrueAndDeletedAtIsNull(clubId)
                .map(ClubPhoto::getImageUrl)
                .orElse(null);
    }

    // 동아리 대표자 ID 조회 헬퍼 메서드
    private Integer getClubRepresentativeId(Integer clubId) {
        return clubMemberRepository.findByClubIdAndDeletedAtIsNull(clubId).stream()
                .filter(member -> member.getRole() == ClubMember.MemberRole.REPRESENTATIVE)
                .map(member -> member.getUser().getId())
                .findFirst()
                .orElse(null);
    }

    // ClubMember를 MemberInfoDTO로 변환하는 헬퍼 메서드
    private ClubMembersRespDTO.MemberInfoDTO convertToMemberInfoDTO(ClubMember member) {
        Users user = member.getUser();
        String position = user.getPosition() != null ? user.getPosition().name() : null;

        return ClubMembersRespDTO.MemberInfoDTO.builder()
                .userId(user.getId())
                .name(user.getNickname())
                .position(position)
                .build();
    }
}
