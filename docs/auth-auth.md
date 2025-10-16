# Auth API

## 인증 방식
카카오 OAuth 로그인 + JWT 토큰

---

## 1. 카카오 로그인
```
GET /api/auth/login?code={KAKAO_AUTH_CODE}
```

### 요청 예시
```bash
curl "http://localhost:8080/api/auth/login?code=abc123def456"
```

### 쿼리 파라미터
- `code`: 카카오 OAuth 인증 코드

### 성공 응답 (200)
```json
{
  "success": true,
  "message": "로그인 성공",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "isRegistered": true
  }
}
```

### 응답 필드
- `accessToken`: JWT 액세스 토큰
- `refreshToken`: JWT 리프레시 토큰
- `isRegistered`: 회원가입 완료 여부 (false면 회원가입 필요)

### 실패 응답
- **400**: 잘못된 카카오 인증 코드
- **500**: 카카오 서버 오류

---

## 2. 로그아웃
```
POST /api/auth/logout
Authorization: Bearer {JWT_TOKEN}
```

### 요청 예시
```bash
curl -X POST "http://localhost:8080/api/auth/logout" \
  -H "Authorization: Bearer {JWT_TOKEN}"
```

### 성공 응답 (200)
```json
{
  "success": true,
  "message": "로그아웃 완료",
  "data": null
}
```

### 실패 응답
- **401**: 유효하지 않은 토큰

---

## 3. 회원가입
```
POST /api/auth/signup
Authorization: Bearer {TEMP_JWT_TOKEN}
Content-Type: application/json
```

### 요청 예시
```bash
curl -X POST "http://localhost:8080/api/auth/signup" \
  -H "Authorization: Bearer {TEMP_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "position": "GUITAR",
    "university": "서울대학교"
  }'
```

### 요청 필드
- `position`: 포지션 (VOCAL, GUITAR, KEYBOARD, BASS, DRUM, OTHER)
- `university`: 대학교 이름

### 성공 응답 (200)
```json
{
  "success": true,
  "message": "회원가입 성공",
  "data": {
    "id": 1,
    "nickname": "홍길동",
    "profilePhoto": "https://example.com/profile.jpg",
    "position": "GUITAR",
    "university": "서울대학교"
  }
}
```

### 실패 응답
- **400**: 이미 회원가입 완료된 계정
- **401**: 유효하지 않은 임시 토큰
- **404**: 존재하지 않는 대학교

---

## 4. 회원탈퇴 (확장된 3단계 처리)
```
POST /api/auth/cancel
Authorization: Bearer {JWT_TOKEN}
```

### 요청 예시
```bash
curl -X POST "http://localhost:8080/api/auth/cancel" \
  -H "Authorization: Bearer {JWT_TOKEN}"
```

### 성공 응답 (200)
```json
{
  "success": true,
  "message": "회원탈퇴 성공",
  "data": null
}
```

### 회원탈퇴 처리 방식

#### **사전 검증**
- 동아리 대표자는 탈퇴 불가 (먼저 대표자 권한 위임 필요)
- 카카오 OAuth 연결 해제

#### **그룹 1: 개인 종속 데이터 (소프트 삭제)**
즉시 소프트 삭제 처리 (deleted_at 설정), 7일 후 cronjob으로 하드 삭제 예정

**대상 테이블:**
- `users`: 사용자 기본 정보
- `user_photo`: 사용자 프로필 사진
- `user_timetable`: 사용자 시간표
- `club_member`: 동아리 멤버십
- `team_member`: 팀 멤버십

#### **그룹 2: 콘텐츠 소유권 (익명화 처리)**
즉시 user_id 관련 필드를 -1로 변경하여 익명화

**대상 테이블:**
- `club_gal_photo`: 동아리 갤러리 사진
- `club_event`: 동아리 이벤트
- `poll`: 투표
- `poll_song`: 투표 곡 제안
- `promo`: 홍보글
- `promo_photo`: 홍보글 사진
- `promo_comment`: 홍보글 댓글
- `team`: 팀
- `team_event`: 팀 이벤트
- `promo_report`: 홍보글 신고
- `promo_comment_report`: 댓글 신고

#### **그룹 3: 사용자 고유 행위 (즉시 하드 삭제 + 카운트 조정)**
유니크 제약이 있는 데이터는 즉시 DELETE 처리하고 연관 카운트 조정

**대상 테이블:**
- `vote`: 투표 행위 삭제 (카운트 조정 불필요, 실시간 연산)
- `promo_like`: 홍보글 좋아요 삭제 + promo.like_count - 1
- `promo_comment_like`: 댓글 좋아요 삭제 (카운트 조정 불필요, 실시간 연산)

### 처리 결과 로깅
- 각 그룹별 레코드 정리 작업 로깅
- 전체 처리 과정의 성공/실패 상태 기록

### 탈퇴 후 제약 사항
- 탈퇴 후 7일간 재로그인 제한 (deleted_at 기반 체크)
- 익명화된 콘텐츠(user_id: -1)는 "(탈퇴한 계정)"으로 표시
- 7일 후 cronjob에 의한 개인 종속 데이터 완전 삭제 예정
- 투표 집계는 실시간 연산으로 처리되어 별도 카운트 조정 불필요

### 실패 응답
- **400**: 동아리 대표자는 탈퇴 불가 (먼저 대표자 권한 위임 필요)
- **401**: 유효하지 않은 토큰

---

## 5. 토큰 재발급
```
POST /api/auth/refresh
Content-Type: application/json
```

### 요청 예시
```bash
curl -X POST "http://localhost:8080/api/auth/refresh" \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }'
```

### 요청 필드
- `refreshToken`: 리프레시 토큰

### 성공 응답 (200)
```json
{
  "success": true,
  "message": "토큰 재발급 성공",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }
}
```

### 실패 응답
- **401**: 만료되거나 유효하지 않은 리프레시 토큰

---

## 에러 응답
```json
{
  "success": false,
  "message": "에러 메시지",
  "data": null
}
```

### HTTP 상태 코드
- `200 OK`: 성공
- `400 Bad Request`: 잘못된 요청
- `401 Unauthorized`: 인증 실패
- `500 Internal Server Error`: 서버 오류

## 포지션 값
- `VOCAL`: 보컬
- `GUITAR`: 기타
- `KEYBOARD`: 키보드
- `BASS`: 베이스
- `DRUM`: 드럼
- `OTHER`: 기타
