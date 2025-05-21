package com.jandi.band_backend.user.service;

import com.jandi.band_backend.global.exception.UserNotFoundException;
import com.jandi.band_backend.image.S3Service;
import com.jandi.band_backend.user.entity.UserPhoto;
import com.jandi.band_backend.user.entity.Users;
import com.jandi.band_backend.user.repository.UserPhotoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserPhotoService {
    private final S3Service s3Service;
    private final UserService userService;
    private final UserPhotoRepository userPhotoRepository;

    private final String kakaoPrefix = "k.kakaocdn.net";
    private final String s3DirName = "user-photo";

    /// 유저 프로필 사진 조회
    public UserPhoto getMyPhoto(String kakaoOauthId) {
        Users user = userService.getMyInfo(kakaoOauthId);
        UserPhoto userProfile = userPhotoRepository.findByUser(user);

        if (userProfile == null) throw new UserNotFoundException();
        return userProfile;
    }

    /// 유저 프로필 사진 수정
    public void updateMyPhoto(String kakaoOauthId, MultipartFile newProfileFile) {
        // 프로필 사진이 없을 경우 수정하지 않음
        if (newProfileFile == null || newProfileFile.isEmpty()) return;

        // 프로필 조회
        UserPhoto profile = getMyPhoto(kakaoOauthId);
        String originalUrl = profile.getImageUrl();

        // 새 이미지 업로드 및 이전 이미지 삭제
        String newUrl = uploadNewProfileFile(newProfileFile);
        deleteOriginalProfileFile(originalUrl);
        profile.setImageUrl(newUrl);
        userPhotoRepository.save(profile);
    }

    /// 내부 메서드
    // S3에 새 프로필 이미지 업로드
    private String uploadNewProfileFile(MultipartFile newProfileFile) {
        try{
            return s3Service.uploadImage(newProfileFile, s3DirName);
        }catch (Exception e) {
            throw new RuntimeException("프로필 사진 업로드 실패: " + e.getMessage());
        }
    }

    // S3에서 이전 프로필 이미지 삭제
    private void deleteOriginalProfileFile(String originalProfileUrl) {
        // 카카오 기본 프로필인 경우 삭제 작업을 잔행하지 않음
        if(originalProfileUrl.contains(kakaoPrefix)) return;

        try{
            s3Service.deleteImage(originalProfileUrl);
        }catch (Exception e) {
            log.error("프로필 사진 삭제 실패: url={}", originalProfileUrl);
        }
    }
}
