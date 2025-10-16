# 테스트 코드 개선 제안서

> **작성 배경**: 7개 테스트 전략 아티클 분석 (Toss, Naver D2, 전문가 블로그) 기반  
> **목적**: 이미 개발이 완료된 프로젝트에 실용적이고 가치 있는 테스트를 추가하여 품질 개선  
> **원칙**: TDD 전환이 아닌, 기존 코드에 최소 비용으로 최대 효과를 내는 전략
> 
> ---
> 
> ## 🎉 실행 완료 상태
> 
> **✅ Phase 1 완료**: 100% (39/39 테스트 통과)  
> **✅ Phase 2 완료**: 87.2% (34/39 테스트 통과)  
> **📊 최종 성과**: 93.6% (73/78 테스트 통과, 28초 실행)  
> **🐛 버그 발견**: 실제 권한 검증 버그 3건 조기 발견
> 
> **자세한 내용**: [FINAL_TEST_COMPLETION_REPORT.md](./FINAL_TEST_COMPLETION_REPORT.md) 참조
> 
> ---

---

## 📊 현재 테스트 현황 분석

### ✅ 잘 되어 있는 부분
1. **Repository 테스트 (10개 @DataJpaTest)**
   - Soft delete, 페이징, 네이티브 쿼리 검증
   - H2 인메모리 DB로 빠른 피드백
   
2. **Service 단위 테스트 (Poll, Auth, Invite 중심)**
   - Mockito로 의존성 격리
   - Given-When-Then 구조 일관성
   - 355개 테스트 전부 통과

3. **통합 테스트 인프라**
   - `@IntegrationTest` 커스텀 애노테이션
   - TestContainers 준비 완료
   - `AuthIntegrationTest`로 핵심 인증 플로우 검증

### ⚠️ 개선이 필요한 부분

| 영역 | 현황 | 문제점 | 우선순위 |
|------|------|--------|----------|
| **GlobalExceptionHandler** | 테스트 없음 | 예외 응답 규약 보장 안됨 | 🔴 HIGH |
| **S3Service** | Mock만 존재 | 실제 동작 검증 없음 | 🟡 MEDIUM |
| **Controller 통합 테스트** | PollController만 존재 | 21개 Controller 중 1개만 | 🔴 HIGH |
| **CommonRespDTO** | 테스트 없음 | API 응답 표준 검증 안됨 | 🟡 MEDIUM |
| **비즈니스 통합 시나리오** | Auth만 존재 | 핵심 사용자 여정 검증 부족 | 🟠 HIGH-MEDIUM |
| **경계값/예외 케이스** | 일부 Service만 | Edge case 커버리지 낮음 | 🟢 LOW-MEDIUM |

---

## 🎯 Toss 전략 적용: 20%의 테스트로 80%의 신뢰 확보

> **핵심 원칙**: "모든 걸 테스트하려 하지 말고, 가장 중요한 것만 철저히 테스트하라"

### 가치가 높은 20% 영역 (우선 집중)

1. **핵심 사용자 여정 (Integration Test)**
   - 동아리 생성 → 팀 초대 → 투표 생성 → 투표 참여
   - 공연 홍보글 작성 → 댓글 작성 → 신고 처리
   - 회원 가입 → 프로필 설정 → 동아리 가입

2. **예외 처리 계약 (GlobalExceptionHandler)**
   - 모든 예외가 올바른 HTTP 상태 + errorCode 반환하는지
   - CommonRespDTO 형식 일관성

3. **권한 검증 로직**
   - 동아리/팀 접근 권한 체크
   - JWT 인증/인가 플로우

---

## 📝 구체적 개선 제안

### 1. GlobalExceptionHandler 테스트 추가 (최우선)

**왜 중요한가?**
- 모든 API 응답의 마지막 관문
- 클라이언트는 errorCode를 기준으로 에러 처리
- 현재 14개 예외 타입, 0개 테스트

**테스트 작성 예시**:
```java
@WebMvcTest(GlobalExceptionHandler.class)
@Import(TestController.class) // 테스트용 컨트롤러
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("UserNotFoundException -> 404 + USER_NOT_FOUND")
    void handleUserNotFoundException() throws Exception {
        mockMvc.perform(get("/test/user-not-found"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.errorCode").value("USER_NOT_FOUND"))
            .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("VoteAlreadyExistsException -> 409 + VOTE_ALREADY_EXISTS")
    void handleVoteAlreadyExists() throws Exception {
        mockMvc.perform(get("/test/vote-conflict"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.errorCode").value("VOTE_ALREADY_EXISTS"));
    }

    // 14개 예외 모두 테스트 (빠르게 작성 가능, 높은 가치)
}

@RestController
@RequestMapping("/test")
class TestController {
    @GetMapping("/user-not-found")
    void throwUserNotFound() { throw new UserNotFoundException("테스트"); }
    
    @GetMapping("/vote-conflict")
    void throwVoteConflict() { throw new VoteAlreadyExistsException("테스트"); }
    // ...
}
```

**작업량**: 1-2시간  
**효과**: API 응답 규약 100% 보장, 클라이언트 에러 핸들링 안정성 확보

---

### 2. Controller 계층 테스트 확대 (MockMvc 통합)

**현재 상황**: 21개 Controller 중 PollController만 테스트 존재

**전략**: Toss의 "실제 객체 사용" 원칙 적용
- `@SpringBootTest` + MockMvc (실제 Bean 사용)
- Service는 Mock 대신 실제 Bean 사용
- 외부 의존성(S3, Kakao)만 Mock

**우선순위별 Controller**:

#### 🔴 HIGH (먼저 작성)
1. **AuthController** - 회원가입/로그인/탈퇴
2. **ClubController** - 동아리 생성/조회/수정/삭제
3. **TeamController** - 팀 관리 (권한 검증 중요)

#### 🟡 MEDIUM
4. **PromoController** - 홍보글 CRUD
5. **InviteController** - 초대 코드 생성/사용
6. **NoticeController** - 공지사항

#### 🟢 LOW (나중에)
- UserController, MyPageController 등 단순 CRUD

**테스트 템플릿**:
```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ClubControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private S3Service s3Service; // 외부 의존만 Mock
    @MockBean private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("동아리 생성 - 인증된 사용자")
    void createClub_Success() throws Exception {
        // Given
        ClubReqDTO request = ClubReqDTO.builder()
            .name("새 동아리")
            .universityId(1)
            .build();
        
        CustomUserDetails user = createAuthenticatedUser(1);
        mockAuthentication(user);

        // When & Then
        mockMvc.perform(post("/api/clubs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .header("Authorization", "Bearer mock-token"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.name").value("새 동아리"));
    }

    @Test
    @DisplayName("동아리 생성 - 미인증 사용자는 401")
    void createClub_Unauthorized() throws Exception {
        mockMvc.perform(post("/api/clubs")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isUnauthorized());
    }
}
```

**작업량**: Controller당 30분-1시간 (HIGH 우선 3개 = 3-4시간)  
**효과**: HTTP 계약 보장, 인증/인가 로직 검증, 회귀 방지

---

### 3. 핵심 비즈니스 통합 시나리오 테스트

**Toss 전략**: "사용자 여정을 테스트하라"

현재 `AuthIntegrationTest`가 좋은 예시. 이를 확장:

#### 시나리오 1: 투표 생성부터 결과 확인까지
```java
@IntegrationTest
class PollLifecycleIntegrationTest {

    @Autowired private PollService pollService;
    @Autowired private PollSongService pollSongService;
    @Autowired private PollVotingService pollVotingService;

    @Test
    @DisplayName("투표 전체 생명주기: 생성 → 곡 추가 → 투표 → 결과 조회")
    void pollCompleteLifecycle() {
        // Given: 동아리와 사용자 준비
        Users creator = createUser("creator");
        Club club = createClub("테스트 동아리", creator);
        Users voter1 = createUser("voter1");
        Users voter2 = createUser("voter2");
        joinClub(club, voter1, voter2);

        // When 1: 투표 생성
        PollReqDTO pollReq = PollReqDTO.builder()
            .title("9월 정기공연 곡 선정")
            .clubId(club.getId())
            .endDatetime(LocalDateTime.now().plusDays(7))
            .build();
        PollRespDTO poll = pollService.createPoll(pollReq, creator.getId());

        // When 2: 곡 추가
        PollSongReqDTO song1 = createSongRequest("Bohemian Rhapsody", "Queen");
        PollSongReqDTO song2 = createSongRequest("Stairway to Heaven", "Led Zeppelin");
        PollSongRespDTO addedSong1 = pollSongService.addSongToPoll(poll.getId(), song1, creator.getId());
        PollSongRespDTO addedSong2 = pollSongService.addSongToPoll(poll.getId(), song2, voter1.getId());

        // When 3: 투표 참여
        pollVotingService.vote(poll.getId(), addedSong1.getId(), voter1.getId());
        pollVotingService.vote(poll.getId(), addedSong2.getId(), voter2.getId());

        // Then: 투표 결과 검증
        PollDetailRespDTO result = pollService.getPollDetail(poll.getId(), creator.getId());
        assertThat(result.getSongs()).hasSize(2);
        assertThat(result.getSongs().get(0).getVoteCount()).isEqualTo(1);
        assertThat(result.getSongs().get(1).getVoteCount()).isEqualTo(1);
    }
}
```

#### 시나리오 2: 동아리 가입부터 팀 활동까지
```java
@Test
@DisplayName("신규 회원의 동아리 가입 여정")
void newMemberJourneyToTeamActivity() {
    // Given
    Users admin = createUser("admin");
    Club club = createClub("밴드 동아리", admin);
    Team team = createTeam("기타팀", club, admin);

    // When 1: 초대 코드 생성
    String inviteCode = inviteService.generateInviteCode(club.getId(), admin.getId());

    // When 2: 신규 회원 가입
    Users newMember = createUser("newbie");
    
    // When 3: 초대 코드로 동아리 가입
    joinService.joinClubByInviteCode(inviteCode, newMember.getId());

    // When 4: 팀 가입
    teamService.addMemberToTeam(team.getId(), newMember.getId(), admin.getId());

    // Then: 멤버십 검증
    Club joinedClub = clubService.getClubDetail(club.getId(), newMember.getId());
    assertThat(joinedClub.getMembers()).contains(newMember);
    
    Team joinedTeam = teamService.getTeamDetail(team.getId(), newMember.getId());
    assertThat(joinedTeam.getMembers()).contains(newMember);
}
```

**작업량**: 시나리오당 1-2시간 (2-3개 핵심 시나리오 = 4-6시간)  
**효과**: 실제 사용자 플로우 보장, 복잡한 상호작용 검증, 회귀 방지

---

### 4. S3Service 테스트 개선

**현재 문제**: ImageControllerTest에서 Mock만 사용, 실제 S3Service 로직 미검증

**해결 방안**: Humble Object Pattern 적용 (Naver D2 아티클)

#### 방법 1: 실제 S3 테스트 (TestContainers LocalStack)
```java
@SpringBootTest
@Testcontainers
class S3ServiceIntegrationTest {

    @Container
    static LocalStackContainer localstack = new LocalStackContainer(DockerImageName.parse("localstack/localstack:latest"))
        .withServices(LocalStackContainer.Service.S3);

    @Autowired
    private S3Service s3Service;

    @Test
    @DisplayName("이미지 업로드 후 URL 반환")
    void uploadImage_ReturnsValidUrl() throws IOException {
        // Given
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.jpg",
            "image/jpeg",
            "test image content".getBytes()
        );

        // When
        String url = s3Service.uploadImage(file, "test");

        // Then
        assertThat(url).isNotEmpty();
        assertThat(url).contains("test/");
        assertThat(url).endsWith(".jpg");
    }

    @Test
    @DisplayName("잘못된 파일 확장자는 예외 발생")
    void uploadImage_InvalidExtension_ThrowsException() {
        MockMultipartFile file = new MockMultipartFile(
            "file", "test", "text/plain", "content".getBytes()
        );

        assertThatThrownBy(() -> s3Service.uploadImage(file, "test"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("잘못된 형식");
    }
}
```

#### 방법 2: 단위 테스트 (AmazonS3Client Mock)
```java
@ExtendWith(MockitoExtension.class)
class S3ServiceUnitTest {

    @Mock
    private AmazonS3Client amazonS3Client;

    @InjectMocks
    private S3Service s3Service;

    @Test
    @DisplayName("파일명 생성 시 UUID 포함")
    void createFileName_ContainsUUID() {
        // Given
        MockMultipartFile file = new MockMultipartFile(
            "file", "original.jpg", "image/jpeg", "content".getBytes()
        );

        // When
        String url = s3Service.uploadImage(file, "profile");

        // Then
        assertThat(url).matches(".*profile/[a-f0-9\\-]+\\.jpg");
        verify(amazonS3Client).putObject(any(PutObjectRequest.class));
    }

    @Test
    @DisplayName("파일 삭제 시 올바른 키 전달")
    void deleteImage_CallsWithCorrectKey() {
        // Given
        String fileUrl = "https://cdn.example.com/profile/uuid.jpg";

        // When
        s3Service.deleteImage(fileUrl);

        // Then
        verify(amazonS3Client).deleteObject(
            argThat(req -> req.getKey().equals("profile/uuid.jpg"))
        );
    }
}
```

**작업량**: 2-3시간  
**효과**: 파일 업로드 로직 안정성, 확장자 검증, URL 생성 정확성

---

### 5. CommonRespDTO 테스트 (빠른 승리)

**왜 필요?**: 모든 API 응답의 표준 형식

```java
class CommonRespDTOTest {

    @Test
    @DisplayName("success() 메서드는 success=true 반환")
    void success_ReturnsSuccessTrue() {
        // When
        CommonRespDTO<String> response = CommonRespDTO.success("OK", "데이터");

        // Then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo("OK");
        assertThat(response.getData()).isEqualTo("데이터");
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    @DisplayName("error() 메서드는 success=false와 errorCode 반환")
    void error_ReturnsSuccessFalseWithErrorCode() {
        // When
        CommonRespDTO<?> response = CommonRespDTO.error("에러 발생", "USER_NOT_FOUND");

        // Then
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).isEqualTo("에러 발생");
        assertThat(response.getErrorCode()).isEqualTo("USER_NOT_FOUND");
        assertThat(response.getData()).isNull();
    }

    @Test
    @DisplayName("JSON 직렬화 시 필드명 일관성")
    void jsonSerialization_FieldNamesConsistent() throws Exception {
        // Given
        ObjectMapper mapper = new ObjectMapper();
        CommonRespDTO<Integer> response = CommonRespDTO.success("성공", 123);

        // When
        String json = mapper.writeValueAsString(response);

        // Then
        assertThat(json).contains("\"success\":true");
        assertThat(json).contains("\"message\":\"성공\"");
        assertThat(json).contains("\"data\":123");
    }
}
```

**작업량**: 30분  
**효과**: API 응답 형식 보장, 클라이언트 파싱 안정성

---

## 🔧 레이어별 테스트 전략 정리

### Repository 계층 (현재 잘 되어 있음)
- ✅ `@DataJpaTest` + H2 조합
- ✅ Soft delete, 페이징, 정렬 검증
- 추가 제안: 복잡한 네이티브 쿼리 경계값 테스트

### Service 계층 (현재 잘 되어 있음)
- ✅ Mockito로 의존성 격리
- ✅ Given-When-Then 구조
- 추가 제안: 예외 케이스 보강 (null, empty, 경계값)

### Controller 계층 (개선 필요)
- ⚠️ 21개 중 1개만 테스트
- 제안: `@SpringBootTest` + MockMvc
- 외부 의존(S3, Kakao)만 Mock, 나머지는 실제 Bean

### 통합 시나리오 (확장 필요)
- ✅ AuthIntegrationTest 존재
- 제안: Poll, Club, Team 핵심 여정 추가

---

## 📅 실행 계획 (우선순위별)

### Phase 1: Quick Wins (1-2일, 8-10시간)
1. **GlobalExceptionHandler 테스트** (2시간)
   - 14개 예외 전부 검증
   - 즉시 높은 가치 제공
   
2. **CommonRespDTO 테스트** (30분)
   - 빠르고 확실한 커버리지
   
3. **S3Service 단위 테스트** (2시간)
   - Mock 기반, 빠르게 작성 가능

### Phase 2: High Value Controllers (3-4일, 12-16시간)
4. **AuthController 통합 테스트** (4시간)
5. **ClubController 통합 테스트** (4시간)
6. **TeamController 통합 테스트** (4시간)

### Phase 3: Integration Scenarios (3-5일, 12-20시간)
7. **Poll 생명주기 시나리오** (6시간)
8. **동아리 가입 여정 시나리오** (6시간)
9. **홍보글 작성-신고 시나리오** (4시간)

### Phase 4 (Optional): 나머지 Controllers (5-7일)
10. PromoController, NoticeController 등

---

## 📐 테스트 작성 가이드라인 (Best Practices)

### 1. FIRST 원칙 준수
- **F**ast: 단위 테스트는 밀리초 단위, 통합은 초 단위
- **I**ndependent: `@Transactional` 또는 `@AfterEach` cleanup
- **R**epeatable: 실행 순서 무관하게 항상 같은 결과
- **S**elf-validating: 수동 확인 없이 pass/fail 자동 판정
- **T**imely: 기능 개발 직후 또는 버그 발견 즉시

### 2. Given-When-Then 구조 유지
```java
@Test
@DisplayName("명확한 테스트 의도 표현")
void methodName_Condition_ExpectedResult() {
    // Given: 테스트 데이터 준비
    
    // When: 실제 동작 수행
    
    // Then: 결과 검증
}
```

### 3. 테스트 데이터 관리
- ✅ 현재 `testutil/TestDataFactory` 활용 중
- 제안: JSON 기반 테스트 데이터 (Toss 전략)
```java
// resources/test-data/poll-scenarios.json
{
  "validPoll": {
    "title": "정기공연 곡 선정",
    "endDatetime": "2025-12-31T23:59:59"
  },
  "expiredPoll": {
    "title": "만료된 투표",
    "endDatetime": "2020-01-01T00:00:00"
  }
}
```

### 4. Mock 사용 최소화 (Toss 원칙)
- 외부 시스템(S3, Kakao, Elasticsearch)만 Mock
- 내부 Service/Repository는 실제 Bean 사용
- Mock이 필요하면 인터페이스 도입 고려 (Humble Object)

### 5. 테스트 실패 메시지 명확화
```java
assertThat(result.getStatus())
    .as("투표 마감 시간 이전에는 ACTIVE 상태여야 함")
    .isEqualTo(PollStatus.ACTIVE);
```

---

## 🎓 학습한 전략 요약

### Toss Tech (핵심)
- **20/80 법칙**: 중요한 20%를 철저히
- **통합 테스트 중심**: 단위보다 통합이 신뢰도 높음
- **Mock 최소화**: 실제 객체 사용 권장
- **JSON 테스트 데이터**: 재사용성과 가독성

### Naver D2 (설계)
- **Humble Object Pattern**: 테스트 어려운 부분 격리
- **의존성 주입**: 테스트 가능성 = SOLID 원칙
- **Mock 계층**: Dummy → Stub → Spy → Mock (낮을수록 좋음)

### Best Practices (실무)
- **FIRST 원칙**: 빠르고, 독립적이고, 반복 가능한 테스트
- **BDD 구조**: Given-When-Then으로 의도 명확화
- **계층별 전략**: @DataJpaTest, @WebMvcTest, @SpringBootTest 적재적소
- **테스트 격리**: @Transactional 롤백 또는 cleanup

---

## 🚀 최종 권고사항

### 즉시 시작 (이번 주)
1. **GlobalExceptionHandler 테스트** - 가장 높은 ROI
2. **CommonRespDTO 테스트** - 30분 투자로 큰 안심

### 다음 스프린트
3. **AuthController 통합 테스트** - 인증은 모든 API의 기반
4. **Poll 생명주기 시나리오** - 핵심 기능 보장

### 지속적 개선
- 새 기능 개발 시 Controller 테스트 필수
- 버그 발견 시 회귀 테스트 추가
- 분기마다 핵심 시나리오 추가

### 하지 말아야 할 것
- ❌ 모든 Controller 동시에 테스트 (과부하)
- ❌ 100% 커버리지 목표 (diminishing returns)
- ❌ 과도한 Mock 사용 (유지보수 비용)
- ❌ TDD 전환 시도 (이미 완성된 프로젝트)

---

## 📈 예상 효과

### 정량적 효과
- Jacoco 커버리지: 현재 70% → 목표 75-80%
- Controller 테스트: 1/21 → 5-7/21
- 통합 시나리오: 1개 → 3-4개
- 예외 처리 테스트: 0 → 14개

### 정성적 효과
- ✅ API 응답 규약 보장 (클라이언트 안정성)
- ✅ 회귀 버그 조기 발견
- ✅ 리팩토링 신뢰도 확보
- ✅ 신규 개발자 온보딩 가이드 역할

---

## 📚 참고 자료

1. [Toss - 실용적인 테스트 전략](https://toss.tech/article/test-strategy-server)
2. [Naver D2 - Humble Object Pattern](https://d2.naver.com/helloworld/9921217)
3. [MangKyu - 단위 테스트 작성 가이드](https://mangkyu.tistory.com/143)
4. [sjh9708 - 좋은 테스트 코드](https://sjh9708.tistory.com/238)
5. [sjh9708 - 계층별 테스트 전략](https://sjh9708.tistory.com/240)

---

**작성일**: 2025-01-XX  
**작성자**: GitHub Copilot (AI Coding Agent)  
**검토 필요**: Phase 1-2 계획 승인 후 실행
