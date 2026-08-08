# 물리 FK(Foreign Key) 조사 결과 및 개선 TODO

> 조사일: 2026-08-08
> 대상: `master` @ `fb3ba8e`
> 방법: 엔티티 매핑 정적 분석 · `ddl-auto` 설정 git 이력 추적 · 테스트 실행을 통한 DDL 실측 · 탈퇴/삭제 로직 및 테스트 코드 검토
> 비고: 두 차례 독립 조사(Codex / Claude) 결과를 대조·교차 검증하여 통합한 문서

---

## 1. 결론

**이 프로젝트의 JPA 엔티티는 물리 FK가 생성되는 기본 설정을 그대로 사용한다.**

| 항목 | 수치 |
|:--|--:|
| `@ManyToOne` | 45 |
| `@JoinColumn` | 45 |
| `@OneToOne` / `@ManyToMany` / `@JoinTable` | 0 |
| `ConstraintMode.NO_CONSTRAINT` 등 FK 생성 억제 설정 | **0** |
| 테스트(H2) 스키마에 실제로 생성된 물리 FK | **45** |
| `ON DELETE CASCADE` 지정 | 0 (기본 `RESTRICT`) |

### 확정된 사실과 미확정 사항 구분

- **확정**: 엔티티 매핑상 FK 생성 억제 설정이 전무하며, 테스트 환경에서는 물리 FK 45개가 실제로 생성된다.
- **미확정(확인 필요)**: 운영 DB에 실제로 FK가 존재하는지는 **운영 DB 메타데이터를 조회해야 확정된다.**
  운영 설정이 `ddl-auto=validate`이고 `validate`는 FK를 생성하지도 검증하지도 않기 때문이다.
  다만 아래 2-2 ~ 2-4의 정황 증거는 **존재 쪽을 강하게 가리킨다.**

### 중요한 관점

**물리 FK의 사용 자체를 결함으로 단정할 수 없다.**
단일 Spring Boot 애플리케이션 + 단일 MySQL 구조에서는 물리 FK가 데이터 무결성 보장에 유용하다.
실제로 아래 3-3의 버그도 FK가 없었다면 **조용히 고아 행을 만들며 지나갔을** 문제다.

이번 조사에서 드러난 실질적 문제는 FK의 존재가 아니라 다음 두 가지다.

1. **스키마가 코드로 관리되지 않는다** — 마이그레이션 부재로 FK도, 필수 초기 데이터도 재현 불가
2. **사용자 참조 데이터의 생명주기 정책이 완결되지 않았다** — 보존/익명화/삭제 기준 누락

논리 FK 전환 여부는 독립 배포, DB 분리, 샤딩 등 **실제 아키텍처 요구사항을 기준으로** 결정해야 하며,
위 1·2번을 해결한 뒤에 논의해도 늦지 않다.

---

## 2. 근거

### 2-1. 생성 DDL 실측 (재현 절차 포함)

`src/test/resources/application-test.properties`에 아래 2줄을 **임시로** 추가하고 `@DataJpaTest` 계열 테스트를 1개 실행하면
Hibernate가 생성하는 DDL 전문을 파일로 받을 수 있다. (확인 후 반드시 원복할 것 — 이 설정이 있으면 H2에 테이블이
생성되지 않아 테스트가 전부 실패한다)

```properties
spring.jpa.properties.jakarta.persistence.schema-generation.scripts.action=create
spring.jpa.properties.jakarta.persistence.schema-generation.scripts.create-target=<출력경로>/ddl-export.sql
```

```powershell
.\gradlew.bat test --tests "com.jandi.band_backend.repository.ClubRepositoryTest" --no-daemon
```

**실측 결과 (2026-08-08 확인)**

| 항목 | 결과 |
|:--|--:|
| `foreign key` 구문 | **45** |
| `on delete` 구문 (CASCADE / SET NULL 등) | **0** |
| `references users` (users 참조 FK 컬럼) | **21** |

생성 DDL 실제 예시:

```sql
alter table if exists club       add constraint FKmf82w0dtc98509noons2e6hww foreign key (university_id)    references university;
alter table if exists club_event add constraint FKi2h9w0o5q2wxx1a6h1rgnc4bf foreign key (club_id)          references club;
alter table if exists club_event add constraint FKf7n5ed0i71s14pm1o3us673iw foreign key (creator_user_id)  references users;
```

`ON DELETE CASCADE`가 없으므로 참조 중인 부모 행 삭제는 기본 참조 무결성 정책(`RESTRICT`)에 의해 제한된다.
이것이 3-3에서 하드 삭제가 실패하는 직접적인 원인이다.

### 2-2. 스키마 생성 시점 추적 (git 이력)

```
69ab6be (2025-05-15) "엔티티 추가"   +spring.jpa.hibernate.ddl-auto=update    ← 이 구간에 스키마 생성
d679520              "..."           -spring.jpa.hibernate.ddl-auto=update
493cf9d (2025-05-18) "feature/auth"  +spring.jpa.hibernate.ddl-auto=validate  ← 이후 전환
```

`update` 구간 동안 Hibernate가 모든 `@JoinColumn`에 대해
`alter table ... add constraint FKxxx foreign key (...) references ...` 를 자동 생성했을 것으로 보인다.
이후 `validate`로 전환해도 기존 FK는 제거되지 않으므로 그대로 남는다.

- 운영: `src/main/resources/application.properties.example:8` → `validate`
- 테스트: `src/test/resources/application-test.properties:1` → `create-drop`

### 2-3. 문서에 남아 있는 DDL

`docs/notice-notice.md:487`

```sql
CREATE TABLE notice (
    ...
    FOREIGN KEY (creator_user_id) REFERENCES users(user_id)
);
```

### 2-4. 테스트 코드가 남긴 증거 — 그리고 서로 모순되는 두 가지 대응

동일한 `anonymizeByUserId` 문제를 두 테스트가 **정반대 방식으로** 처리하고 있다.

**(a) 센티널 사용자를 직접 삽입하고 성공을 검증** — `ClubEventRepositoryTest.java:211-246`

```java
@DisplayName("생성자 익명화 성공")
void anonymizeByUserId() {
    // Create dummy user with ID -1 for anonymization using native SQL
    entityManager.getEntityManager().createNativeQuery(
        "INSERT INTO users (user_id, kakao_oauth_id, nickname, admin_role, is_registered, created_at, updated_at) " +
        "VALUES (-1, 'dummy123', 'dummy', 'USER', false, NOW(), NOW())")
        .executeUpdate();
    ...
    assertThat(updated).isEqualTo(1);
}
```

**(b) FK 위반 예외가 발생하는 것을 "정상"으로 고정** — `ClubGalPhotoRepositoryTest.java:249-259`

```java
@DisplayName("사용자 ID로 갤러리 사진 업로더 익명화 - 제약 조건으로 인한 예외 발생")
void anonymizeByUserId() {
    // when & then - H2에서는 -1 사용자가 없어서 외래키 제약 조건 위반 발생
    // 실제 운영 환경에서는 익명 사용자(ID: -1)가 미리 생성되어 있음
    assertThatThrownBy(() -> clubGalPhotoRepository.anonymizeByUserId(userId))
            .isInstanceOf(Exception.class);
}
```

주석의 **"실제 운영 환경에서는 익명 사용자(ID: -1)가 미리 생성되어 있음"** 은
운영 DB에 물리 FK가 존재한다는 정황 증거인 동시에, 3-2 문제의 근원이다.
유사 사례: `PollRepositoryTest.java:212-227` (`- 예외 확인`)

---

## 3. 발견된 구체적 위험

### 3-1. 회원 탈퇴 로직이 FK를 우회하느라 비대해짐

`src/main/java/com/jandi/band_backend/auth/service/AuthService.java:150-210`

탈퇴 1건 처리에 3개 그룹, 18개 테이블 조작이 필요하다.

```java
processGroup1SoftDelete(userId, deletedAt);  // 4개 테이블 deletedAt 세팅
processGroup2Anonymize(userId);              // 11개 테이블 → user_id = -1 (네이티브 UPDATE)
processGroup3HardDelete(userId);             // 3개 테이블 물리 삭제
```

그룹 2는 전부 아래 형태의 네이티브 쿼리이며, FK를 만족시키기 위해
참조를 "탈퇴 사용자" 센티널 로우(`user_id = -1`)로 떠넘기는 패턴이다.

```java
@Query(value = "UPDATE promo SET creator_user_id = -1 WHERE creator_user_id = :userId",
       nativeQuery = true)
int anonymizeByCreatorId(@Param("userId") Integer userId);
```

전체 위치는 [부록 B](#부록-b--익명화-네이티브-쿼리-11곳) 참조.

### 3-2. 센티널 로우(`users.user_id = -1`)에 대한 관리되지 않는 운영 의존성

- 레포에 `.sql` 파일 **0개**. Flyway / Liquibase / `schema.sql` **모두 미도입**.
- `user_id = -1` 행은 운영 DB에 **수동 INSERT된 상태로만 존재**한다.
- 생성 경로도, 삭제 방지 장치도 없다.

다음 상황에서 **회원 탈퇴 트랜잭션이 FK 위반으로 실패**한다.

- 운영/개발 DB에 `users.user_id = -1` 행이 없는 경우
  → 최근 추가된 개발 서버(`ba30d3c`), 신규 로컬, DB 재구축 환경이 여기 해당
- 해당 행이 실수로 삭제되거나 변경된 경우
- 신규 사용자 참조 테이블을 추가하면서 익명화 로직을 누락한 경우

**이번 조사에서 가장 시급한 항목.**

### 3-3. 사용자 하드 삭제 시 처리되지 않는 참조 (실재 가능성 높은 버그)

`src/main/java/com/jandi/band_backend/user/service/UserHardDeleteScheduler.java:36-84`

매일 새벽 3시(`cron = "0 0 3 * * ?"`)에 탈퇴 후 7일 지난 유저를 물리 삭제한다.
`users`를 참조하는 테이블은 **20개**(FK 컬럼 기준 21개, `club_pending`만 2개)인데
스케줄러는 4개만 정리하고 `users`를 지운다.

```java
userTimetableRepository.deleteAllByUser(user);   // user_timetable
userPhotoRepository.delete(userPhoto);           // user_photo
clubMemberRepository.deleteAllByUser(user);      // club_member
teamMemberRepository.deleteAllByUser(user);      // team_member
userRepository.delete(user);                     // ← FK 위반 지점
```

탈퇴 시 익명화(3-1)로 대부분은 비껴가지만, **아래 3개 참조가 탈퇴·하드삭제 어느 쪽에서도 처리되지 않는다.**

| FK 참조 | 상태 |
|:--|:--|
| `notice.creator_user_id -> users.user_id` (NOT NULL) | 미처리 |
| `club_pending.user_id -> users.user_id` (NOT NULL) | 미처리 |
| `club_pending.processed_by -> users.user_id` | 미처리 |

특히 `club_pending`은 `ClubPendingRepository`에 삭제 쿼리가 아예 없고
`bulkExpirePendingApplications`가 `status`만 `EXPIRED`로 바꾼다
(`clubpending/repository/ClubPendingRepository.java:28-30`). 즉 행이 영원히 남아 해당 유저를 가리킨다.

> **결과: 동아리에 한 번이라도 가입 신청했거나 공지를 작성한 유저는 하드 삭제가 FK 위반으로 실패한다.**
> 스케줄러는 `@Transactional`이라 해당 유저 처리가 롤백되고, 매일 같은 실패를 반복하게 된다.

**단, 물리 FK만 제거하면 삭제는 성공하지만 존재하지 않는 사용자 ID를 가리키는 고아 데이터가 남는다.**
핵심 문제는 FK가 아니라 **연관 데이터별 보존/익명화/삭제 정책이 완결되지 않은 것**이다.

### 3-4. 스키마 변경 이력 부재

운영이 `ddl-auto=validate`인데 다음이 모두 없다.

- Flyway 마이그레이션
- Liquibase 변경 이력
- 버전 관리되는 `schema.sql` 또는 별도 DDL

이 상태에서는 엔티티와 운영 DB 스키마의 생성 과정, FK 유무, 익명 사용자 생성 여부를
**저장소만으로 재현할 수 없다.**

### 3-5. 테스트가 회귀를 잡지 못함

2-4의 (b) 유형 테스트는 `anonymizeByUserId`가 **성공해야 정상**인데 "예외가 나면 통과"로 작성되어 있다.
운영에서 이 쿼리가 깨져도 전체 테스트(`@Test` 466개)는 전부 통과한다.
(참고: `.github/copilot-instructions.md:70`에 적힌 "355개"는 현재 기준으로 낡은 수치다 — 실제 466개)
또한 (a)와 (b)가 같은 문제를 다르게 다루고 있어 기준 자체가 없는 상태다.

---

## 4. 먼저 확인해야 할 것 (운영 / 개발 DB)

```sql
-- (1) 실제 물리 FK 목록. 45개 내외로 나올 것으로 예상
SELECT TABLE_NAME,
       CONSTRAINT_NAME,
       COLUMN_NAME,
       REFERENCED_TABLE_NAME,
       REFERENCED_COLUMN_NAME
FROM information_schema.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA = DATABASE()
  AND REFERENCED_TABLE_NAME IS NOT NULL
ORDER BY TABLE_NAME, CONSTRAINT_NAME;

-- (2) 센티널 유저 존재 여부. 개발 서버에서 비어 있으면 그 환경 탈퇴 기능은 이미 죽어 있음
SELECT * FROM users WHERE user_id = -1;

-- (3) 3-3 이슈 실재 여부. 결과가 1건 이상이면 스케줄러가 이미 실패 중
SELECT u.user_id,
       (SELECT COUNT(*) FROM club_pending cp WHERE cp.user_id = u.user_id)     AS pending_rows,
       (SELECT COUNT(*) FROM club_pending cp WHERE cp.processed_by = u.user_id) AS processed_rows,
       (SELECT COUNT(*) FROM notice n WHERE n.creator_user_id = u.user_id)      AS notice_rows
FROM users u
WHERE u.deleted_at IS NOT NULL
HAVING pending_rows + processed_rows + notice_rows > 0;
```

---

## 5. TODO

### P0 — 스키마를 코드로 되돌리기 (FK 유지/제거와 무관하게 선행)

- [ ] 4번 확인 쿼리 3종을 운영 / 개발 DB에서 각각 실행하고 결과를 이 문서에 기록
- [ ] 운영 DB에서 `mysqldump --no-data`로 현재 스키마 덤프 확보
- [ ] Flyway 도입 (`build.gradle` + `src/main/resources/db/migration/`)
- [ ] `V1__baseline.sql` — 덤프한 현재 스키마 그대로 (FK 포함)
- [ ] `V2__seed_anonymous_user.sql` — `users(user_id = -1)` 센티널 로우 INSERT
      (`INSERT ... ON DUPLICATE KEY UPDATE`로 멱등하게)
- [ ] 기존 환경은 `flyway.baselineOnMigrate=true`로 baseline 처리
- [ ] 애플리케이션 시작 시 필요한 스키마와 기준 데이터 존재 여부를 검증
- [ ] 신규 환경에서 `./gradlew bootRun` → 탈퇴 API까지 정상 동작하는지 확인

### P1 — 데이터 생명주기 정리 및 실재 버그 수정

- [ ] **사용자 참조 20개 테이블 전체에 대해** 탈퇴 시 처리 방식을
      `삭제` / `소프트 삭제` / `익명화` / `보존` 중 하나로 정의한 표를 작성 (정책 문서화)
- [ ] `notice.creator_user_id` 처리 정책 구현
- [ ] `club_pending.user_id` 처리 정책 구현
- [ ] `club_pending.processed_by` 처리 정책 구현
- [ ] `UserHardDeleteScheduler`에 위 누락 참조 정리 로직 추가
- [ ] 탈퇴 사용자가 `notice` / `club_pending`에 참조된 상태에서 하드 삭제가 실패하는지
      **운영과 동일한 MySQL 환경**에서 재현 (H2는 동작이 다를 수 있음)
- [ ] 매직 넘버 `-1`을 상수화 (`AnonymousUser.ID` 등) 하고 DB 초기 데이터로 관리
- [ ] 신규 사용자 FK가 추가될 때 탈퇴/하드삭제 정책 누락을 검출하는 테스트 추가
      (예: `users`를 참조하는 테이블 목록과 정책 표를 대조하는 메타 테스트)

### P1 — 테스트 정합성 회복

- [ ] `ClubGalPhotoRepositoryTest:249`, `PollRepositoryTest:212` —
      "예외가 나면 통과" 방식을 `ClubEventRepositoryTest:211` 방식(센티널 fixture + 성공 검증)으로 통일
- [ ] 센티널 사용자 fixture를 공통화 (`TestDataFactory` 또는 테스트 전용 Flyway 마이그레이션)
      — 현재 `ClubEventRepositoryTest`에만 하드코딩된 native INSERT가 있음

### P2 — 물리 FK 유지 여부 결정 (P0 완료 후 판단)

- [ ] 현재 시스템이 단일 DB 경계를 계속 유지하는지 확인
- [ ] 서비스별 독립 배포, DB 분리, 샤딩 계획이 있는지 확인
- [ ] 위 요구사항을 기준으로 물리 FK 유지 / 논리 FK 전환을 결정하고 **ADR로 기록**

**(A) 논리 FK로 전환하는 경우**

- [ ] 전환 전 기존 고아 데이터를 먼저 검사하고 정리
- [ ] 45개 `@JoinColumn`에 `foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)` 추가
- [ ] `V3__drop_foreign_keys.sql` — 4-(1) 결과 기반 `ALTER TABLE ... DROP FOREIGN KEY`
- [ ] FK 컬럼 인덱스가 남는지 확인하고 명시적으로 유지 (조회 성능)
- [ ] 애플리케이션 수준 무결성 검증 및 고아 행 탐지 배치 마련
- [ ] 3-1의 `= -1` 익명화 패턴을 `= NULL` 또는 소프트 삭제로 단순화 가능한지 재검토

**(B) 물리 FK를 유지하는 경우**

- [ ] P0 + P1만 처리하고 종료
- [ ] `@ManyToOne` 기본 fetch가 EAGER인 지점 점검 (별개 이슈지만 함께 보면 좋음)

---

## 6. 두 조사 결과 대조 (참고)

| 항목 | Codex | Claude | 통합 판단 |
|:--|:--|:--|:--|
| FK 45개 / 억제 설정 0개 | O | O | **일치 — 확정** |
| 테스트 실행으로 DDL 실측 | O (45개 확인) | X → 재실측 O | Codex 주장을 재현 검증 완료 (45개 / `on delete` 0개 일치) |
| `ddl-auto` git 이력 추적 | X | O (`update`→`validate`) | Claude 근거 채택 |
| 운영 DB FK 존재 여부 | "미확정, 조회 필요" | "정황상 존재" | **Codex 표현 채택** — 정황은 강하나 쿼리로 확정 필요 |
| `-1` 센티널 미관리 문제 | O | O | **일치 — 최우선 과제** |
| `notice` / `club_pending` 누락 | O | O | **일치 — 실재 버그 가능성 높음** |
| 익명화 쿼리 11곳 위치 특정 | 일부 | O (파일·라인) | Claude 목록 채택 → 부록 B |
| 영역별 / 엔티티별 FK 집계 | O (영역별) | O (엔티티별) | 둘 다 채택 → 부록 A |
| 테스트의 FK 위반 대응 모순 | (a)만 언급 | (b)만 언급 | **통합 시 신규 발견** — 2-4 항목 |
| ADR 기록 / 인덱스 유지 | O | X | Codex 항목 채택 |
| `ON DELETE CASCADE` 부재 확인 | O | X | Codex 항목 채택 |

두 조사가 **독립적으로 동일한 3대 문제**(센티널 미관리 / `notice`·`club_pending` 누락 / 마이그레이션 부재)에
도달했다는 점에서 해당 항목들의 신뢰도는 높다.

---

## 부록 A — 물리 FK 전체 목록

### 영역별 집계

| 영역 | FK 수 |
|:--|--:|
| club | 8 |
| clubpending | 3 |
| notice | 1 |
| poll | 7 |
| promo | 16 |
| team | 6 |
| univ | 1 |
| user | 3 |
| **합계** | **45** |

### 엔티티별 상세

| 엔티티 | FK 컬럼 → 참조 |
|:--|:--|
| `Club` | `university_id` → `university` |
| `ClubEvent` | `club_id` → `club`, `creator_user_id` → `users` |
| `ClubGalPhoto` | `club_id` → `club`, `uploader_user_id` → `users` |
| `ClubMember` | `club_id` → `club`, `user_id` → `users` |
| `ClubPhoto` | `club_id` → `club` |
| `ClubPending` | `club_id` → `club`, `user_id` → `users`, `processed_by` → `users` |
| `Notice` | `creator_user_id` → `users` |
| `Poll` | `club_id` → `club`, `team_id` → `team`, `creator_user_id` → `users` |
| `PollSong` | `poll_id` → `poll`, `suggester_user_id` → `users` |
| `Vote` | `poll_song_id` → `poll_song`, `user_id` → `users` |
| `Promo` | `club_id` → `club`, `creator_user_id` → `users` |
| `PromoComment` | `promo_id` → `promo`, `creator_user_id` → `users` |
| `PromoCommentLike` | `promo_comment_id` → `promo_comment`, `user_id` → `users` |
| `PromoCommentReport` | `promo_comment_id` → `promo_comment`, `reporter_user_id` → `users`, `report_reason_id` → `report_reason` |
| `PromoLike` | `promo_id` → `promo`, `user_id` → `users` |
| `PromoPhoto` | `promo_id` → `promo`, `uploader_user_id` → `users` |
| `PromoReport` | `promo_id` → `promo`, `reporter_user_id` → `users`, `report_reason_id` → `report_reason` |
| `Team` | `club_id` → `club`, `creator_user_id` → `users` |
| `TeamEvent` | `team_id` → `team`, `creator_user_id` → `users` |
| `TeamMember` | `team_id` → `team`, `user_id` → `users` |
| `University` | `region_id` → `region` |
| `Users` | `university_id` → `university` |
| `UserPhoto` | `user_id` → `users` |
| `UserTimetable` | `user_id` → `users` |

---

## 부록 B — 익명화 네이티브 쿼리 11곳

`AuthService.processGroup2Anonymize`에서 호출. 전부 `SET ... = -1` 패턴.

| 파일 | 라인 | 대상 테이블 / 컬럼 |
|:--|--:|:--|
| `club/repository/ClubEventRepository.java` | 31 | `club_event.creator_user_id` |
| `club/repository/ClubGalPhotoRepository.java` | 32 | `club_gal_photo.uploader_user_id` |
| `poll/repository/PollRepository.java` | 26 | `poll.creator_user_id` |
| `poll/repository/PollSongRepository.java` | 18 | `poll_song.suggester_user_id` |
| `promo/repository/PromoRepository.java` | 101 | `promo.creator_user_id` |
| `promo/repository/PromoPhotoRepository.java` | 25 | `promo_photo.uploader_user_id` |
| `promo/repository/PromoCommentRepository.java` | 28 | `promo_comment.creator_user_id` |
| `promo/repository/PromoReportRepository.java` | 14 | `promo_report.reporter_user_id` |
| `promo/repository/PromoCommentReportRepository.java` | 14 | `promo_comment_report.reporter_user_id` |
| `team/repository/TeamRepository.java` | 23 | `team.creator_user_id` |
| `team/repository/TeamEventRepository.java` | 41 | `team_event.creator_user_id` |

---

## 부록 C — 참고 파일

- `src/main/resources/application.properties.example`
- `src/test/resources/application-test.properties`
- `src/main/java/com/jandi/band_backend/auth/service/AuthService.java`
- `src/main/java/com/jandi/band_backend/user/service/UserHardDeleteScheduler.java`
- `src/main/java/com/jandi/band_backend/notice/entity/Notice.java`
- `src/main/java/com/jandi/band_backend/clubpending/entity/ClubPending.java`
- `src/main/java/com/jandi/band_backend/clubpending/repository/ClubPendingRepository.java`
- `src/test/java/com/jandi/band_backend/repository/ClubEventRepositoryTest.java`
- `src/test/java/com/jandi/band_backend/repository/ClubGalPhotoRepositoryTest.java`
- `src/test/java/com/jandi/band_backend/poll/repository/PollRepositoryTest.java`
- `docs/notice-notice.md`
