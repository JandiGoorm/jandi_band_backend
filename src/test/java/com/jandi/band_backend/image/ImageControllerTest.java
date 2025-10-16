package com.jandi.band_backend.image;

import com.jandi.band_backend.global.dto.CommonRespDTO;
import com.jandi.band_backend.security.CustomUserDetails;
import com.jandi.band_backend.user.entity.Users;
import com.jandi.band_backend.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ImageController 단위 테스트")
class ImageControllerTest {

    @Mock
    private S3Service s3Service;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ImageController imageController;

    @Test
    @DisplayName("관리자 사용자는 이미지를 업로드할 수 있다")
    void uploadImage_AdminUser_Succeeds() throws IOException {
        Users adminUser = createUser(1, Users.AdminRole.ADMIN);
        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(s3Service.uploadImage(any(), eq("test"))).thenReturn("https://cdn.example.com/test/image.jpg");

        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                "sample.jpg",
                "image/jpeg",
                new byte[]{1, 2, 3}
        );

        ResponseEntity<CommonRespDTO<String>> response = imageController.uploadImage(
                multipartFile,
                "test",
                new CustomUserDetails(adminUser)
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData()).isEqualTo("https://cdn.example.com/test/image.jpg");
        verify(s3Service).uploadImage(multipartFile, "test");
    }

    @Test
    @DisplayName("관리자가 아니면 이미지 업로드 시 예외가 발생한다")
    void uploadImage_NonAdminUser_Throws() {
        Users normalUser = createUser(2, Users.AdminRole.USER);
        when(userRepository.findById(normalUser.getId())).thenReturn(Optional.of(normalUser));

        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                "sample.jpg",
                "image/jpeg",
                new byte[]{1, 2, 3}
        );

        assertThatThrownBy(() -> imageController.uploadImage(
                multipartFile,
                "test",
                new CustomUserDetails(normalUser)
        )).isInstanceOf(RuntimeException.class)
          .hasMessageContaining("관리자만");
    }

    @Test
    @DisplayName("관리자 사용자는 이미지를 삭제할 수 있다")
    void deleteImage_AdminUser_Succeeds() {
        Users adminUser = createUser(3, Users.AdminRole.ADMIN);
        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));

        ResponseEntity<CommonRespDTO<Void>> response = imageController.deleteImage(
                "folder/image.jpg",
                new CustomUserDetails(adminUser)
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData()).isNull();
        verify(s3Service).deleteImage("folder/image.jpg");
    }

    @Test
    @DisplayName("관리자가 아니면 이미지 삭제 시 예외가 발생한다")
    void deleteImage_NonAdminUser_Throws() {
        Users normalUser = createUser(4, Users.AdminRole.USER);
        when(userRepository.findById(normalUser.getId())).thenReturn(Optional.of(normalUser));

        assertThatThrownBy(() -> imageController.deleteImage(
                "folder/image.jpg",
                new CustomUserDetails(normalUser)
        )).isInstanceOf(RuntimeException.class)
          .hasMessageContaining("관리자만");
    }

    private Users createUser(Integer id, Users.AdminRole adminRole) {
        Users user = new Users();
        user.setId(id);
        user.setKakaoOauthId("oauth-" + id);
        user.setNickname("user" + id);
        user.setAdminRole(adminRole);
        user.setIsRegistered(true);
        return user;
    }
}
