# 테스트 가이드 (TEST_GUIDE.md)

## 개요

이 문서는 RhythMeet 백엔드 프로젝트의 테스트 코드 구현, 실행 및 분석 방법을 설명합니다. 실무적으로 필요한 정도의 테스트만 작성하여 CI/CD 파이프라인에서 안정적으로 동작하도록 설계되었습니다.

## 테스트 구조

### 1. 테스트 계층

```
src/test/java/com/jandi/band_backend/
├── repository/          # 데이터 액세스 계층 테스트
├── service/            # 비즈니스 로직 계층 테스트
├── security/           # 보안 관련 테스트
├── controller/         # API 엔드포인트 테스트
├── integration/        # 통합 테스트
└── testutil/           # 테스트 유틸리티
```

### 2. 테스트 종류

#### 단위 테스트 (Unit Tests)
- **Repository Tests**: JPA 엔티티 및 쿼리 메서드 검증
- **Service Tests**: 비즈니스 로직 검증
- **Controller Tests**: API 계약 및 HTTP 응답 검증

#### 통합 테스트 (Integration Tests)
- **AuthControllerIntegrationTest**: 인증 흐름 전체 검증
- **FileUploadIntegrationTest**: 파일 업로드 기능 검증

## 테스트 실행 방법

### 1. 전체 테스트 실행
```bash
./gradlew test
```

### 2. 특정 테스트 클래스 실행
```bash
./gradlew test --tests "*ControllerTest*"
./gradlew test --tests "*ServiceTest*"
./gradlew test --tests "*RepositoryTest*"
```

### 3. 특정 테스트 메서드 실행
```bash
./gradlew test --tests "PollControllerTest.createPoll_Success"
```

### 4. 실패 시 계속 실행 (CI/CD용)
```bash
./gradlew test --continue
```

### 5. 테스트 리포트 확인
```bash
# HTML 리포트
open build/reports/tests/test/index.html

# 실패한 테스트만 확인
./gradlew test --tests "*Test*" --continue | findstr "FAILED"
```

## 테스트 구현 패턴

### 1. Repository 테스트

```java
@SpringBootTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void findByKakaoOauthId_존재하는_사용자() {
        // Given
        Users user = createTestUser();
        userRepository.save(user);

        // When
        Optional<Users> found = userRepository.findByKakaoOauthId("test-kakao-id");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getKakaoOauthId()).isEqualTo("test-kakao-id");
    }
}
```

**특징:**
- `@SpringBootTest`로 전체 컨텍스트 로드
- `@ActiveProfiles("test")`로 H2 데이터베이스 사용
- 실제 데이터베이스 연동으로 쿼리 정확성 검증

### 2. Service 테스트

```java
@SpringBootTest
@ActiveProfiles("test")
class UserServiceTest {

    @Autowired
    private UserService userService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void createUser_정상_케이스() {
        // Given
        UserCreateReqDTO request = UserCreateReqDTO.builder()
                .kakaoOauthId("test-id")
                .build();

        // When
        UserRespDTO result = userService.createUser(request);

        // Then
        assertThat(result.getId()).isNotNull();
        assertThat(result.getKakaoOauthId()).isEqualTo("test-id");
    }
}
```

**특징:**
- 외부 의존성(JWT, Redis 등)은 `@MockBean`으로 mocking
- 실제 비즈니스 로직 검증
- 데이터베이스 트랜잭션 롤백

### 3. Controller 테스트

```java
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class PollControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PollService pollService;

    @Test
    void createPoll_Success() throws Exception {
        // Given
        PollReqDTO request = createValidPollRequest();
        PollRespDTO response = createPollResponse();
        when(pollService.createPoll(any(), eq(1))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/polls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("Authorization", "Bearer mock-token"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1));
    }
}
```

**특징:**
- `@AutoConfigureMockMvc`로 MockMvc 자동 설정
- 서비스 계층은 `@MockBean`으로 격리
- HTTP 요청/응답 검증
- JWT 토큰은 mock 토큰 사용

## 테스트 데이터 관리

### 1. TestDataFactory

```java
@Component
public class TestDataFactory {

    public Users createTestUser() {
        return Users.builder()
                .kakaoOauthId("test-kakao-id")
                .name("테스트 사용자")
                .email("test@example.com")
                .university(createTestUniversity())
                .build();
    }

    public University createTestUniversity() {
        return University.builder()
                .name("테스트대학교")
                .region(createTestRegion())
                .build();
    }
}
```

### 2. 테스트 픽스처

```java
class PollControllerTest {

    private PollReqDTO validPollReqDTO;
    private PollRespDTO pollRespDTO;

    @BeforeEach
    void setUp() {
        validPollReqDTO = PollReqDTO.builder()
                .title("테스트 투표")
                .clubId(1)
                .endDatetime(LocalDateTime.now().plusDays(30))
                .build();

        pollRespDTO = PollRespDTO.builder()
                .id(1)
                .title("테스트 투표")
                .clubId(1)
                .build();
    }
}
```

## CI/CD 통합

### 1. Jenkins 파이프라인

```groovy
pipeline {
    stages {
        stage('Test') {
            steps {
                sh './gradlew test --continue'
                junit 'build/test-results/test/*.xml'
            }
        }
    }
    post {
        always {
            publishHTML([
                allowMissing: false,
                alwaysLinkToLastBuild: true,
                keepAll: true,
                reportDir: 'build/reports/tests/test',
                reportFiles: 'index.html',
                reportName: 'Test Report'
            ])
        }
    }
}
```

### 2. 테스트 커버리지

```gradle
// build.gradle
plugins {
    id 'jacoco'
}

jacocoTestReport {
    reports {
        xml.required = true
        html.required = true
    }
}
```

## 테스트 분석 및 디버깅

### 1. 실패 테스트 분석

```bash
# 상세 로그 확인
./gradlew test --info --tests "FailingTest"

# 스택트레이스 확인
./gradlew test --stacktrace
```

### 2. 테스트 리포트 분석

- **빌드 성공률**: 전체 테스트 중 성공한 비율
- **실패 패턴**: 특정 모듈/기능에서 반복 실패
- **성능 저하**: 테스트 실행 시간 증가 추이

### 3. 커버리지 분석

```bash
./gradlew jacocoTestReport
open build/reports/jacoco/test/html/index.html
```

## 테스트 작성 원칙

### 1. 실무적 접근
- ✅ **포함**: 핵심 비즈니스 로직 검증
- ✅ **포함**: API 계약 및 HTTP 응답 검증
- ✅ **포함**: 데이터 무결성 및 제약조건 검증
- ❌ **제외**: 과도한 입력 검증 테스트
- ❌ **제외**: UI/프론트엔드 연동 테스트
- ❌ **제외**: 성능 테스트 (별도 분리)

### 2. 유지보수성
- 테스트 픽스처를 재사용 가능한 메서드로 분리
- 테스트 데이터는 의미 있는 값 사용
- 테스트 이름은 `기능_조건_결과` 패턴 사용

### 3. CI/CD 호환성
- `--continue` 옵션으로 전체 실패 확인
- 테스트 간 독립성 보장 (데이터 격리)
- 외부 의존성 최소화 (mocking 적극 활용)

## 테스트 실행 환경

### 1. 로컬 개발 환경
```bash
# Elasticsearch 시작
.\search\start-elasticsearch-windows.ps1

# 모니터링 스택 시작
.\monitoring-local\scripts\start-local.ps1

# 테스트 실행
./gradlew test
```

### 2. 테스트 프로파일 설정

```yaml
# application-test.yml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: false
```

## 문제 해결 가이드

### 1. 일반적인 문제

**Q: 테스트가 느려요**
A: `@SpringBootTest` 대신 `@WebMvcTest` 사용, 불필요한 컨텍스트 로드 방지

**Q: 데이터베이스 연결 실패**
A: `@ActiveProfiles("test")` 확인, H2 설정 검증

**Q: MockBean이 동작하지 않아요**
A: `@SpringBootTest`에서만 사용 가능, 단위 테스트에서는 `@Mock` 사용

### 2. 디버깅 팁

- 테스트별 로그 레벨 설정
- `@DirtiesContext`로 컨텍스트 재생성
- 실제 데이터베이스 쿼리 확인 (`show-sql: true`)

## 결론

이 가이드를 따라 테스트를 작성하면:
- CI/CD 파이프라인에서 안정적 실행
- 코드 변경 시 빠른 회귀 검증
- 유지보수 비용 최소화
- 실무적 요구사항 충족

테스트는 "품질 보증"이 아닌 "빠른 피드백"을 위한 도구임을 기억하세요.</content>
<parameter name="filePath">c:\Users\USER\source\jandi_band_backend\TEST_GUIDE.md