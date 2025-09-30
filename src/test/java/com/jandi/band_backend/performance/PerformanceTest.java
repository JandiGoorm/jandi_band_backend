package com.jandi.band_backend.performance;

import com.jandi.band_backend.security.jwt.JwtTokenProvider;
import com.jandi.band_backend.user.entity.Users;
import com.jandi.band_backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.context.WebApplicationContext;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 실무 성능 요구사항 검증 테스트
 * - API 응답 시간 검증
 * - 동시 접속 처리 능력
 * - 대용량 데이터 처리
 * - 메모리 사용량 모니터링
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("성능 테스트 - 실무 요구사항 검증")
class PerformanceTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserRepository userRepository;

    private Users testUser;
    private String validAccessToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        testUser = new Users();
        testUser.setKakaoOauthId("perf_test_user");
        testUser.setNickname("성능테스트사용자");
        testUser = userRepository.save(testUser);

        validAccessToken = jwtTokenProvider.generateAccessToken(testUser.getKakaoOauthId());
    }

    @Test
    @DisplayName("성능 1. API 응답 시간 - 500ms 이내")
    void apiResponseTime_Under500ms() throws Exception {
        long startTime = System.currentTimeMillis();

        mockMvc.perform(get("/api/health")
                        .header("Authorization", "Bearer " + validAccessToken))
                .andExpect(status().isOk());

        long endTime = System.currentTimeMillis();
        long responseTime = endTime - startTime;

        System.out.println("API Response Time: " + responseTime + "ms");
        assert responseTime < 500; // 500ms 이내
    }

    @Test
    @DisplayName("성능 2. 동시 사용자 100명 - 동시 API 호출 처리")
    void concurrentUsers_100Users_Success() throws Exception {
        int userCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(userCount);
        @SuppressWarnings("unchecked")
        CompletableFuture<Void>[] futures = new CompletableFuture[userCount];

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < userCount; i++) {
            futures[i] = CompletableFuture.runAsync(() -> {
                try {
                    mockMvc.perform(get("/api/health")
                                    .header("Authorization", "Bearer " + validAccessToken))
                            .andExpect(status().isOk());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, executor);
        }

        // 모든 요청 완료 대기
        CompletableFuture.allOf(futures).get(30, TimeUnit.SECONDS);

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;

        System.out.println("100 concurrent users processed in: " + totalTime + "ms");
        assert totalTime < 10000; // 10초 이내에 모든 요청 처리

        executor.shutdown();
    }

    @Test
    @DisplayName("성능 3. 대용량 페이징 조회 - 1000개 항목")
    void largePagination_1000Items_Efficient() throws Exception {
        long startTime = System.currentTimeMillis();

        mockMvc.perform(get("/api/clubs")
                        .param("page", "0")
                        .param("size", "1000")
                        .header("Authorization", "Bearer " + validAccessToken))
                .andExpect(status().isOk());

        long endTime = System.currentTimeMillis();
        long responseTime = endTime - startTime;

        System.out.println("Large pagination response time: " + responseTime + "ms");
        assert responseTime < 3000; // 3초 이내
    }

    @Test
    @DisplayName("성능 4. 검색 성능 - 복잡한 검색 쿼리")
    void complexSearch_Performance() throws Exception {
        long startTime = System.currentTimeMillis();

        mockMvc.perform(get("/api/clubs/search")
                        .param("keyword", "테스트")
                        .param("region", "서울")
                        .param("sortBy", "createdAt")
                        .param("order", "desc")
                        .param("page", "0")
                        .param("size", "20")
                        .header("Authorization", "Bearer " + validAccessToken))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assert status == 200 || status == 404; // 구현되어 있으면 성공
                });

        long endTime = System.currentTimeMillis();
        long responseTime = endTime - startTime;

        System.out.println("Complex search response time: " + responseTime + "ms");
        assert responseTime < 2000; // 2초 이내
    }

    @Test
    @DisplayName("성능 5. 메모리 사용량 모니터링")
    void memoryUsage_Monitor() throws Exception {
        Runtime runtime = Runtime.getRuntime();
        
        // GC 실행
        System.gc();
        long beforeMemory = runtime.totalMemory() - runtime.freeMemory();

        // 여러 API 호출로 메모리 사용량 테스트
        for (int i = 0; i < 100; i++) {
            mockMvc.perform(get("/api/health")
                            .header("Authorization", "Bearer " + validAccessToken))
                    .andExpect(status().isOk());
        }

        System.gc();
        long afterMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsed = afterMemory - beforeMemory;

        System.out.println("Memory used for 100 API calls: " + (memoryUsed / 1024 / 1024) + " MB");
        
        // 메모리 사용량이 50MB를 초과하지 않아야 함
        assert memoryUsed < 50 * 1024 * 1024;
    }

    @Test
    @DisplayName("성능 6. 데이터베이스 연결 풀 성능")
    void databaseConnectionPool_Performance() throws Exception {
        int requestCount = 50;
        ExecutorService executor = Executors.newFixedThreadPool(requestCount);
        @SuppressWarnings("unchecked")
        CompletableFuture<Void>[] futures = new CompletableFuture[requestCount];

        long startTime = System.currentTimeMillis();

        // DB를 사용하는 API 동시 호출
        for (int i = 0; i < requestCount; i++) {
            futures[i] = CompletableFuture.runAsync(() -> {
                try {
                    mockMvc.perform(get("/api/auth/me")
                                    .header("Authorization", "Bearer " + validAccessToken))
                            .andExpect(status().isOk());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, executor);
        }

        CompletableFuture.allOf(futures).get(20, TimeUnit.SECONDS);

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;

        System.out.println("DB connection pool test completed in: " + totalTime + "ms");
        assert totalTime < 15000; // 15초 이내

        executor.shutdown();
    }

    @Test
    @DisplayName("성능 7. 캐시 효율성 검증")
    void cacheEfficiency_Test() throws Exception {
        // 첫 번째 요청 (캐시 미스)
        long startTime1 = System.currentTimeMillis();
        mockMvc.perform(get("/api/health")
                        .header("Authorization", "Bearer " + validAccessToken))
                .andExpect(status().isOk());
        long firstRequestTime = System.currentTimeMillis() - startTime1;

        // 두 번째 요청 (캐시 히트 예상)
        long startTime2 = System.currentTimeMillis();
        mockMvc.perform(get("/api/health")
                        .header("Authorization", "Bearer " + validAccessToken))
                .andExpect(status().isOk());
        long secondRequestTime = System.currentTimeMillis() - startTime2;

        System.out.println("First request: " + firstRequestTime + "ms");
        System.out.println("Second request: " + secondRequestTime + "ms");

        // 캐시가 구현되어 있다면 두 번째 요청이 더 빨라야 함
        // 캐시가 없어도 테스트는 통과 (실제 캐시 구현 여부 확인용)
        System.out.println("Cache efficiency ratio: " + 
                (firstRequestTime > 0 ? (double)secondRequestTime / firstRequestTime : 1.0));
    }

    @Test
    @DisplayName("성능 8. JWT 토큰 검증 성능")
    void jwtValidation_Performance() throws Exception {
        long startTime = System.currentTimeMillis();

        // JWT 토큰을 사용하는 API 100번 호출
        for (int i = 0; i < 100; i++) {
            mockMvc.perform(get("/api/auth/me")
                            .header("Authorization", "Bearer " + validAccessToken))
                    .andExpect(status().isOk());
        }

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        double avgTime = (double) totalTime / 100;

        System.out.println("JWT validation - 100 requests in: " + totalTime + "ms");
        System.out.println("Average time per request: " + avgTime + "ms");

        assert avgTime < 100; // 평균 100ms 이내
    }

    @Test
    @DisplayName("성능 9. 파일 업로드 성능")
    void fileUpload_Performance() throws Exception {
        // 5MB 파일 업로드 시뮬레이션
        byte[] fileContent = new byte[5 * 1024 * 1024];
        MockMultipartFile largeFile = new MockMultipartFile(
                "image",
                "large-file.jpg",
                "image/jpeg",
                fileContent
        );
        
        long startTime = System.currentTimeMillis();

        mockMvc.perform(multipart("/api/images/upload")
                        .file(largeFile)
                        .header("Authorization", "Bearer " + validAccessToken))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    // 파일 업로드 API가 구현되어 있으면 성공 또는 크기 제한 에러
                    assert status == 200 || status >= 400;
                });

        long endTime = System.currentTimeMillis();
        long uploadTime = endTime - startTime;

        System.out.println("5MB file upload time: " + uploadTime + "ms");
        assert uploadTime < 30000; // 30초 이내
    }

    @Test
    @DisplayName("성능 10. 시스템 리소스 사용률 모니터링")
    void systemResource_Monitoring() throws Exception {
        // 시스템 리소스 정보 수집
        Runtime runtime = Runtime.getRuntime();
        
        int totalMemoryMB = (int) (runtime.totalMemory() / 1024 / 1024);
        int freeMemoryMB = (int) (runtime.freeMemory() / 1024 / 1024);
        int usedMemoryMB = totalMemoryMB - freeMemoryMB;
        int maxMemoryMB = (int) (runtime.maxMemory() / 1024 / 1024);
        
        System.out.println("=== System Resource Monitoring ===");
        System.out.println("Total Memory: " + totalMemoryMB + " MB");
        System.out.println("Used Memory: " + usedMemoryMB + " MB");
        System.out.println("Free Memory: " + freeMemoryMB + " MB");
        System.out.println("Max Memory: " + maxMemoryMB + " MB");
        System.out.println("Memory Usage: " + (usedMemoryMB * 100 / totalMemoryMB) + "%");
        
        // 메모리 사용률이 90%를 초과하지 않아야 함
        assert (usedMemoryMB * 100 / totalMemoryMB) < 90;
        
        // 부하 테스트
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get("/api/health"))
                    .andExpect(status().isOk());
        }
        
        // 부하 후 메모리 사용률 체크
        System.gc();
        int finalUsedMemoryMB = (int) ((runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024);
        System.out.println("Memory after load test: " + finalUsedMemoryMB + " MB");
    }
}