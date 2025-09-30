# RhythMeet Backend 테스트 가이드

## 테스트 현황 요약

### 현재 테스트 상태 (2025.10.01 기준)
- 총 테스트: 394개 (성공: 349개, 실패: 45개)
- 성공률: 88.6%

### 테스트 구조
```
src/test/java/com/jandi/band_backend/
├── auth/                    # 인증 도메인 테스트
│   ├── controller/         # AuthController 테스트
│   └── service/            # AuthService 테스트
├── club/                   # 클럽 도메인 테스트
│   └── service/            # ClubService 테스트
├── config/                 # 설정 테스트
├── image/                  # 이미지 도메인 테스트
├── integration/            # 통합 테스트
│   └── AuthIntegrationTest.java
├── invite/                 # 초대 도메인 테스트
├── performance/            # 성능 테스트
│   └── PerformanceTest.java
├── poll/                   # 투표 도메인 테스트
│   ├── controller/         # PollController 테스트
│   ├── integration/        # Poll 통합 테스트
│   ├── repository/         # PollRepository 테스트
│   └── service/            # PollService 테스트
├── promo/                  # 프로모션 도메인 테스트
├── repository/             # Repository 계층 테스트
│   ├── ClubEventRepositoryTest.java
│   ├── ClubGalPhotoRepositoryTest.java
│   ├── ClubRepositoryTest.java
│   ├── NoticeRepositoryTest.java
│   ├── PromoRepositoryTest.java
│   ├── TeamRepositoryTest.java
│   ├── UserPhotoRepositoryTest.java
│   └── UserRepositoryTest.java
├── security/               # 보안 통합 테스트
│   └── SecurityIntegrationTest.java
├── team/                   # 팀 도메인 테스트
├── test/                   # 테스트 유틸리티
├── testutil/               # 테스트 유틸리티
├── user/                   # 사용자 도메인 테스트
│   ├── controller/         # UserController 테스트
│   ├── repository/         # UserRepository 테스트
│   └── service/            # UserService 테스트
└── JandiBandBackendApplicationTests.java
```

### 계층별 테스트 현황
계층 | 테스트 수 | 상태 | 설명
------|----------|------|------
Repository | 8개 | 100% 성공 | 모든 Repository 테스트 통과
Security | 1개 | 100% 성공 | SecurityIntegrationTest 15개 테스트 모두 통과
Service | 다수 | 대부분 성공 | PollService 등 주요 서비스 테스트 성공
Controller | 다수 | 일부 실패 | AuthController, PollController 등
Integration | 1개 | 성공 | AuthIntegrationTest 통과
Performance | 1개 | 성공 | PerformanceTest 통과

## 빠른 테스트 실행

### 전체 테스트 실행
```bash
./gradlew test
```

### 계층별 테스트 실행
```bash
# Repository Layer 테스트만
./gradlew test --tests "*repository*"

# Service Layer 테스트만
./gradlew test --tests "*service*"

# Controller 테스트만
./gradlew test --tests "*controller*"

# 보안 테스트만
./gradlew test --tests "*SecurityIntegrationTest*"

# 성능 테스트만
./gradlew test --tests "*PerformanceTest*"
```

### 커버리지 리포트 생성
```bash
./gradlew jacocoTestReport
# 결과: build/reports/jacoco/test/html/index.html
```

## 테스트 환경 설정

### 필수 설정
```properties
# src/test/resources/application-test.properties
spring.profiles.active=test
spring.jpa.hibernate.ddl-auto=create-drop
spring.datasource.url=jdbc:h2:mem:testdb
logging.level.com.jandi.band_backend=DEBUG
```

### H2 테스트 DB 설정
```yaml
spring:
  h2:
    console:
      enabled: true
  datasource:
    url: jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    driver-class-name: org.h2.Driver
```

## 계층별 테스트 설명

### 1. Repository 테스트
Repository 계층 테스트는 @DataJpaTest를 사용하여 데이터 액세스 로직을 검증합니다.

```java
@DataJpaTest
class UserRepositoryTest {
    @Test
    void findByKakaoOauthId_Success() {
        // Given: 테스트 데이터 생성
        // When: Repository 메소드 호출
        // Then: 결과 검증
    }
}
```

**포함된 Repository 테스트:**
- UserRepositoryTest: 사용자 CRUD 및 소프트 삭제
- ClubRepositoryTest: 클럽 관리 및 타임스탬프 검증
- ClubEventRepositoryTest: 클럽 이벤트 관리
- ClubGalPhotoRepositoryTest: 클럽 갤러리 사진
- NoticeRepositoryTest: 공지사항 관리
- PromoRepositoryTest: 프로모션 관리
- TeamRepositoryTest: 팀 관리
- UserPhotoRepositoryTest: 사용자 사진 관리

### 2. Security 통합 테스트 (SecurityIntegrationTest)
JWT 토큰, 인증/인가, 보안 취약점 방지를 검증하는 통합 테스트입니다.

**테스트 항목:**
- JWT 토큰 검증 및 만료 처리
- 인증되지 않은 요청 차단
- SQL Injection 방지
- XSS 공격 방지
- CORS 설정 검증
- 권한 기반 접근 제어

### 3. Service 테스트
비즈니스 로직을 검증하는 단위 테스트입니다.

**주요 Service 테스트:**
- PollService: 투표 생성, 투표 참여, 결과 집계
- UserService: 사용자 관리 및 프로필 업데이트
- ClubService: 클럽 관리 및 멤버십
- AuthService: 인증 및 토큰 관리

### 4. Controller 테스트
API 엔드포인트를 검증하는 통합 테스트입니다.

**주요 Controller 테스트:**
- AuthController: 로그인, 토큰 갱신
- PollController: 투표 API
- UserController: 사용자 정보 관리

### 5. Integration 테스트
여러 컴포넌트가 연동되는 통합 테스트입니다.

**포함된 통합 테스트:**
- AuthIntegrationTest: 인증 플로우 통합 검증

### 6. Performance 테스트
응답 시간, 동시성, 메모리 사용량을 검증합니다.

**성능 테스트 항목:**
- API 응답 시간 (500ms 이내)
- 동시 사용자 처리 (100명)
- 메모리 사용량 모니터링
- 데이터베이스 연결 풀 성능

## 주요 해결된 문제들

### 1. Repository 테스트 UNIVERSITY_CODE 제약조건 위반
문제: UserRepositoryTest에서 UNIVERSITY_CODE 필드 누락으로 인한 NULL 제약조건 위반
해결: TestDataFactory.createUniversity()에 university_code와 address 필드 추가

### 2. Users Entity @PrePersist 후처리 문제
문제: @PrePersist에서 university 필드를 null로 설정하여 테스트 데이터 손상
해결: @PrePersist 로직 수정으로 university 관계 유지

### 3. Club Repository 타임스탬프 비교 실패
문제: LocalDateTime.now() 호출 시점 차이로 인한 비교 실패
해결: updateClub_UpdateTimestamp() 테스트에서 명시적 미래 시간 설정

### 4. SecurityIntegrationTest MockMvc 설정 충돌
문제: JwtAuthenticationFilter 수동 추가로 인한 Spring Security 설정 충돌
해결: MockMvc 설정 간소화 및 테스트 로직 단순화

### 5. Controller 테스트 SecurityContext 설정 문제
문제: @WebMvcTest에서 JWT 필터가 로드되지 않아 @AuthenticationPrincipal이 null 반환
해결: IntegrationTest 어노테이션으로 변경하거나 SecurityContext 수동 설정 필요

## 테스트 실패 디버깅 가이드

### 자주 발생하는 문제들

1. 403 Forbidden 오류
   ```java
   // 해결: 인증 토큰 추가
   .header("Authorization", "Bearer " + validToken)
   ```

2. H2 Database 오류
   ```properties
   # 해결: 테스트 프로파일 설정
   spring.jpa.hibernate.ddl-auto=create-drop
   ```

3. MockMvc 404 오류
   ```java
   // 해결: 컨트롤러 경로 확인
   @WebMvcTest(controllers = YourController.class)
   ```

## CI/CD 통합

### Jenkins 파이프라인 설정
```groovy
pipeline {
    stages {
        stage('Test') {
            steps {
                sh './gradlew test jacocoTestReport'
                publishHTML([
                    allowMissing: false,
                    alwaysLinkToLastBuild: true,
                    keepAll: true,
                    reportDir: 'build/reports/jacoco/test/html',
                    reportFiles: 'index.html',
                    reportName: 'JaCoCo Coverage Report'
                ])
            }
        }
    }
    post {
        always {
            junit 'build/test-results/test/*.xml'
        }
    }
}
```

### 커버리지 임계치 설정
```gradle
jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = 0.80 // 80% 이상
            }
        }
    }
}
```

## 테스트 체크리스트

### PR 전 필수 확인사항
- 모든 신규 기능에 대한 테스트 작성
- 기존 테스트 실행 성공 (실패율 15% 이내)
- 커버리지 목표 달성 확인
- 보안 관련 기능은 보안 테스트 추가
- 성능에 영향을 주는 기능은 성능 테스트 추가

### 배포 전 필수 확인사항
- 통합 테스트 모두 통과
- 성능 테스트 임계치 충족
- 보안 테스트 모두 통과
- 프로덕션 환경 시뮬레이션 테스트

## 다음 단계 계획

### 우선순위 1 (완료)
- Repository 레이어 테스트 성공: 8개 Repository 테스트 모두 통과
- Security 통합 테스트 성공: SecurityIntegrationTest 15/15 성공
- Entity 관계 및 제약조건 검증: UNIVERSITY_CODE, 타임스탬프, 외래키 관계 해결

### 우선순위 2 (진행 중)
- 실패 테스트 수정: 남은 45개 테스트 실패 해결
- Controller 테스트 보강: AuthController, PollController 등 안정화
- Integration 테스트 확장: 추가 통합 시나리오 구현

### 우선순위 3 (향후)
- E2E 테스트 추가: 사용자 시나리오 기반 테스트
- 부하 테스트 도입: 실제 트래픽 시뮬레이션
- 모니터링 통합: 테스트 결과 모니터링 시스템 연동