# Redis 캐싱 운영 체크리스트

## 🚀 배포 전 체크리스트

### ✅ 설정 확인
- [ ] Redis 서버 연결 상태 확인: `GET /health/redis`
- [ ] Redis 메모리 사용량 확인: `redis-cli INFO memory`
- [ ] CacheManager 빈 등록 확인
- [ ] @EnableCaching 어노테이션 적용 확인

### ✅ 캐시 적용 확인
- [ ] **정적 데이터**: 대학교/지역 정보 캐싱
- [ ] **공연홍보**: 목록/검색 캐싱
- [ ] **사용자**: 개인정보/마이페이지 캐싱
- [ ] **동아리**: 목록/상세/멤버 캐싱
- [ ] **팀**: 목록/상세 캐싱
- [ ] **일정**: 캘린더/연습일정 캐싱
- [ ] **초대시스템**: 초대 코드 Redis 저장

### ✅ 캐시 무효화 확인
- [ ] CUD 작업 시 `@CacheEvict` 적용
- [ ] 연관 캐시 함께 무효화
- [ ] 캐시 키 일관성 확인

## 🔍 운영 중 모니터링

### 일일 체크 (5분)
```bash
# Redis 연결 상태
curl http://localhost:8080/health/redis

# 주요 캐시 존재 확인
redis-cli KEYS "*regions*" | wc -l
redis-cli KEYS "*universities*" | wc -l
redis-cli KEYS "*promos*" | wc -l

# 초대 코드 개수 확인 (10자리 랜덤 코드)
redis-cli KEYS "*" | grep -E "^[a-zA-Z0-9]{10}$" | wc -l
```

### 주간 체크 (15분)
```bash
# 메모리 사용량
redis-cli INFO memory | grep used_memory_human

# 캐시 키 개수
redis-cli INFO keyspace

# 주요 캐시 TTL 확인
redis-cli TTL "regions::all"
redis-cli TTL "universities::ALL_null_null"

# 초대 코드 TTL 확인 (7일 = 604800초)
redis-cli KEYS "*" | grep -E "^[a-zA-Z0-9]{10}$" | head -5 | xargs -I {} redis-cli TTL {}
```

### 월간 체크 (30분)
- [ ] 캐시 적중률 분석
- [ ] 평균 응답시간 변화 추이
- [ ] DB 쿼리 수 감소율 확인
- [ ] Redis 메모리 사용 패턴 분석

## 🚨 장애 대응 체크리스트

### Redis 서버 장애
1. [ ] Redis 서버 상태 확인: `systemctl status redis`
2. [ ] Redis 재시작: `systemctl restart redis`
3. [ ] 애플리케이션 재시작 (필요시)
4. [ ] DB 부하 모니터링 강화

### 캐시 성능 저하
1. [ ] 메모리 사용량 확인: `redis-cli INFO memory`
2. [ ] 불필요한 캐시 키 정리: `redis-cli FLUSHALL` (주의!)
3. [ ] TTL 설정 재검토
4. [ ] 캐시 키 패턴 최적화

### 데이터 일관성 문제
1. [ ] 문제 API의 캐시 즉시 삭제
2. [ ] 관련 캐시들 연쇄 삭제
3. [ ] 캐시 무효화 로직 점검
4. [ ] TTL 단축 고려

### 초대 코드 보안 이슈
1. [ ] 만료된 초대 코드 정리: `redis-cli KEYS "*" | grep -E "^[a-zA-Z0-9]{10}$" | xargs -I {} sh -c 'redis-cli TTL {} | grep -q "^-1$" && redis-cli DEL {}'`
2. [ ] 의심스러운 초대 코드 패턴 확인
3. [ ] 초대 코드 생성/사용 로그 점검
4. [ ] TTL 설정 확인 (7일 초과 금지)

## 📊 핵심 성능 지표

### 🎯 목표 지표
- **캐시 적중률**: 80% 이상
- **평균 응답시간**: 기존 대비 60% 이상 단축
- **DB 쿼리 수**: 기존 대비 70% 이상 감소
- **Redis 메모리**: 2GB 이하 유지

### 📈 측정 방법
```bash
# 응답시간 측정 (주요 API)
curl -w "@curl-format.txt" -s -o /dev/null http://localhost:8080/api/region/all
curl -w "@curl-format.txt" -s -o /dev/null http://localhost:8080/api/clubs

# Redis 상태
redis-cli INFO stats | grep keyspace
redis-cli INFO memory | grep used_memory_human
```

## 🔧 주요 Redis 명령어

### 캐시 확인
```bash
# 전체 키 확인
redis-cli KEYS *

# 카테고리별 키 확인
redis-cli KEYS "*regions*"
redis-cli KEYS "*universities*"
redis-cli KEYS "*promos*"
redis-cli KEYS "*clubs*"
redis-cli KEYS "*calendarEvents*"

# 초대 코드 확인
redis-cli KEYS "*" | grep -E "^[a-zA-Z0-9]{10}$"

# 특정 캐시 값 확인
redis-cli GET "regions::all"
redis-cli GET "universities::ALL_null_null"

# 초대 코드 값 확인 (예시)
redis-cli GET "a1b2c3d4e5"  # CLUB:1 또는 TEAM:5 형태 반환

# TTL 확인
redis-cli TTL "promos::list_0_20_createdAt,desc"
redis-cli TTL "a1b2c3d4e5"  # 초대 코드 TTL (7일 = 604800초)
```

### 캐시 관리
```bash
# 특정 캐시 삭제
redis-cli DEL "promos::list_0_20_createdAt,desc"

# 패턴 매칭 삭제 (주의!)
redis-cli --scan --pattern "promos::*" | xargs redis-cli DEL

# 만료된 초대 코드 정리
redis-cli KEYS "*" | grep -E "^[a-zA-Z0-9]{10}$" | xargs -I {} sh -c 'redis-cli TTL {} | grep -q "^-1$" && redis-cli DEL {}'

# 전체 캐시 삭제 (비상시만!)
redis-cli FLUSHALL
```

### 메모리 관리
```bash
# 메모리 사용량
redis-cli INFO memory

# 키 개수
redis-cli DBSIZE

# 메모리 사용량 큰 키 찾기
redis-cli --bigkeys
```

## 🎯 최적화 포인트

### 높은 우선순위
1. **캘린더 통합 조회**: `calendarEvents::calendar_*`
2. **대학교 목록**: `universities::ALL_null_null`
3. **프로모션 목록**: `promos::list_*`

### 보안 중요도
1. **초대 코드**: 10자리 랜덤 문자열 (7일 TTL 필수)

### 모니터링 포커스
```bash
# 가장 중요한 캐시들 확인
redis-cli EXISTS "calendarEvents::calendar_1_2024_12"
redis-cli EXISTS "universities::ALL_null_null"
redis-cli EXISTS "regions::all"

# 활성 초대 코드 개수 확인 (보안)
redis-cli KEYS "*" | grep -E "^[a-zA-Z0-9]{10}$" | wc -l
```

### 성능 최적화 체크
- [ ] TTL이 적절한가?
- [ ] 캐시 키가 효율적인가?
- [ ] 불필요한 캐시가 있는가?
- [ ] 메모리 사용량이 적절한가?

## 📞 이슈 발생 시 대응

### 1단계: 즉시 확인
```bash
# 기본 상태 체크
curl http://localhost:8080/health
curl http://localhost:8080/health/redis
redis-cli ping
```

### 2단계: 상세 진단
```bash
# Redis 로그 확인
tail -f /var/log/redis/redis-server.log

# 애플리케이션 로그 확인
tail -f logs/application.log | grep -i cache
```

### 3단계: 복구 작업
1. 캐시 전체 삭제 후 재생성
2. Redis 재시작
3. 애플리케이션 재시작
4. DB 부하 모니터링
