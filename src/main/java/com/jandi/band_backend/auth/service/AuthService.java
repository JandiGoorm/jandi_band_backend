package com.jandi.band_backend.auth.service;

import com.jandi.band_backend.auth.dto.*;
import com.jandi.band_backend.auth.dto.kakao.KakaoTokenRespDTO;
import com.jandi.band_backend.auth.dto.kakao.KakaoUserInfoDTO;
import com.jandi.band_backend.auth.service.kakao.KaKaoTokenService;
import com.jandi.band_backend.auth.service.kakao.KakaoUserService;
import com.jandi.band_backend.global.exception.InvalidAccessException;
import com.jandi.band_backend.global.exception.InvalidTokenException;
import com.jandi.band_backend.global.exception.UserNotFoundException;
import com.jandi.band_backend.security.jwt.JwtTokenProvider;
import com.jandi.band_backend.univ.entity.University;
import com.jandi.band_backend.univ.repository.UniversityRepository;
import com.jandi.band_backend.user.dto.UserInfoDTO;
import com.jandi.band_backend.user.entity.UserPhoto;
import com.jandi.band_backend.user.entity.Users;
import com.jandi.band_backend.user.repository.UserPhotoRepository;
import com.jandi.band_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final KaKaoTokenService kaKaoTokenService;
    private final KakaoUserService kakaoUserService;
    private final UserRepository userRepository;
    private final UserPhotoRepository userPhotoRepository;
    private final UniversityRepository universityRepository;
    private final JwtTokenProvider jwtTokenProvider;

    /// 로그인
    public TokenRespDTO login(String code){
        // 카카오로부터 유저 정보 얻기
        KakaoTokenRespDTO kakaoToken = kaKaoTokenService.getKakaoToken(code);
        KakaoUserInfoDTO kakaoUserInfo = kakaoUserService.getKakaoUserInfo(kakaoToken.getAccessToken());

        // DB에서 유저를 찾되, 없다면 임시 회원 가입 진행
        Users user = getOrCreateUser(kakaoUserInfo);

        // 만약 탈퇴 회원이라면 로그인 불가
        if(user.getDeletedAt() != null) { // 탈퇴 회원이라면 로그인 불가
            throw new InvalidAccessException("탈퇴 후 7일간 서비스 이용이 불가합니다.");
        }

        // 자체 jwt 토큰 발급
        return new LoginRespDTO(
            jwtTokenProvider.generateAccessToken(user.getKakaoOauthId()),
            jwtTokenProvider.generateRefreshToken(user.getKakaoOauthId()),
            user.getIsRegistered()
        );
    }

    /// 정식 회원가입
    @Transactional
    public UserInfoDTO signup(String kakaoOauthId, SignUpReqDTO reqDTO) {
        // 유저 조회
        Users user = userRepository.findByKakaoOauthId(kakaoOauthId)
                .orElseThrow(UserNotFoundException::new);

        // 회원가입 여부 확인 -> 정식 회원가입 완료한 기존 회원이라면 진행하지 않음
        if(user.getIsRegistered()) {
            throw new InvalidAccessException("이미 회원 가입이 완료된 계정입니다");
        }

        // 기본 유저 정보 입력
        University university = universityRepository.findByName(reqDTO.getUniversity());
        Users.Position position = Users.Position.valueOf(reqDTO.getPosition());

        user.setUniversity(university);
        user.setPosition(position);
        user.setIsRegistered(true);
        userRepository.save(user);

        log.info("KakaoOauthId: {}에 대해 정식 회원 가입 완료", kakaoOauthId);

        // 유저 정보 반환: 유저 기본 정보, 유저 프로필
        return new UserInfoDTO(
                user, userPhotoRepository.findByUser(user)
        );
    }

    /// 리프레시 토큰 생성
    public TokenRespDTO refresh(String refreshToken) {
        // 리프레시 토큰 검증
        if(!jwtTokenProvider.validateToken(refreshToken)) {
            throw new InvalidTokenException();
        }

        // 토큰 재발급
        String kakaoOauthId = jwtTokenProvider.getKakaoOauthId(refreshToken);
        return new TokenRespDTO(
                jwtTokenProvider.generateAccessToken(kakaoOauthId),
                jwtTokenProvider.ReissueRefreshToken(refreshToken)
        );
    }

    /// 내부 메서드
    // DB에서 유저를 찾되, 없다면 임시 회원 가입 진행. 만약 가입시 문제가 생기면 롤백
    @Transactional
    public Users getOrCreateUser(KakaoUserInfoDTO kakaoUserInfo) {
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
