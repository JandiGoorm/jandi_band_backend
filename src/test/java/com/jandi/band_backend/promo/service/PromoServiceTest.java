package com.jandi.band_backend.promo.service;

import com.jandi.band_backend.global.exception.ResourceNotFoundException;
import com.jandi.band_backend.global.util.PermissionValidationUtil;
import com.jandi.band_backend.global.util.S3FileManagementUtil;
import com.jandi.band_backend.global.util.UserValidationUtil;
import com.jandi.band_backend.promo.dto.PromoReqDTO;
import com.jandi.band_backend.promo.dto.PromoRespDTO;
import com.jandi.band_backend.promo.dto.PromoSimpleRespDTO;
import com.jandi.band_backend.promo.entity.Promo;
import com.jandi.band_backend.promo.repository.PromoPhotoRepository;
import com.jandi.band_backend.promo.repository.PromoRepository;
import com.jandi.band_backend.user.entity.Users;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PromoService 공연 홍보 관리 테스트")
class PromoServiceTest {

    @Mock
    private PromoRepository promoRepository;

    @Mock
    private PromoPhotoRepository promoPhotoRepository;

    @Mock
    private PromoLikeService promoLikeService;

    @Mock
    private PermissionValidationUtil permissionValidationUtil;

    @Mock
    private UserValidationUtil userValidationUtil;

    @Mock
    private S3FileManagementUtil s3FileManagementUtil;

    @InjectMocks
    private PromoService promoService;

    @Test
    @DisplayName("공연 홍보 목록 조회 성공")
    void getPromos_Success() {
        // Given
        Pageable pageable = PageRequest.of(0, 20);
        Promo mockPromo = createMockPromo();
        Page<Promo> promoPage = new PageImpl<>(Arrays.asList(mockPromo));

        when(promoRepository.findAllSortedByEventDatetime(pageable)).thenReturn(promoPage);

        // When
        Page<PromoRespDTO> result = promoService.getPromos(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("테스트 공연");
        assertThat(result.getContent().get(0).getTeamName()).isEqualTo("테스트 팀");

        verify(promoRepository).findAllSortedByEventDatetime(pageable);
    }

    @Test
    @DisplayName("공연 홍보 목록 조회 성공 - 사용자별 좋아요 상태 포함")
    void getPromos_WithUserLike_Success() {
        // Given
        Integer userId = 1;
        Pageable pageable = PageRequest.of(0, 20);
        Promo mockPromo = createMockPromo();
        Page<Promo> promoPage = new PageImpl<>(Arrays.asList(mockPromo));

        when(promoRepository.findAllSortedByEventDatetime(pageable)).thenReturn(promoPage);
        when(promoLikeService.isLikedByUser(anyInt(), anyInt())).thenReturn(true);

        // When
        Page<PromoRespDTO> result = promoService.getPromos(userId, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("테스트 공연");

        verify(promoRepository).findAllSortedByEventDatetime(pageable);
        verify(promoLikeService).isLikedByUser(mockPromo.getId(), userId);
    }

    @Test
    @DisplayName("공연 홍보 상세 조회 성공")
    void getPromo_Success() {
        // Given
        Integer promoId = 1;
        Promo mockPromo = createMockPromo();
        mockPromo.setViewCount(100);

        when(promoRepository.findByIdAndNotDeleted(promoId)).thenReturn(mockPromo);

        // When
        PromoRespDTO result = promoService.getPromo(promoId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("테스트 공연");
        assertThat(mockPromo.getViewCount()).isEqualTo(101); // 조회수 증가 확인

        verify(promoRepository).findByIdAndNotDeleted(promoId);
    }

    @Test
    @DisplayName("공연 홍보 상세 조회 실패 - 존재하지 않는 공연")
    void getPromo_NotFound() {
        // Given
        Integer promoId = 999;

        when(promoRepository.findByIdAndNotDeleted(promoId)).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> promoService.getPromo(promoId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("공연 홍보를 찾을 수 없습니다.");

        verify(promoRepository).findByIdAndNotDeleted(promoId);
    }

    @Test
    @DisplayName("공연 홍보 상세 조회 성공 - 사용자별 좋아요 상태 포함")
    void getPromo_WithUserLike_Success() {
        // Given
        Integer promoId = 1;
        Integer userId = 1;
        Promo mockPromo = createMockPromo();
        mockPromo.setViewCount(50);

        when(promoRepository.findByIdAndNotDeleted(promoId)).thenReturn(mockPromo);
        when(promoLikeService.isLikedByUser(promoId, userId)).thenReturn(false);

        // When
        PromoRespDTO result = promoService.getPromo(promoId, userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("테스트 공연");
        assertThat(mockPromo.getViewCount()).isEqualTo(51); // 조회수 증가 확인

        verify(promoRepository).findByIdAndNotDeleted(promoId);
        verify(promoLikeService).isLikedByUser(promoId, userId);
    }

    @Test
    @DisplayName("공연 홍보 생성 성공")
    void createPromo_Success() {
        // Given
        Integer creatorId = 1;
        PromoReqDTO request = createMockPromoReqDTO();
        Users mockCreator = createMockUser();
        Promo savedPromo = createMockPromo();

        when(userValidationUtil.getUserById(creatorId)).thenReturn(mockCreator);
        when(promoRepository.save(any(Promo.class))).thenReturn(savedPromo);

        // When
        PromoSimpleRespDTO result = promoService.createPromo(request, creatorId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(savedPromo.getId());

        verify(userValidationUtil).getUserById(creatorId);
        verify(promoRepository).save(any(Promo.class));
    }

    @Test
    @DisplayName("공연 홍보 생성 성공 - 이미지 포함")
    void createPromo_WithImage_Success() {
        // Given
        Integer creatorId = 1;
        PromoReqDTO request = createMockPromoReqDTO();
        MultipartFile mockImage = mock(MultipartFile.class);
        request.setImage(mockImage);
        
        Users mockCreator = createMockUser();
        Promo savedPromo = createMockPromo();

        when(userValidationUtil.getUserById(creatorId)).thenReturn(mockCreator);
        when(promoRepository.save(any(Promo.class))).thenReturn(savedPromo);
        when(mockImage.isEmpty()).thenReturn(false);
        when(s3FileManagementUtil.uploadFile(any(), anyString(), anyString())).thenReturn("https://example.com/image.jpg");

        // When
        PromoSimpleRespDTO result = promoService.createPromo(request, creatorId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(savedPromo.getId());

        verify(userValidationUtil).getUserById(creatorId);
        verify(promoRepository).save(any(Promo.class));
        verify(s3FileManagementUtil).uploadFile(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("공연 홍보 검색 성공")
    void searchPromos_Success() {
        // Given
        String keyword = "테스트";
        Pageable pageable = PageRequest.of(0, 20);
        Promo mockPromo = createMockPromo();
        Page<Promo> promoPage = new PageImpl<>(Arrays.asList(mockPromo));

        when(promoRepository.searchByKeyword(keyword, pageable)).thenReturn(promoPage);

        // When
        Page<PromoRespDTO> result = promoService.searchPromos(keyword, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("테스트 공연");

        verify(promoRepository).searchByKeyword(keyword, pageable);
    }

    @Test
    @DisplayName("공연 홍보 검색 성공 - 사용자별 좋아요 상태 포함")
    void searchPromos_WithUserLike_Success() {
        // Given
        String keyword = "테스트";
        Integer userId = 1;
        Pageable pageable = PageRequest.of(0, 20);
        Promo mockPromo = createMockPromo();
        Page<Promo> promoPage = new PageImpl<>(Arrays.asList(mockPromo));

        when(promoRepository.searchByKeyword(keyword, pageable)).thenReturn(promoPage);
        when(promoLikeService.isLikedByUser(anyInt(), anyInt())).thenReturn(true);

        // When
        Page<PromoRespDTO> result = promoService.searchPromos(keyword, userId, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("테스트 공연");

        verify(promoRepository).searchByKeyword(keyword, pageable);
        verify(promoLikeService).isLikedByUser(mockPromo.getId(), userId);
    }

    @Test
    @DisplayName("공연 홍보 검색 - 빈 결과")
    void searchPromos_EmptyResult() {
        // Given
        String keyword = "존재하지않는키워드";
        Pageable pageable = PageRequest.of(0, 20);
        Page<Promo> emptyPage = new PageImpl<>(Collections.emptyList());

        when(promoRepository.searchByKeyword(keyword, pageable)).thenReturn(emptyPage);

        // When
        Page<PromoRespDTO> result = promoService.searchPromos(keyword, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();

        verify(promoRepository).searchByKeyword(keyword, pageable);
    }

    @Test
    @DisplayName("공연 홍보 필터링 성공")
    void filterPromos_Success() {
        // Given
        LocalDateTime startDate = LocalDateTime.now();
        LocalDateTime endDate = LocalDateTime.now().plusDays(7);
        String teamName = "테스트 팀";
        Pageable pageable = PageRequest.of(0, 20);
        Promo mockPromo = createMockPromo();
        Page<Promo> promoPage = new PageImpl<>(Arrays.asList(mockPromo));

        when(promoRepository.filterPromosByTeamName(startDate, endDate, teamName, pageable))
                .thenReturn(promoPage);

        // When
        Page<PromoRespDTO> result = promoService.filterPromos(startDate, endDate, teamName, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTeamName()).isEqualTo("테스트 팀");

        verify(promoRepository).filterPromosByTeamName(startDate, endDate, teamName, pageable);
    }

    // Helper methods for creating mock objects
    private Promo createMockPromo() {
        Promo promo = new Promo();
        promo.setId(1);
        promo.setTitle("테스트 공연");
        promo.setTeamName("테스트 팀");
        promo.setDescription("테스트 공연 설명");
        promo.setLocation("테스트 장소");
        promo.setEventDatetime(LocalDateTime.now().plusDays(1));
        promo.setAdmissionFee(BigDecimal.valueOf(10000));
        promo.setViewCount(0);
        promo.setLikeCount(0);
        promo.setCommentCount(0);
        promo.setCreatedAt(LocalDateTime.now());
        promo.setCreator(createMockUser());
        return promo;
    }

    private Users createMockUser() {
        Users user = new Users();
        user.setId(1);
        user.setNickname("테스트 사용자");
        return user;
    }

    private PromoReqDTO createMockPromoReqDTO() {
        PromoReqDTO request = new PromoReqDTO();
        request.setTitle("테스트 공연");
        request.setTeamName("테스트 팀");
        request.setDescription("테스트 공연 설명");
        request.setLocation("테스트 장소");
        request.setEventDatetime(LocalDateTime.now().plusDays(1));
        request.setAdmissionFee(BigDecimal.valueOf(10000));
        return request;
    }
}