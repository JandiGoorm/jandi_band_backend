# Jandi Band Backend (RhythMeet) - AI Coding Guidelines

## 프로젝트 개요
대학 밴드 동아리 매칭/관리 플랫폼 백엔드. Spring Boot 3.4.5, Java 21 기반.

## 아키텍처
```
src/main/java/com/jandi/band_backend/
├── auth/          # 카카오 OAuth + JWT 인증
├── club/          # 동아리 (핵심 도메인)
├── team/          # 팀 (동아리 내 소그룹)
├── poll/          # 팀 내 곡 투표
├── promo/         # 홍보 게시판
├── invite/        # 초대 링크 (Redis 기반)
├── user/          # 사용자 관리
├── univ/          # 대학/지역 정보
├── global/        # 공통 예외, DTO, 유틸
├── config/        # 보안, Redis, S3, Swagger 설정
└── security/      # JWT 인증 필터, UserDetails
```

### 핵심 도메인 관계
- `Users` → `ClubMember` → `Club` → `Team` → `TeamMember`
- 동아리(`Club`)는 대학 소속 또는 연합(university=null) 가능
- 팀(`Team`)은 반드시 동아리 내에 존재

## 코드 패턴

### 도메인별 레이어 구조
각 도메인(`club/`, `team/` 등)은 동일한 구조 준수:
- `controller/` - REST API, `@AuthenticationPrincipal CustomUserDetails` 사용
- `service/` - 비즈니스 로직
- `repository/` - JPA Repository
- `entity/` - JPA 엔티티 (@PrePersist/@PreUpdate로 타임스탬프)
- `dto/` - 요청/응답 DTO

### 공통 응답 형식
모든 API는 `CommonRespDTO<T>` 래퍼 사용:
```java
// 성공
return CommonRespDTO.success("메시지", data);
return CommonRespDTO.success("메시지"); // 데이터 없음

// 실패는 GlobalExceptionHandler에서 자동 처리
throw new ClubNotFoundException("동아리를 찾을 수 없습니다.");
```

### 예외 처리
- 도메인별 커스텀 예외는 `global/exception/`에 정의
- `GlobalExceptionHandler`가 일괄 처리하여 `CommonRespDTO.error()` 반환
- 예외 네이밍: `{리소스}NotFoundException`, `Invalid{동작}Exception`

### 권한 검증 유틸
```java
// global/util/PermissionValidationUtil.java 활용
permissionValidationUtil.validateClubMemberAccess(clubId, userId, "에러메시지");
permissionValidationUtil.validateClubRepresentativeAccess(clubId, userId, "에러메시지");
permissionValidationUtil.validateTeamMemberAccess(teamId, userId, "에러메시지");
// ADMIN 역할은 자동 우회
```

### Soft Delete 패턴
엔티티에 `deletedAt` 필드 사용, 쿼리시 `deletedAtIsNull` 조건 필수:
```java
findByClubIdAndUserIdAndDeletedAtIsNull(clubId, userId)
```

## 개발 명령어
```bash
./gradlew test                    # 전체 테스트 (355개)
./gradlew test --tests "*ServiceTest"  # 서비스 단위 테스트
./gradlew jacocoTestReport        # 커버리지 리포트 (build/reports/jacoco/)
./gradlew bootRun                 # 로컬 실행 (application.properties 필요)
```

## 테스트 작성
- **단위 테스트**: `@ExtendWith(MockitoExtension.class)` + Mockito
- **Repository 테스트**: `@DataJpaTest` + H2 (test profile)
- **통합 테스트**: `@SpringBootTest` + MockMvc
- 테스트 데이터는 `testutil/TestDataFactory.java` 활용

## 인증 플로우
1. 카카오 OAuth → `GET /api/auth/login?code={code}`
2. JWT AccessToken(헤더) + RefreshToken(쿠키) 발급
3. 이후 요청: `Authorization: Bearer {accessToken}`
4. 컨트롤러에서 `@AuthenticationPrincipal CustomUserDetails userDetails` 주입

## 외부 의존성
- **MySQL**: 운영 DB (`spring.jpa.hibernate.ddl-auto=validate`)
- **Redis**: 토큰 블랙리스트, 초대 코드 저장
- **AWS S3**: 이미지 업로드 (`cloud.aws.s3.*` 설정)
- **Prometheus/Grafana**: 모니터링 (`/actuator/prometheus`)

## 설정 파일
- `application.properties.example` 복사 후 설정값 입력
- 테스트: `application-test.properties` (H2 인메모리 자동 사용)

## API 문서
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- 상세 API 명세: `docs/` 디렉토리 마크다운 참조
