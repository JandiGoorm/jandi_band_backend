package com.jandi.band_backend.club.service;

import com.jandi.band_backend.club.dto.ClubReqDTO;
import com.jandi.band_backend.club.dto.ClubDetailRespDTO;
import com.jandi.band_backend.club.dto.ClubRespDTO;
import com.jandi.band_backend.club.dto.ClubUpdateReqDTO;
import com.jandi.band_backend.club.entity.Club;
import com.jandi.band_backend.club.entity.ClubMember;
import com.jandi.band_backend.club.entity.ClubPhoto;
import com.jandi.band_backend.club.repository.ClubMemberRepository;
import com.jandi.band_backend.club.repository.ClubPhotoRepository;
import com.jandi.band_backend.club.repository.ClubRepository;
import com.jandi.band_backend.univ.dto.UniversityRespDTO;
import com.jandi.band_backend.univ.entity.University;
import com.jandi.band_backend.univ.repository.UniversityRepository;
import com.jandi.band_backend.user.entity.Users;
import com.jandi.band_backend.user.repository.UserRepository;
import com.jandi.band_backend.global.exception.ClubNotFoundException;
import com.jandi.band_backend.global.exception.UnauthorizedClubAccessException;
import com.jandi.band_backend.global.exception.UniversityNotFoundException;
import com.jandi.band_backend.global.exception.UserNotFoundException;
import com.jandi.band_backend.image.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ClubService {

    private final ClubRepository clubRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final ClubPhotoRepository clubPhotoRepository;
    private final UserRepository userRepository;
    private final UniversityRepository universityRepository;
    private final S3Service s3Service;

    private static final String CLUB_PHOTO_DIR = "club-photo";

    @Transactional
    public ClubDetailRespDTO createClub(ClubReqDTO request, Integer userId) {
        // 사용자 확인
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException());

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
            University university = universityRepository.findById(request.getUniversityId())
                    .orElseThrow(() -> new UniversityNotFoundException("대학을 찾을 수 없습니다. ID: " + request.getUniversityId()));
            club.setUniversity(university);
        }

        Club savedClub = clubRepository.save(club);

        // 동아리 멤버 추가 (생성자를 대표자로 설정)
        ClubMember clubMember = new ClubMember();
        clubMember.setClub(savedClub);
        clubMember.setUser(user);
        clubMember.setRole(ClubMember.MemberRole.REPRESENTATIVE);
        clubMember.setJoinedAt(LocalDateTime.now());
        clubMember.setUpdatedAt(LocalDateTime.now());

        clubMemberRepository.save(clubMember);

        return convertToClubDetailRespDTO(savedClub, null, 1);
    }

    @Transactional(readOnly = true)
    public Page<ClubRespDTO> getClubList(Pageable pageable) {
        // deletedAt이 null인 동아리만 조회하도록 수정
        Page<Club> clubPage = clubRepository.findAllByDeletedAtIsNull(pageable);

        return clubPage.map(club -> {
            // 동아리 대표 사진 URL 조회
            String photoUrl = getClubMainPhotoUrl(club.getId());

            int memberCount = club.getClubMembers().size();

            return convertToClubRespDTO(club, photoUrl, memberCount);
        });
    }

    @Transactional(readOnly = true)
    public ClubDetailRespDTO getClubDetail(Integer clubId) {
        Club club = clubRepository.findByIdAndDeletedAtIsNull(clubId)
                .orElseThrow(() -> new ClubNotFoundException("동아리를 찾을 수 없습니다."));

        int memberCount = clubMemberRepository.countByClubId(clubId);

        // 동아리 대표 사진 URL 조회
        String photoUrl = getClubMainPhotoUrl(club.getId());

        return convertToClubDetailRespDTO(club, photoUrl, memberCount);
    }

    @Transactional
    public ClubDetailRespDTO updateClub(Integer clubId, ClubUpdateReqDTO request, Integer userId) {
        Club club = clubRepository.findByIdAndDeletedAtIsNull(clubId)
                .orElseThrow(() -> new ClubNotFoundException("동아리를 찾을 수 없습니다."));

        // 권한 확인 (대표자만 수정 가능)
        clubMemberRepository.findByClubIdAndUserId(clubId, userId)
                .filter(member -> member.getRole() == ClubMember.MemberRole.REPRESENTATIVE)
                .orElseThrow(() -> new UnauthorizedClubAccessException("동아리 정보 수정 권한이 없습니다."));

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
        int memberCount = clubMemberRepository.countByClubId(clubId);

        return convertToClubDetailRespDTO(updatedClub, photoUrl, memberCount);
    }

    @Transactional
    public void deleteClub(Integer clubId, Integer userId) {
        Club club = clubRepository.findByIdAndDeletedAtIsNull(clubId)
                .orElseThrow(() -> new ClubNotFoundException("동아리를 찾을 수 없습니다."));

        // 권한 확인 (대표자만 삭제 가능)
        clubMemberRepository.findByClubIdAndUserId(clubId, userId)
                .filter(member -> member.getRole() == ClubMember.MemberRole.REPRESENTATIVE)
                .orElseThrow(() -> new UnauthorizedClubAccessException("동아리 삭제 권한이 없습니다."));

        // 동아리 대표 사진 소프트 삭제
        clubPhotoRepository.findByClubIdAndIsCurrentTrueAndDeletedAtIsNull(clubId)
                .ifPresent(photo -> {
                    photo.setIsCurrent(false);
                    photo.setDeletedAt(LocalDateTime.now());
                    clubPhotoRepository.save(photo);
                });

        // 동아리 삭제 (소프트 딜리트)
        club.setDeletedAt(LocalDateTime.now());
        clubRepository.save(club);
    }

    /**
     * TODO: 동아리 대표 사진 업로드 기능 적용
     */
    @Transactional
    public String uploadClubPhoto(Integer clubId, MultipartFile image, Integer userId) throws IOException {
        Club club = clubRepository.findByIdAndDeletedAtIsNull(clubId)
                .orElseThrow(() -> new ClubNotFoundException("동아리를 찾을 수 없습니다."));

        // 권한 확인 (대표자만 업로드 가능)
        clubMemberRepository.findByClubIdAndUserId(clubId, userId)
                .filter(member -> member.getRole() == ClubMember.MemberRole.REPRESENTATIVE)
                .orElseThrow(() -> new UnauthorizedClubAccessException("동아리 사진 업로드 권한이 없습니다."));

        // S3에 이미지 업로드
        String imageUrl = s3Service.uploadImage(image, CLUB_PHOTO_DIR);

        // 기존 사진이 있으면 소프트 삭제 처리
        clubPhotoRepository.findByClubIdAndIsCurrentTrueAndDeletedAtIsNull(clubId)
                .ifPresent(photo -> {
                    photo.setIsCurrent(false);
                    photo.setDeletedAt(LocalDateTime.now());
                    clubPhotoRepository.save(photo);
                });

        // 새 사진 저장
        saveClubPhoto(club, imageUrl);

        return imageUrl;
    }

    /**
     * TODO: 동아리 대표 사진 삭제 기능 적용
     */
    @Transactional
    public void deleteClubPhoto(Integer clubId, Integer userId) {
        Club club = clubRepository.findByIdAndDeletedAtIsNull(clubId)
                .orElseThrow(() -> new ClubNotFoundException("동아리를 찾을 수 없습니다."));

        // 권한 확인 (대표자만 삭제 가능)
        clubMemberRepository.findByClubIdAndUserId(clubId, userId)
                .filter(member -> member.getRole() == ClubMember.MemberRole.REPRESENTATIVE)
                .orElseThrow(() -> new UnauthorizedClubAccessException("동아리 사진 삭제 권한이 없습니다."));

        clubPhotoRepository.findByClubIdAndIsCurrentTrueAndDeletedAtIsNull(clubId)
                .ifPresent(photo -> {
                    // S3에서 이미지 삭제
                    s3Service.deleteImage(photo.getImageUrl());

                    // DB에서 삭제 처리
                    photo.setIsCurrent(false);
                    photo.setDeletedAt(LocalDateTime.now());
                    clubPhotoRepository.save(photo);
                });
    }

    private ClubDetailRespDTO convertToClubDetailRespDTO(Club club, String photoUrl, int memberCount) {
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

    // 동아리 대표 사진 저장 헬퍼 메서드
    private void saveClubPhoto(Club club, String imageUrl) {
        ClubPhoto photo = new ClubPhoto();
        photo.setClub(club);
        photo.setImageUrl(imageUrl);
        photo.setIsCurrent(true);
        clubPhotoRepository.save(photo);
    }
}
