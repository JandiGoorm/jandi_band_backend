# Redis 캐싱 구현 가이드

## 📋 개요

잔디밴드 백엔드에 Redis 기반 캐싱을 적용하여 API 응답 성능을 향상시키고 데이터베이스 부하를 줄이는 구현 가이드입니다.

## 🎯 캐싱 우선순위 및 적용 범위

### ✅ 높은 우선순위 (완료)
1. **정적 데이터**: 대학교/지역 정보
2. **프로모션 관련**: 공연 홍보 목록, 검색, 필터링
3. **동아리 기본 정보**: 목록, 상세, 멤버 정보

### ✅ 중간 우선순위 (완료)
4. **사용자 개인 정보**: 내 정보, 마이페이지, 시간표
5. **팀 정보**: 팀 목록, 상세 정보
6. **일정 정보**: 캘린더 통합 조회, 연습 일정
7. **초대 시스템**: 동아리/팀 초대 코드 관리



## 🔧 Redis 캐시 설정

### 기본 설정 (`RedisConfig.java`)

```java
@Configuration
@EnableCaching
public class RedisConfig {
    // CacheManager 설정
    @Bean
    public CacheManager cacheManager() {
        RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1)) // 기본 TTL 1시간
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new Jackson2JsonRedisSerializer<>(Object.class)));
    }
}
```

### 캐시별 TTL 설정

| 캐시명 | TTL | 용도 |
|--------|-----|------|
| `universities` | 1일 | 대학교 정보 (정적) |
| `regions` | 1일 | 지역 정보 (정적) |
| `promos` | 30분 | 프로모션 목록 |
| `promoSearch` | 15분 | 프로모션 검색 결과 |
| `clubs` | 1시간 | 동아리 정보 |
| `clubMembers` | 30분 | 동아리 멤버 정보 |
| `userInfo` | 1시간 | 사용자 기본 정보 |
| `myPages` | 30분 | 마이페이지 데이터 |
| `userTimetables` | 30분 | 사용자 시간표 |
| `teams` | 30분 | 팀 목록 |
| `teamDetails` | 15분 | 팀 상세 정보 |
| `schedules` | 15분 | 연습 일정 |
| `calendarEvents` | 15분 | 캘린더 통합 일정 |
| `inviteCodes` | 7일 | 초대 코드 (동아리/팀) |

## 📚 서비스별 캐싱 구현

### 1. 정적 데이터 캐싱

#### RegionService
```java
@Cacheable(value = "regions", key = "'all'")
public List<RegionRespDTO> getAllRegions() {
    // 전체 지역 목록 조회
}
```

#### UniversityService
```java
@Cacheable(value = "universities", key = "#filter + '_' + (#type != null ? #type : 'null') + '_' + (#region != null ? #region : 'null')")
public List<UniversityRespDTO> getAllUniversity(String filter, String type, String region) {
    // 필터별 대학교 목록 조회
}

@Cacheable(value = "universities", key = "'detail_' + #id")
public UniversityDetailRespDTO getUniversityById(Integer id) {
    // 대학교 상세 정보 조회
}
```

### 2. 프로모션 캐싱

#### PromoService
```java
// 조회 메소드 캐싱
@Cacheable(value = "promos", key = "'list_' + #pageable.pageNumber + '_' + #pageable.pageSize + '_' + #pageable.sort.toString()")
public Page<PromoRespDTO> getPromos(Pageable pageable) { }

@Cacheable(value = "promoSearch", key = "'search_' + #keyword + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
public Page<PromoRespDTO> searchPromos(String keyword, Pageable pageable) { }

// CUD 작업 시 캐시 무효화
@CacheEvict(value = {"promos", "promoSearch"}, allEntries = true)
public PromoSimpleRespDTO createPromo(PromoReqDTO request, Integer creatorId) { }
```

### 3. 동아리 캐싱

#### ClubService
```java
@Cacheable(value = "clubs", key = "'list_' + #pageable.pageNumber + '_' + #pageable.pageSize + '_' + #pageable.sort.toString()")
public Page<ClubRespDTO> getClubList(Pageable pageable) { }

@Cacheable(value = "clubs", key = "'detail_' + #clubId")
public ClubDetailRespDTO getClubDetail(Integer clubId) { }

@Cacheable(value = "clubMembers", key = "'members_' + #clubId")
public ClubMembersRespDTO getClubMembers(Integer clubId) { }
```

### 4. 사용자 정보 캐싱

#### UserService
```java
@Cacheable(value = "userInfo", key = "'user_' + #userId")
public Users getMyInfo(Integer userId) { }

@CacheEvict(value = {"userInfo", "myPages"}, key = "'user_' + #userId")
public Integer updateMyInfo(Integer userId, UpdateUserInfoReqDTO updateDTO) { }
```

#### MyPageService
```java
@Cacheable(value = "myPages", key = "'clubs_' + #userId")
public List<MyClubRespDTO> getMyClubs(Integer userId) { }

@Cacheable(value = "myPages", key = "'teams_' + #userId")
public List<MyTeamRespDTO> getMyTeams(Integer userId) { }
```

### 5. 팀 정보 캐싱

#### TeamService
```java
@Cacheable(value = "teams", key = "'club_' + #clubId + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
public Page<TeamRespDTO> getTeamsByClub(Integer clubId, Pageable pageable, Integer currentUserId) { }

@Cacheable(value = "teamDetails", key = "'detail_' + #teamId")
public TeamDetailRespDTO getTeamDetail(Integer teamId, Integer currentUserId) { }
```

### 6. 일정 정보 캐싱

#### ClubEventService
```java
// ⭐ 핵심 캐시: 캘린더 통합 일정 조회
@Cacheable(value = "calendarEvents", key = "'calendar_' + #clubId + '_' + #year + '_' + #month")
public List<CalendarEventRespDTO> getCalendarEventsForClub(Integer clubId, Integer userId, int year, int month) {
    // 동아리 일정 + 모든 하위 팀 일정 통합 조회
}
```

#### PracticeScheduleService
```java
@Cacheable(value = "schedules", key = "'team_' + #teamId + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
public Page<PracticeScheduleRespDTO> getPracticeSchedulesByTeam(Integer teamId, Pageable pageable, Integer userId) { }
```

### 7. 초대 시스템 Redis 저장

#### InviteCodeService
```java
@Service
@RequiredArgsConstructor
public class InviteCodeService {
    private final StringRedisTemplate redisTemplate;
    @Value("${invite.expire.days}") private Integer expireDays;

    // 초대 코드 저장 (TTL: 7일)
    public void saveCode(InviteType type, Integer id, String code) {
        String keyId = type + ":" + id;  // 예: "CLUB:1", "TEAM:5"
        redisTemplate.opsForValue().set(code, keyId, Duration.ofDays(expireDays));
    }

    // 초대 코드로 대상 정보 조회
    public String getKeyId(String code) {
        String keyId = redisTemplate.opsForValue().get(code);
        if(keyId == null) {
            throw new InvalidAccessException("권한 오류: code=" + code +"를 찾을 수 없습니다.");
        }
        return keyId;
    }

    // 사용된 초대 코드 삭제
    public void deleteRecord(String code) {
        redisTemplate.delete(code);
    }
}
```

#### InviteService
```java
// 동아리 초대 링크 생성
@Transactional
public InviteLinkRespDTO generateInviteClubLink(Integer clubId, Integer userId) {
    // 권한 확인 후 초대 코드 생성
    String code = generateRandomCode();
    inviteCodeService.saveCode(InviteType.CLUB, clubId, code);

    String link = clubLinkPrefix + "?code=" + code;
    return new InviteLinkRespDTO(link);
}

// 팀 초대 링크 생성
@Transactional
public InviteLinkRespDTO generateInviteTeamLink(Integer teamId, Integer userId) {
    // 권한 확인 후 초대 코드 생성
    String code = generateRandomCode();
    inviteCodeService.saveCode(InviteType.TEAM, teamId, code);

    String link = teamLinkPrefix + "?code=" + code;
    return new InviteLinkRespDTO(link);
}
```

## 🔑 캐시 키 전략

### 명명 규칙
```
{캐시명}::{타입}_{식별자}_{추가정보}
```

### 주요 캐시 키 예시
```bash
# 정적 데이터
regions::all
universities::ALL_null_null
universities::TYPE_UNIVERSITY_null
universities::detail_1

# 프로모션
promos::list_0_20_createdAt,desc
promoSearch::search_락밴드_0_20

# 동아리
clubs::list_0_10_createdAt,desc
clubs::detail_1
clubMembers::members_1

# 사용자
userInfo::user_1
myPages::clubs_1
userTimetables::list_1

# 팀
teams::club_1_0_10
teamDetails::detail_1

# 일정 (핵심)
calendarEvents::calendar_1_2024_3
schedules::team_1_0_20

# 초대 코드 (임시 저장)
{randomCode10자리} -> "CLUB:1"
{randomCode10자리} -> "TEAM:5"
```

## 🚀 성능 향상 효과

### 예상 성능 개선

| API 카테고리 | 중요도 | 응답시간 단축 | DB 부하 감소 |
|-------------|--------|-------------|-------------|
| 대학교/지역 조회 | ⭐⭐⭐ | | |
| 캘린더 통합 조회 | ⭐⭐⭐ | | |
| 프로모션 목록/검색 | ⭐⭐⭐ | | |
| 마이페이지 | ⭐⭐ | | |
| 팀 상세 정보 | ⭐⭐ | | |
| 동아리 정보 | ⭐⭐ | | |
| 초대 코드 관리 | ⭐⭐ | | |
| 사용자 시간표 | ⭐ | | |
| 연습 일정 | ⭐ | | |

### 전체 시스템 영향
- **평균 응답시간**: 50-80% 단축 예상
- **데이터베이스 부하**: 60-90% 감소 예상
- **동시 사용자 처리량**: 2-3배 증가 예상

## 🔄 캐시 무효화 전략

### 연쇄 무효화 설계
```java
// 팀 변경 시
@CacheEvict(value = {"teams", "teamDetails", "myPages", "calendarEvents"}, allEntries = true)

// 일정 변경 시
@CacheEvict(value = {"schedules", "calendarEvents"}, allEntries = true)

// 사용자 정보 변경 시
@CacheEvict(value = {"userInfo", "myPages"}, key = "'user_' + #userId")

// 초대 코드 사용 시 (일회성 삭제)
inviteCodeService.deleteRecord(code);
```

### 무효화 정책
- **즉시 무효화**: CUD 작업 완료 시 즉시 캐시 삭제
- **전체 vs 선택적**: 안전성 우선으로 대부분 전체 무효화 적용
- **연관 캐시**: 관련된 모든 캐시를 함께 무효화
- **일회성 데이터**: 초대 코드는 사용 즉시 삭제 (보안)

## 🔍 모니터링 및 운영

### Redis CLI 모니터링
```bash
# Redis 연결
redis-cli

# 전체 캐시 키 확인
KEYS *

# 특정 카테고리 캐시 확인
KEYS "*regions*"
KEYS "*promos*"
KEYS "*calendarEvents*"

# 초대 코드 확인 (랜덤 키이므로 전체 확인)
KEYS "*" | grep -E "^[a-zA-Z0-9]{10}$"

# 캐시 내용 및 TTL 확인
GET "regions::all"
TTL "universities::ALL_null_null"

# 캐시 통계
INFO memory
INFO keyspace
```

### 성능 모니터링 지표
1. **캐시 적중률** (Cache Hit Ratio)
2. **평균 응답시간** 변화
3. **데이터베이스 쿼리 수** 감소
4. **Redis 메모리 사용량**
5. **동시 접속자 수** 처리 능력

### 주요 모니터링 포인트
```bash
# 가장 중요한 캐시들
GET "calendarEvents::calendar_1_2024_3"  # 캘린더 통합 조회
GET "universities::ALL_null_null"        # 대학교 전체 목록
GET "promos::list_0_20_createdAt,desc"   # 프로모션 목록

# 초대 코드 TTL 확인 (보안 관련)
TTL {초대코드}  # 7일(604800초) 이하인지 확인
```

## ⚠️ 주의사항 및 운영 가이드

### 1. 캐시 일관성
- **강한 일관성 필요**: 즉시 캐시 무효화 적용
- **eventual consistency 허용**: TTL 기반 만료 대기
- **중요 데이터**: 실시간 반영이 필요한 데이터는 캐시 TTL 단축

### 2. 메모리 관리
- **TTL 설정**: 데이터 특성에 맞는 적절한 TTL 설정
- **메모리 사용량 모니터링**: Redis 메모리 사용량 지속 관찰
- **캐시 크기 제한**: 필요시 maxmemory 정책 설정

### 3. 장애 대응
- **Redis 장애 시**: 캐시 미스로 동작, DB 부하 증가 주의
- **캐시 워밍업**: 서버 재시작 후 주요 캐시 미리 로딩
- **Fallback 전략**: 캐시 실패 시 DB 조회로 자동 전환
- **초대 코드 손실**: Redis 장애 시 기존 초대 링크 무효화, 재생성 필요

### 4. 개발 시 고려사항
```java
// 올바른 캐시 적용 예시
@Cacheable(value = "myCache", key = "'prefix_' + #param")
public DataType getData(String param) {
    // 무거운 DB 조회 로직
}

@CacheEvict(value = "myCache", allEntries = true)
public void updateData(DataUpdateRequest request) {
    // 데이터 수정 로직
    // 캐시 무효화는 자동 처리됨
}
```

### 5. 성능 최적화 팁
- **적절한 TTL**: 데이터 변경 빈도에 맞는 TTL 설정
- **키 설계**: 효율적인 캐시 키 네이밍으로 관리 용이성 확보
- **부분 무효화**: 가능한 경우 전체보다 특정 키만 무효화
- **배치 처리**: 대량 데이터 변경 시 배치로 캐시 무효화

## 📈 향후 개선 방안

### 1. 고도화 방안
- **분산 캐시**: Redis Cluster 적용
- **캐시 레이어링**: L1(로컬) + L2(Redis) 캐시
- **지능형 캐시**: 사용 패턴 기반 TTL 동적 조정
- **캐시 압축**: 대용량 데이터 압축 저장

### 2. 모니터링 고도화
- **대시보드**: Grafana 연동 캐시 모니터링
- **알림**: 캐시 적중률 하락 시 알림
- **자동 최적화**: 사용 패턴 분석 기반 자동 TTL 조정

---

## 📞 문제 발생 시 확인

캐싱 관련 이슈나 성능 문제 발생 시:
1. Redis 연결 상태 확인: `GET /health/redis`
2. 캐시 키 현황 확인: `redis-cli KEYS *`
3. 메모리 사용량 확인: `redis-cli INFO memory`
