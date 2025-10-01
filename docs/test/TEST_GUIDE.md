# Jandi Band Backend 테스트 가이드

## 1. 테스트 현황 (2025-10-01 기준)
- 총 355개 테스트 모두 성공 (실패 0)
- 커버리지 축 중심
  - **도메인 서비스 단위 테스트**: Auth, Invite, Poll, Promo, Team, User
  - **핵심 JPA Repository 테스트**: soft delete, 익명화, 페이징/정렬 쿼리 검증
  - **통합 시나리오**: `AuthIntegrationTest` 한 편으로 회원 인증 플로우 검증(MockMvc + 실제 빈 구성)
- 제거된 항목: 과도한 성능/보안 시뮬레이션(`PerformanceTest`, `SecurityIntegrationTest`), 컨트롤러 전 범위 통합(`AuthControllerIntegrationTest`), 복잡한 파일 업로드 통합(`FileUploadIntegrationTest`), 공용 MockMvc 베이스 클래스(`BaseControllerTest`)
- 파일 업로드 플로우는 `ImageControllerTest` 단위 테스트로 대체하여 권한/의존성 체크를 가볍게 유지
제
## 2. 디렉터리 구조
```
src/test/java/com/jandi/band_backend/
├── JandiBandBackendApplicationTests.java   # 기본 컨텍스트 로드
├── auth/service/                          # AuthService 단위 및 탈퇴 플로우 테스트
├── club/service/                          # ClubService 단위 테스트
├── config/                                # IntegrationTest 등 공용 설정
├── image/                                 # ImageController 단위 테스트
├── integration/                           # AuthIntegrationTest(MockMvc 통합)
├── invite/service/                        # 초대/참여 서비스 단위 테스트
├── poll/controller|repository|service/    # Poll API + 도메인 단위/레포지토리 테스트
├── promo/service/                         # PromoService 단위 테스트
├── repository/                            # 공통 Repository(@DataJpaTest) 스펙
├── team/service/                          # TeamService 단위 테스트
├── testutil/                              # TestDataFactory 등 공용 픽스처
└── user/repository|service/               # 사용자 도메인 단위/레포지토리 테스트
```

## 3. 테스트 유형과 역할
- **서비스 단위 테스트**: Mockito 기반으로 예외/상태 전환을 정밀 검증 (`AuthServiceTest`, `AuthCancelServiceTest`, `Invite`/`Poll`/`Promo`/`Team`/`User` 서비스 테스트)
- **Repository 테스트**: `@DataJpaTest` + H2 조합으로 soft delete, 익명화, 페이징, 네이티브 쿼리를 확인. 미세한 시간 차이는 `isEqualToIgnoringNanos`로 허용
- **Controller/MockMvc 테스트**: `PollControllerTest` 등은 `@SpringBootTest` 환경에서 MockMvc와 JwtTokenProvider mocking을 조합해 HTTP 계약을 검증
- **통합 테스트**: `AuthIntegrationTest`는 실제 Bean 구성을 로드해 JWT 발급/회원가입/탈퇴 플로우를 끝단까지 재현. 테스트 프로필(`test`)과 `@TestPropertySource` 덕분에 인메모리 데이터베이스(H2)로 동작하며, 외부 의존(KakaoUserService)만 `@MockBean`으로 치환

## 4. 실행 방법
```bash
# 전체 테스트
./gradlew test

# Poll 서비스 계층 전용
./gradlew test --tests "com.jandi.band_backend.poll.service.*"

# Auth 통합 테스트만
./gradlew test --tests "*integration.AuthIntegrationTest"

# 이미지 업로드 권한 체크 단위 테스트
./gradlew test --tests "*ImageControllerTest"

# 회원 탈퇴 복합 시나리오
./gradlew test --tests "*AuthCancelServiceTest"
```

## 5. 운영 가이드
1. 기능 개발 시 **서비스 단위 테스트 + 필요 시 Repository 테스트**를 우선 작성
2. 인증/인가 흐름에 영향이 있으면 `AuthIntegrationTest` 실행으로 최종 검증
3. `./gradlew test` 전체 실행 후 JaCoCo 리포트를 확인 (`build/reports/jacoco/test/html/index.html`)
4. 현재 스위트는 핵심 로직/쿼리를 빠르게 검증하도록 설계되어 있으므로, 시스템 전반(E2E, 부하, 보안 정책)을 다루려면 별도 시나리오를 추가하세요

필요에 따라 해당 구조를 기반으로 테스트를 보강하거나 새로운 통합 테스트를 추가하면 됩니다.
