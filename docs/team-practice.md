# 팀 연습 일정 API 명세서

## 📋 개요
팀의 곡 연습 일정을 관리하는 API입니다. 기존 TeamEvent 엔티티를 활용하여 구현되었습니다.

## 🔐 인증
모든 API는 JWT 토큰 인증이 필요합니다.
```
Authorization: Bearer {JWT_TOKEN}
```

---

## 1. 곡 팀 연습 일정 추가

### `POST /api/v1/teams/{teamId}/practice-schedules`

팀에 새로운 곡 연습 일정을 추가합니다.

### 요청
**Path Parameters:**
- `teamId` (Integer): 팀 ID

**Request Body:**
```json
{
  "songName": "string",           // 필수: 곡 제목 (최대 100자)
  "artistName": "string",         // 선택: 아티스트명 (최대 100자)
  "youtubeUrl": "string",         // 선택: YouTube URL (최대 500자)
  "startDatetime": "datetime",    // 필수: 연습 시작 일시 (ISO 8601 형식)
  "endDatetime": "datetime",      // 필수: 연습 종료 일시 (ISO 8601 형식)
  "location": "string",           // 선택: 장소 (최대 255자)
  "address": "string",            // 선택: 주소 (최대 255자)
  "additionalDescription": "string" // 선택: 추가 설명
}
```

### 응답
```json
{
  "success": true,
  "message": "곡 연습 일정 생성 성공",
  "data": {
    "id": 1,
    "teamId": 1,
    "teamName": "밴드팀",
    "songName": "Bohemian Rhapsody",
    "artistName": "Queen",
    "youtubeUrl": "https://www.youtube.com/watch?v=fJ9rUzIMcZQ",
    "startDatetime": "2024-01-15T19:00:00",
    "endDatetime": "2024-01-15T21:00:00",
    "location": "연습실 A",
    "address": "서울시 강남구 테헤란로 123",
    "additionalDescription": "보컬 파트 집중 연습",
    "creatorId": 1,
    "creatorName": "김연습",
    "createdAt": "2024-01-10T10:00:00",
    "updatedAt": "2024-01-10T10:00:00",
    "participants": []
  }
}
```

### cURL 예제
```bash
curl -X POST "http://localhost:8080/api/v1/teams/1/practice-schedules" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "songName": "Bohemian Rhapsody",
    "artistName": "Queen",
    "youtubeUrl": "https://www.youtube.com/watch?v=fJ9rUzIMcZQ",
    "startDatetime": "2024-01-15T19:00:00",
    "endDatetime": "2024-01-15T21:00:00",
    "location": "연습실 A",
    "address": "서울시 강남구 테헤란로 123",
    "additionalDescription": "보컬 파트 집중 연습"
  }'
```

---

## 2. 곡 팀 연습 일정 목록 조회

### `GET /api/v1/teams/{teamId}/practice-schedules`

특정 팀의 곡 연습 일정 목록을 조회합니다.

### 요청
**Path Parameters:**
- `teamId` (Integer): 팀 ID

**Query Parameters:**
- `page` (Integer, 선택): 페이지 번호 (기본값: 0)
- `size` (Integer, 선택): 페이지 크기 (기본값: 20)
- `sort` (String, 선택): 정렬 기준 (기본값: startDatetime,asc)

### 응답
```json
{
  "success": true,
  "message": "팀별 곡 연습 일정 목록 조회 성공",
  "data": {
    "content": [
      {
        "id": 1,
        "teamId": 1,
        "teamName": "밴드팀",
        "songName": "Bohemian Rhapsody",
        "artistName": "Queen",
        "youtubeUrl": "https://www.youtube.com/watch?v=fJ9rUzIMcZQ",
        "startDatetime": "2024-01-15T19:00:00",
        "endDatetime": "2024-01-15T21:00:00",
        "location": "연습실 A",
        "address": "서울시 강남구 테헤란로 123",
        "additionalDescription": "보컬 파트 집중 연습",
        "creatorId": 1,
        "creatorName": "김연습",
        "createdAt": "2024-01-10T10:00:00",
        "updatedAt": "2024-01-10T10:00:00",
        "participants": []
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 20,
      "sort": {
        "sorted": true,
        "ascending": true
      }
    },
    "totalElements": 1,
    "totalPages": 1,
    "first": true,
    "last": true,
    "numberOfElements": 1
  }
}
```

### cURL 예제
```bash
# 기본 조회
curl -X GET "http://localhost:8080/api/v1/teams/1/practice-schedules" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# 페이지네이션과 정렬
curl -X GET "http://localhost:8080/api/v1/teams/1/practice-schedules?page=0&size=10&sort=startDatetime,desc" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

## 3. 곡 팀 연습 일정 상세 조회

### `GET /api/v1/practice-schedules/{scheduleId}`

특정 곡 연습 일정의 상세 정보를 조회합니다.

### 요청
**Path Parameters:**
- `scheduleId` (Integer): 연습 일정 ID

### 응답
```json
{
  "success": true,
  "message": "곡 연습 일정 상세 조회 성공",
  "data": {
    "id": 1,
    "teamId": 1,
    "teamName": "밴드팀",
    "songName": "Bohemian Rhapsody",
    "artistName": "Queen",
    "youtubeUrl": "https://www.youtube.com/watch?v=fJ9rUzIMcZQ",
    "startDatetime": "2024-01-15T19:00:00",
    "endDatetime": "2024-01-15T21:00:00",
    "location": "연습실 A",
    "address": "서울시 강남구 테헤란로 123",
    "additionalDescription": "보컬 파트 집중 연습",
    "creatorId": 1,
    "creatorName": "김연습",
    "createdAt": "2024-01-10T10:00:00",
    "updatedAt": "2024-01-10T10:00:00",
    "participants": [
      {
        "id": 1,
        "userId": 2,
        "userName": "이기타"
      },
      {
        "id": 2,
        "userId": 3,
        "userName": "박드럼"
      }
    ]
  }
}
```

### cURL 예제
```bash
curl -X GET "http://localhost:8080/api/v1/practice-schedules/1" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

## 4. 곡 팀 연습 일정 삭제

### `DELETE /api/v1/practice-schedules/{scheduleId}`

곡 연습 일정을 삭제합니다. (소프트 삭제)

### 요청
**Path Parameters:**
- `scheduleId` (Integer): 연습 일정 ID

### 응답
```json
{
  "success": true,
  "message": "곡 연습 일정 삭제 성공"
}
```

### cURL 예제
```bash
curl -X DELETE "http://localhost:8080/api/v1/practice-schedules/1" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

## 📝 에러 응답

모든 API에서 공통으로 사용되는 에러 응답 형식입니다.

### 400 Bad Request (유효성 검사 실패)
```json
{
  "success": false,
  "message": "곡 제목은 필수입니다",
  "errorCode": "VALIDATION_ERROR"
}
```

### 401 Unauthorized (인증 실패)
```json
{
  "success": false,
  "message": "인증이 필요합니다",
  "errorCode": "UNAUTHORIZED"
}
```

### 403 Forbidden (권한 없음)
```json
{
  "success": false,
  "message": "연습 일정을 삭제할 권한이 없습니다",
  "errorCode": "FORBIDDEN"
}
```

### 404 Not Found (리소스 없음)
```json
{
  "success": false,
  "message": "연습 일정을 찾을 수 없습니다",
  "errorCode": "RESOURCE_NOT_FOUND"
}
```

---

## 🔍 구현 참고사항

### 데이터 저장 방식
- **TeamEvent.name**: `"곡명 - 아티스트명"` 형태로 저장
- **TeamEvent.description**: `"YouTube URL\n추가설명"` 형태로 저장
- 곡 연습 일정 구분: name 필드에 " - "가 포함된 TeamEvent만 연습 일정으로 처리

### 권한 관리
- 연습 일정 생성: 인증된 모든 사용자
- 연습 일정 조회: 인증된 모든 사용자
- 연습 일정 삭제: 생성자만 가능

### 페이지네이션
- 기본 페이지 크기: 20
- 기본 정렬: 시작 일시 오름차순 (startDatetime,asc)
- 지원되는 정렬 필드: startDatetime, createdAt, updatedAt

### 시간 형식
- 모든 날짜/시간은 ISO 8601 형식 사용
- 예: `"2024-01-15T19:00:00"`
- 타임존 정보가 없으면 서버 로컬 시간으로 처리
