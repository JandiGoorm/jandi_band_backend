package com.jandi.band_backend.club.service;

import com.jandi.band_backend.club.dto.ClubReqDTO;
import com.jandi.band_backend.club.dto.ClubRespDTO;
import com.jandi.band_backend.club.dto.ClubSimpleRespDTO;
import com.jandi.band_backend.club.dto.ClubUpdateReqDTO;
import com.jandi.band_backend.club.dto.PageRespDTO;
import com.jandi.band_backend.univ.dto.UniversityRespDTO;
import com.jandi.band_backend.club.entity.Club;
import com.jandi.band_backend.club.entity.ClubMember;
import com.jandi.band_backend.club.entity.ClubPhoto;
import com.jandi.band_backend.user.entity.Users;
import com.jandi.band_backend.club.repository.ClubMemberRepository;
import com.jandi.band_backend.club.repository.ClubPhotoRepository;
import com.jandi.band_backend.club.repository.ClubRepository;
import com.jandi.band_backend.user.repository.UserRepository;
import com.jandi.band_backend.univ.entity.University;
import com.jandi.band_backend.univ.repository.UniversityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClubService {

    private static final ZoneId KST_ZONE_ID = ZoneId.of("Asia/Seoul");

    private final ClubRepository clubRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final ClubPhotoRepository clubPhotoRepository;
    private final UserRepository userRepository;
    private final UniversityRepository universityRepository;

    @Transactional
    public ClubRespDTO createClub(ClubReqDTO request, Integer userId) {
        // 사용자 확인
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 동아리 생성
        Club club = new Club();
        club.setName(request.getName());
        club.setChatroomUrl(request.getChatroomUrl());
        club.setDescription(request.getDescription());
        club.setInstagramId(request.getInstagramId());
        club.setCreatedAt(Instant.now());
        club.setUpdatedAt(Instant.now());

        // 대학 정보 설정 (연합 동아리인 경우 null)
        if (request.getUniversityId() != null) {
            University university = universityRepository.findById(request.getUniversityId())
                    .orElseThrow(() -> new IllegalArgumentException("대학을 찾을 수 없습니다. ID: " + request.getUniversityId()));
            club.setUniversity(university);
        }

        Club savedClub = clubRepository.save(club);

        // 동아리 대표 사진 저장 (제공된 경우)
        if (request.getPhotoUrl() != null && !request.getPhotoUrl().isEmpty()) {
            saveClubPhoto(savedClub, request.getPhotoUrl());
        }

        // 동아리 멤버 추가 (생성자를 대표자로 설정)
        ClubMember clubMember = new ClubMember();
        clubMember.setClub(savedClub);
        clubMember.setUser(user);
        clubMember.setRole(ClubMember.MemberRole.REPRESENTATIVE);
        clubMember.setJoinedAt(Instant.now());
        clubMember.setUpdatedAt(Instant.now());

        clubMemberRepository.save(clubMember);

        return buildClubResponse(savedClub);
    }

    @Transactional(readOnly = true)
    public PageRespDTO<ClubSimpleRespDTO> getClubList(Pageable pageable) {
        Page<Club> clubPage = clubRepository.findAll(pageable);

        List<ClubSimpleRespDTO> content = clubPage.getContent().stream()
                .map(this::buildClubSimpleResponse)
                .collect(Collectors.toList());

        return PageRespDTO.<ClubSimpleRespDTO>builder()
                .content(content)
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .totalElements(Math.toIntExact(clubPage.getTotalElements()))
                .totalPages(clubPage.getTotalPages())
                .last(clubPage.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public ClubRespDTO getClubDetail(Integer clubId) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new IllegalArgumentException("동아리를 찾을 수 없습니다."));

        int memberCount = clubMemberRepository.countByClubId(clubId);

        return buildClubResponse(club, memberCount);
    }

    @Transactional
    public ClubRespDTO updateClub(Integer clubId, ClubUpdateReqDTO request, Integer userId) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new IllegalArgumentException("동아리를 찾을 수 없습니다."));

        // 권한 확인 (대표자만 수정 가능)
        clubMemberRepository.findByClubIdAndUserId(clubId, userId)
                .filter(member -> member.getRole() == ClubMember.MemberRole.REPRESENTATIVE)
                .orElseThrow(() -> new IllegalArgumentException("동아리 정보 수정 권한이 없습니다."));

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

        // 대학 정보 업데이트
        if (request.getUniversityId() != null) {
            University university = universityRepository.findById(request.getUniversityId())
                    .orElseThrow(() -> new IllegalArgumentException("대학을 찾을 수 없습니다. ID: " + request.getUniversityId()));
            club.setUniversity(university);
        } else {
            // universityId가 null로 들어온 경우 연합 동아리로 설정
            club.setUniversity(null);
        }

        club.setUpdatedAt(Instant.now());

        // 동아리 대표 사진 업데이트 (제공된 경우)
        if (request.getPhotoUrl() != null && !request.getPhotoUrl().isEmpty()) {
            // 기존 사진이 있으면 소프트 삭제 처리
            clubPhotoRepository.findByClubIdAndIsCurrentTrueAndDeletedAtIsNull(clubId)
                    .ifPresent(photo -> {
                        photo.setIsCurrent(false);
                        photo.setDeletedAt(Instant.now());
                        clubPhotoRepository.save(photo);
                    });

            // 새 사진 저장
            saveClubPhoto(club, request.getPhotoUrl());
        }

        Club updatedClub = clubRepository.save(club);
        int memberCount = clubMemberRepository.countByClubId(clubId);

        return buildClubResponse(updatedClub, memberCount);
    }

    @Transactional
    public void deleteClub(Integer clubId, Integer userId) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new IllegalArgumentException("동아리를 찾을 수 없습니다."));

        // 권한 확인 (대표자만 삭제 가능)
        clubMemberRepository.findByClubIdAndUserId(clubId, userId)
                .filter(member -> member.getRole() == ClubMember.MemberRole.REPRESENTATIVE)
                .orElseThrow(() -> new IllegalArgumentException("동아리 삭제 권한이 없습니다."));

        // 동아리 대표 사진 소프트 삭제
        clubPhotoRepository.findByClubIdAndIsCurrentTrueAndDeletedAtIsNull(clubId)
                .ifPresent(photo -> {
                    photo.setIsCurrent(false);
                    photo.setDeletedAt(Instant.now());
                    clubPhotoRepository.save(photo);
                });

        // 동아리 삭제 (소프트 딜리트)
        club.setDeletedAt(Instant.now());
        clubRepository.save(club);
    }

    // 동아리 응답 객체 생성 헬퍼 메서드
    private ClubRespDTO buildClubResponse(Club club) {
        // 회원 수 별도 조회
        int memberCount = clubMemberRepository.countByClubId(club.getId());
        return buildClubResponse(club, memberCount);
    }

    // 동아리 응답 객체 생성 헬퍼 메서드 (회원 수 제공)
    private ClubRespDTO buildClubResponse(Club club, int memberCount) {
        // 동아리 대표 사진 URL 조회
        String photoUrl = clubPhotoRepository.findByClubIdAndIsCurrentTrueAndDeletedAtIsNull(club.getId())
                .map(ClubPhoto::getImageUrl)
                .orElse(null);

        // Instant를 LocalDateTime으로 변환 (KST 적용)
        LocalDateTime createdAt = null;
        LocalDateTime updatedAt = null;
        if (club.getCreatedAt() != null) {
            createdAt = LocalDateTime.ofInstant(club.getCreatedAt(), KST_ZONE_ID);
        }
        if (club.getUpdatedAt() != null) {
            updatedAt = LocalDateTime.ofInstant(club.getUpdatedAt(), KST_ZONE_ID);
        }

        // 대학 정보와 연합 동아리 여부 설정
        UniversityRespDTO.SimpleResponse universityResp = null;
        boolean isUnionClub = (club.getUniversity() == null);

        if (!isUnionClub) {
            universityResp = UniversityRespDTO.SimpleResponse.builder()
                    .id(club.getUniversity().getId())
                    .name(club.getUniversity().getName())
                    .build();
        }

        return ClubRespDTO.builder()
                .id(club.getId())
                .name(club.getName())
                .university(universityResp)
                .isUnionClub(isUnionClub)
                .chatroomUrl(club.getChatroomUrl())
                .description(club.getDescription())
                .instagramId(club.getInstagramId())
                .photoUrl(photoUrl)
                .memberCount(memberCount)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }

    // 동아리 간단 응답 객체 생성 헬퍼 메서드
    private ClubSimpleRespDTO buildClubSimpleResponse(Club club) {
        // 동아리 대표 사진 URL 조회
        String photoUrl = clubPhotoRepository.findByClubIdAndIsCurrentTrueAndDeletedAtIsNull(club.getId())
                .map(ClubPhoto::getImageUrl)
                .orElse(null);

        // 대학 정보와 연합 동아리 여부 설정
        String universityName = null;
        boolean isUnionClub = (club.getUniversity() == null);

        if (!isUnionClub) {
            universityName = club.getUniversity().getName();
        }

        return ClubSimpleRespDTO.builder()
                .id(club.getId())
                .name(club.getName())
                .universityName(universityName)
                .isUnionClub(isUnionClub)
                .photoUrl(photoUrl)
                .memberCount(club.getClubMembers().size())
                .build();
    }

    // 동아리 대표 사진 저장 헬퍼 메서드
    private void saveClubPhoto(Club club, String imageUrl) {
        ClubPhoto photo = new ClubPhoto();
        photo.setClub(club);
        photo.setImageUrl(imageUrl);
        photo.setIsCurrent(true);
        clubPhotoRepository.save(photo);
    }
}
