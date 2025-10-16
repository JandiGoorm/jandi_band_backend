package com.jandi.band_backend.auth.service;

import com.jandi.band_backend.auth.dto.*;
import com.jandi.band_backend.auth.dto.kakao.KakaoUserInfoDTO;
import com.jandi.band_backend.auth.service.kakao.KakaoUserService;
import com.jandi.band_backend.club.entity.ClubMember;
import com.jandi.band_backend.club.repository.ClubMemberRepository;
import com.jandi.band_backend.club.repository.ClubEventRepository;
import com.jandi.band_backend.club.repository.ClubGalPhotoRepository;
import com.jandi.band_backend.global.exception.InvalidAccessException;
import com.jandi.band_backend.global.exception.InvalidTokenException;
import com.jandi.band_backend.global.exception.UniversityNotFoundException;
import com.jandi.band_backend.global.exception.UserNotFoundException;
import com.jandi.band_backend.poll.repository.PollRepository;
import com.jandi.band_backend.poll.repository.PollSongRepository;
import com.jandi.band_backend.poll.repository.VoteRepository;
import com.jandi.band_backend.promo.repository.*;
import com.jandi.band_backend.security.jwt.JwtTokenProvider;
import com.jandi.band_backend.team.repository.TeamEventRepository;
import com.jandi.band_backend.team.repository.TeamMemberRepository;
import com.jandi.band_backend.team.repository.TeamRepository;
import com.jandi.band_backend.univ.entity.University;
import com.jandi.band_backend.univ.repository.UniversityRepository;
import com.jandi.band_backend.user.dto.UserInfoDTO;
import com.jandi.band_backend.user.entity.UserPhoto;
import com.jandi.band_backend.user.entity.Users;
import com.jandi.band_backend.user.repository.UserPhotoRepository;
import com.jandi.band_backend.user.repository.UserRepository;
import com.jandi.band_backend.user.repository.UserTimetableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final UserPhotoRepository userPhotoRepository;
    private final UserTimetableRepository userTimetableRepository;
    private final UniversityRepository universityRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final KakaoUserService kakaoUserService;
    // 그룹 2
    private final ClubGalPhotoRepository clubGalPhotoRepository;
    private final ClubEventRepository clubEventRepository;
    private final PollRepository pollRepository;
    private final PollSongRepository pollSongRepository;
    private final PromoRepository promoRepository;
    private final PromoPhotoRepository promoPhotoRepository;
    private final PromoCommentRepository promoCommentRepository;
    private final TeamRepository teamRepository;
    private final TeamEventRepository teamEventRepository;
    private final PromoReportRepository promoReportRepository;
    private final PromoCommentReportRepository promoCommentReportRepository;
    // 그룹 3
    private final VoteRepository voteRepository;
    private final PromoLikeRepository promoLikeRepository;
    private final PromoCommentLikeRepository promoCommentLikeRepository;

    @Value("${user-withdraw.days}")
    private Integer userWithdrawDays;

    /// 로그인
    @Transactional
    public TokenRespDTO login(KakaoUserInfoDTO kakaoUserInfo) {
        // DB에서 유저를 찾되, 없다면 임시 회원 가입 진행
        Users user = getOrCreateUser(kakaoUserInfo);

        // 만약 탈퇴 회원이라면 로그인 불가
        if(user.getDeletedAt() != null) { // 탈퇴 회원이라면 로그인 불가
            throw new InvalidAccessException("탈퇴 후 "+ userWithdrawDays + "+일간 서비스 이용이 불가합니다.");
        }

        // 자체 jwt 토큰 발급
        return new LoginRespDTO(
            jwtTokenProvider.generateAccessToken(user.getKakaoOauthId()),
            jwtTokenProvider.generateRefreshToken(user.getKakaoOauthId()),
            user.getIsRegistered()
        );
    }

    /// 로그아웃
    public void logout(Integer userId) {
        // 멘토링 결과 별도의 블랙리스트 처리는 필요하지 않아 로깅만 하는 것으로 작업
        // 카카오 토큰은 카카오 리소스 접근용이라 알림톡/메시지 공유에선 쓰이지 않아 굳이 카카오에게 로그아웃을 요청해 강제 만료처리할 필요가 없음
        Users user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        log.info("KakaoOauthId: {}가 로그아웃 요청", user.getKakaoOauthId());
    }

    /// 정식 회원가입
    @Transactional
    public UserInfoDTO signup(Integer userId, SignUpReqDTO reqDTO) {
        // 유저 조회
        Users user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        // 회원가입 여부 확인 -> 정식 회원가입 완료한 기존 회원이라면 진행하지 않음
        if(user.getIsRegistered()) {
            throw new InvalidAccessException("이미 회원 가입이 완료된 계정입니다");
        }

        // 기본 유저 정보 입력
        Users.Position position = Users.Position.valueOf(reqDTO.getPosition());
        University university = universityRepository.findByName(reqDTO.getUniversity());
        if(university == null) {
            throw new UniversityNotFoundException(("존재하지 않는 대학입니다: " + reqDTO.getUniversity()));
        }

        user.setUniversity(university);
        user.setPosition(position);
        user.setIsRegistered(true);
        userRepository.save(user);

        log.info("KakaoOauthId: {}에 대해 정식 회원 가입 완료", user.getKakaoOauthId());

        // 유저 정보 반환: 유저 기본 정보, 유저 프로필
        return new UserInfoDTO(
                user, userPhotoRepository.findByUser(user)
        );
    }

    /// 회원탈퇴
    @Transactional
    public void cancel(Integer userId) {
        // 만약 동아리장이라면 탈퇴 불가
        Users user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        List<String> myRepresentativeClub
                = clubMemberRepository.findClubNamesByUserRole(userId, ClubMember.MemberRole.REPRESENTATIVE);
        if(!myRepresentativeClub.isEmpty()) {
            String clubNames = String.join(", ", myRepresentativeClub);
            throw new RuntimeException("탈퇴할 수 없습니다: 해당 동아리 대표직을 타인에게 위임 후 재시도해주세요: " + clubNames);
        }

        LocalDateTime deletedAt = LocalDateTime.now();

        log.info("회원 탈퇴 처리 시작 - 사용자 ID: {}", userId);
        processGroup1SoftDelete(userId, deletedAt);
        processGroup2Anonymize(userId);
        processGroup3HardDelete(userId);

        user.setIsRegistered(false);
        user.setDeletedAt(deletedAt);
        userRepository.save(user);

        kakaoUserService.unlink(user.getKakaoOauthId());

        log.info("회원 탈퇴 처리 완료 - 사용자 ID: {}", userId);
    }

    // 그룹 1
    private void processGroup1SoftDelete(Integer userId, LocalDateTime deletedAt) {
        int userPhotoCount = userPhotoRepository.softDeleteByUserId(userId, deletedAt);
        int userTimetableCount = userTimetableRepository.softDeleteByUserId(userId, deletedAt);
        int clubMemberCount = clubMemberRepository.softDeleteByUserId(userId, deletedAt);
        int teamMemberCount = teamMemberRepository.softDeleteByUserId(userId, deletedAt);

        log.info("그룹 1 소프트 삭제 완료 - 사용자프로필: {}, 시간표: {}, 동아리멤버: {}, 팀멤버: {}",
                userPhotoCount, userTimetableCount, clubMemberCount, teamMemberCount);
    }

    // 그룹 2
    private void processGroup2Anonymize(Integer userId) {
        int clubGalPhotoCount = clubGalPhotoRepository.anonymizeByUserId(userId);
        int clubEventCount = clubEventRepository.anonymizeByUserId(userId);
        int pollCount = pollRepository.anonymizeByCreatorId(userId);
        int pollSongCount = pollSongRepository.anonymizeBySuggesterId(userId);
        int promoCount = promoRepository.anonymizeByCreatorId(userId);
        int promoPhotoCount = promoPhotoRepository.anonymizeByUserId(userId);
        int promoCommentCount = promoCommentRepository.anonymizeByUserId(userId);
        int teamCount = teamRepository.anonymizeByCreatorId(userId);
        int teamEventCount = teamEventRepository.anonymizeByUserId(userId);
        int promoReportCount = promoReportRepository.anonymizeByReporterId(userId);
        int promoCommentReportCount = promoCommentReportRepository.anonymizeByReporterId(userId);

        log.info("그룹 2 익명화 완료 - 갤러리: {}, 동아리이벤트: {}, 투표: {}, 투표곡: {}, 홍보글: {}, 홍보사진: {}, 홍보댓글: {}, 팀: {}, 팀이벤트: {}, 홍보신고: {}, 댓글신고: {}",
                clubGalPhotoCount, clubEventCount, pollCount, pollSongCount, promoCount, promoPhotoCount,
                promoCommentCount, teamCount, teamEventCount, promoReportCount, promoCommentReportCount);
    }

    // 그룹 3
    private void processGroup3HardDelete(Integer userId) {
        int voteCount = voteRepository.deleteByUserId(userId);

        List<Integer> promoIds = promoLikeRepository.findPromoIdsByUserId(userId);
        for (Integer promoId : promoIds) {
            promoRepository.decrementLikeCount(promoId);
        }
        int promoLikeCount = promoLikeRepository.deleteByUserId(userId);

        int promoCommentLikeCount = promoCommentLikeRepository.deleteByUserId(userId);

        log.info("그룹 3 하드 삭제 완료 - 투표: {}, 홍보좋아요: {} ({}개 홍보글 카운트 조정), 댓글좋아요: {}",
                voteCount, promoLikeCount, promoIds.size(), promoCommentLikeCount);
    }

    /// 리프레시 토큰 생성
    public TokenRespDTO refresh(String refreshToken) {
        // 리프레시 토큰 검증
        if(!jwtTokenProvider.validateToken(refreshToken)) {
            throw new InvalidTokenException();
        }

        // 유저 검증
        String kakaoOauthId = jwtTokenProvider.getKakaoOauthId(refreshToken);
        userRepository.findByKakaoOauthIdAndDeletedAtIsNull(kakaoOauthId)
                .orElseThrow(UserNotFoundException::new);

        // 토큰 재발급
        return new TokenRespDTO(
                jwtTokenProvider.generateAccessToken(kakaoOauthId),
                jwtTokenProvider.ReissueRefreshToken(refreshToken)
        );
    }

    /// 내부 메서드
    // DB에서 유저를 찾되, 없다면 임시 회원 가입 진행. 만약 가입시 문제가 생기면 롤백
    private Users getOrCreateUser(KakaoUserInfoDTO kakaoUserInfo) {
        String kakaoOauthId = kakaoUserInfo.getKakaoOauthId();
        return userRepository.findByKakaoOauthId(kakaoOauthId)
                .orElseGet(() -> createTemporaryUser(kakaoUserInfo));
    }

    // 임시 회원 가입
    private Users createTemporaryUser(KakaoUserInfoDTO kakaoUserInfo){
        // 유저 생성
        Users newUser = new Users();
        newUser.setKakaoOauthId(kakaoUserInfo.getKakaoOauthId());
        newUser.setNickname(kakaoUserInfo.getNickname());
        userRepository.save(newUser);

        // 유저 프로필 사진 생성
        UserPhoto profile = new UserPhoto();
        profile.setUser(newUser);
        profile.setImageUrl(kakaoUserInfo.getProfilePhoto());
        userPhotoRepository.save(profile);

        return newUser;
    }
}
