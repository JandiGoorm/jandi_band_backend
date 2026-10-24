package com.jandi.band_backend.health;

import com.jandi.band_backend.invite.store.InviteCodeService;
import com.jandi.band_backend.invite.store.InviteType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.jandi.band_backend.persistence.ExpiringValueRepository;
import java.time.Duration;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Tag(name = "Redis Health Check API")
@RestController
@RequiredArgsConstructor
@Slf4j
public class PersistenceHealthController {
    
    private final InviteCodeService inviteCodeService;
    private final ExpiringValueRepository values;
    private final DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;

    @Operation(summary = "Redis 연결 상태 확인")
    @GetMapping("/health/redis")
    public ResponseEntity<Map<String, Object>> redisHealthCheck() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            log.info("Redis 상태 확인 시작");
            
            // 기존 진단 API의 경로와 응답 형식 유지
            if (values == null) {
                throw new RuntimeException("StringRedisTemplate is null");
            }
            
            // 만료 데이터 저장소 읽기·쓰기 확인
            values.put("health-check", "test-value", Duration.ofMinutes(1));
            String basicTest = values.get("health-check").orElse(null);
            
            if (!"test-value".equals(basicTest)) {
                throw new RuntimeException("Basic Redis operation failed");
            }
            
            // 3. InviteCodeService 주입 확인
            if (inviteCodeService == null) {
                throw new RuntimeException("InviteCodeService is null");
            }
            
            // 초대 코드 저장·조회 확인
            String testCode = "health-test-" + System.currentTimeMillis();
            inviteCodeService.saveCode(InviteType.CLUB, 999, testCode);
            String result = inviteCodeService.getKeyId(testCode);
            
            // 5. 정리
            inviteCodeService.deleteRecord(testCode);
            values.delete("health-check");
            
            // 성공 응답
            response.put("status", "UP");
            response.put("message", "Redis connection successful");
            response.put("timestamp", formatter.format(Instant.now()));
            response.put("basicTest", "PASS");
            response.put("inviteServiceTest", "PASS");
            response.put("retrievedValue", result);
            
            log.info("Redis 상태 확인 성공");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Redis 상태 확인 실패", e);
            
            // 실패 응답
            response.put("status", "DOWN");
            response.put("message", "Redis connection failed: " + e.getMessage());
            response.put("timestamp", formatter.format(Instant.now()));
            response.put("error", e.getClass().getSimpleName());
            
            return ResponseEntity.status(503).body(response);
        }
    }

    @Operation(summary = "Redis 기본 연결 테스트")
    @GetMapping("/health/redis/basic")
    public ResponseEntity<Map<String, Object>> basicRedisTest() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            log.info("Redis 기본 연결 테스트 시작");
            
            // 만료 데이터 저장소 읽기·쓰기 확인
            String testKey = "basic-test-" + System.currentTimeMillis();
            values.put(testKey, "ping", Duration.ofMinutes(1));
            String result = values.get(testKey).orElse(null);
            values.delete(testKey);
            
            response.put("status", "UP");
            response.put("message", "Basic Redis test successful");
            response.put("timestamp", formatter.format(Instant.now()));
            response.put("testResult", result);
            
            log.info("Redis 기본 연결 테스트 성공");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Redis 기본 연결 테스트 실패", e);
            
            response.put("status", "DOWN");
            response.put("message", "Basic Redis test failed: " + e.getMessage());
            response.put("timestamp", formatter.format(Instant.now()));
            response.put("error", e.getClass().getSimpleName());
            
            return ResponseEntity.status(503).body(response);
        }
    }

    @Operation(summary = "Redis 설정 정보 확인")
    @GetMapping("/health/redis/info")
    public ResponseEntity<Map<String, Object>> redisInfo() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            response.put("connectionFactory", values.getClass().getName());
            response.put("timestamp", formatter.format(Instant.now()));
            response.put("status", "INFO_RETRIEVED");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Redis 정보 확인 실패", e);
            
            response.put("status", "ERROR");
            response.put("message", "Failed to retrieve Redis info: " + e.getMessage());
            response.put("timestamp", formatter.format(Instant.now()));
            
            return ResponseEntity.status(500).body(response);
        }
    }
} 