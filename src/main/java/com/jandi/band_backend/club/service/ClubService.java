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

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

// TODO: 커스텀 예외 처리를 위한 GlobalExceptionHandler 클래스 구현
// 단순히 각 메서드마다 ResponseStatusException을 사용하는 대신,
// GlobalExceptionHandler 클래스를 생성하여 각종 예외를 중앙에서 처리하는 것도 좋은 방법입니다.
// 이 접근 방식을 사용하면 서비스 클래스는 비즈니스 로직에 집중하고,
// 예외 처리와 HTTP 응답 코드 설정은 별도의 핸들러가 담당하게 됩니다.

@Service
@RequiredArgsConstructor
public class ClubService {

    private final ClubRepository clubRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final ClubPhotoRepository clubPhotoRepository;
    private final UserRepository userRepository;
    private final UniversityRepository universityRepository;

    @Transactional
    public ClubRespDTO createClub(ClubReqDTO request, Integer userId) {
        // 사용자 확인
        Users user = userRepository.findById(userId)
                // TODO: 사용자를 찾지 못한 경우 404 Not Found 반환
                // .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
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
                    // TODO: 대학을 찾지 못한 경우 404 Not Found 반환
                    .orElseThrow(() -> new IllegalArgumentException("대학을 찾을 수 없습니다. ID: " + request.getUniversityId()));
            club.setUniversity(university);
        }

        Club savedClub = clubRepository.save(club);

        // 동아리 대표 사진 URL (제공된 경우에만)
        String photoUrl = (request.getPhotoUrl() != null && !request.getPhotoUrl().isEmpty())
                ? request.getPhotoUrl()
                : null;

        // 동아리 대표 사진 저장 (제공된 경우)
        if (request.getPhotoUrl() != null && !request.getPhotoUrl().isEmpty()) {
            saveClubPhoto(savedClub, photoUrl);
        }

        // 동아리 멤버 추가 (생성자를 대표자로 설정)
        ClubMember clubMember = new ClubMember();
        clubMember.setClub(savedClub);
        clubMember.setUser(user);
        clubMember.setRole(ClubMember.MemberRole.REPRESENTATIVE);
        clubMember.setJoinedAt(Instant.now());
        clubMember.setUpdatedAt(Instant.now());

        clubMemberRepository.save(clubMember);

        return ClubRespDTO.fromEntity(savedClub, photoUrl, 1);
    }

    @Transactional(readOnly = true)
    public PageRespDTO<ClubSimpleRespDTO> getClubList(Pageable pageable) {
        // deletedAt이 null인 동아리만 조회하도록 수정
        Page<Club> clubPage = clubRepository.findAllByDeletedAtIsNull(pageable);

        List<ClubSimpleRespDTO> content = clubPage.getContent().stream()
                .map(club -> {
                    // 동아리 대표 사진 URL 조회
                    String photoUrl = getClubMainPhotoUrl(club.getId());

                    int memberCount = club.getClubMembers().size();

                    return ClubSimpleRespDTO.fromEntity(club, photoUrl, memberCount);
                })
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
        Club club = clubRepository.findByIdAndDeletedAtIsNull(clubId)
                // TODO: 403 대신 404 응답을 반환
                // import org.springframework.web.server.ResponseStatusException;
                // import org.springframework.http.HttpStatus;
                // .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "동아리를 찾을 수 없습니다."));
                .orElseThrow(() -> new IllegalArgumentException("동아리를 찾을 수 없습니다."));

        int memberCount = clubMemberRepository.countByClubId(clubId);

        // 동아리 대표 사진 URL 조회
        String photoUrl = getClubMainPhotoUrl(club.getId());

        return ClubRespDTO.fromEntity(club, photoUrl, memberCount);
    }

    @Transactional
    public ClubRespDTO updateClub(Integer clubId, ClubUpdateReqDTO request, Integer userId) {
        Club club = clubRepository.findByIdAndDeletedAtIsNull(clubId)
                // TODO: 403 대신 404 응답을 반환
                .orElseThrow(() -> new IllegalArgumentException("동아리를 찾을 수 없습니다."));

        // 권한 확인 (대표자만 수정 가능)
        clubMemberRepository.findByClubIdAndUserId(clubId, userId)
                .filter(member -> member.getRole() == ClubMember.MemberRole.REPRESENTATIVE)
                // TODO: 권한 오류는 403 Forbidden이 적절합니다
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
                    // TODO: 대학을 찾지 못한 경우도 404로 처리
                    .orElseThrow(() -> new IllegalArgumentException("대학을 찾을 수 없습니다. ID: " + request.getUniversityId()));
            club.setUniversity(university);
        } else {
            // universityId가 null로 들어온 경우 연합 동아리로 설정
            club.setUniversity(null);
        }

        club.setUpdatedAt(Instant.now());

        // 동아리 대표 사진 URL (제공된 경우 사용, 없으면 기존 URL 조회)
        String photoUrl = (request.getPhotoUrl() != null && !request.getPhotoUrl().isEmpty())
                ? request.getPhotoUrl()
                : getClubMainPhotoUrl(clubId);

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
            saveClubPhoto(club, photoUrl);
        }

        Club updatedClub = clubRepository.save(club);
        int memberCount = clubMemberRepository.countByClubId(clubId);

        return ClubRespDTO.fromEntity(updatedClub, photoUrl, memberCount);
    }

    @Transactional
    public void deleteClub(Integer clubId, Integer userId) {
        Club club = clubRepository.findByIdAndDeletedAtIsNull(clubId)
                // TODO: 403 대신 404 응답을 반환하려면 다음과 같이 수정
                .orElseThrow(() -> new IllegalArgumentException("동아리를 찾을 수 없습니다."));

        // 권한 확인 (대표자만 삭제 가능)
        clubMemberRepository.findByClubIdAndUserId(clubId, userId)
                .filter(member -> member.getRole() == ClubMember.MemberRole.REPRESENTATIVE)
                // TODO: 권한 오류는 403 Forbidden이 적절합니다
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
