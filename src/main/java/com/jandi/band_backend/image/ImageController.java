package com.jandi.band_backend.image;

import com.jandi.band_backend.global.dto.CommonRespDTO;
import com.jandi.band_backend.global.exception.InvalidAccessException;
import com.jandi.band_backend.security.CustomUserDetails;
import com.jandi.band_backend.user.entity.Users;
import com.jandi.band_backend.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Tag(name = "Image API")
@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {

    private final R2Service r2Service;
    private final UserRepository userRepository;

    @Operation(summary = "이미지 업로드 (관리자 전용)")
    @PostMapping("/upload")
    public ResponseEntity<CommonRespDTO<String>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("dirName") String dirName,
            @AuthenticationPrincipal CustomUserDetails userDetails) throws IOException {

        // ADMIN 권한 체크
        validateAdminPermission(userDetails.getUserId());

        String imageUrl = r2Service.uploadImage(file, dirName);
        return ResponseEntity.ok(CommonRespDTO.success("이미지 업로드 성공", imageUrl));
    }

    @Operation(summary = "이미지 삭제 (관리자 전용)")
    @DeleteMapping
    public ResponseEntity<CommonRespDTO<Void>> deleteImage(
            @RequestParam("fileUrl") String fileUrl,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        // ADMIN 권한 체크
        validateAdminPermission(userDetails.getUserId());

        r2Service.deleteImage(fileUrl);
        return ResponseEntity.ok(CommonRespDTO.success("이미지 삭제 성공"));
    }

    /**
     * ADMIN 권한 확인
     */
    private void validateAdminPermission(Integer userId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        if (user.getAdminRole() != Users.AdminRole.ADMIN) {
            throw new InvalidAccessException("관리자만 접근할 수 있습니다.");
        }
    }
}
