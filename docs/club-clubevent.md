# Club Event API 명세서

## Base URL
`/api/clubs/{clubId}/events`

## 인증
JWT 인증 필요 (Spring Security + @AuthenticationPrincipal CustomUserDetails)

## 권한 관리
- **조회**: 로그인한 모든 사용자
- **생성**: 동아리 멤버만 가능  
- **삭제**: 일정을 생성한 사용자, 동아리 대표자, 또는 ADMIN 권한 사용자

---

## 1. 동아리 일정 생성
### POST `/api/clubs/{clubId}/events`

#### 경로 파라미터
- `clubId` (integer, 필수): 동아리 ID

#### 요청
```bash
curl -X POST "http://localhost:8080/api/clubs/1/events" \
  -H "Authorization: Bearer {JWT_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "2024년 정기 공연",
    "startDatetime": "2024-03-15T19:00:00",
    "endDatetime": "2024-03-15T21:30:00",
    "participantUserIds": [2, 3, 5, 7]
  }'
```

#### 요청 필드
- `name` (string, 필수): 이벤트 이름 (최대 255자)
- `startDatetime` (string, 필수): 시작 일시 (ISO 8601 형식: YYYY-MM-DDTHH:mm:ss)
- `endDatetime` (string, 필수): 종료 일시 (시작 일시 이후여야 함)
- `participantUserIds` (array, 선택): 참여자 사용자 ID 목록

#### 응답 (200 OK)
```json
{
  "success": true,
  "message": "동아리 일정이 생성되었습니다.",
  "data": {
    "id": 15,
    "name": "2024년 정기 공연",
    "startDatetime": "2024-03-15T19:00:00",
    "endDatetime": "2024-03-15T21:30:00"
  }
}
```

---

## 2. 동아리 일정 상세 조회
### GET `/api/clubs/{clubId}/events/{eventId}`

#### 경로 파라미터
- `clubId` (integer, 필수): 동아리 ID
- `eventId` (integer, 필수): 일정 ID

#### 요청
```bash
curl -X GET "http://localhost:8080/api/clubs/1/events/15" \
  -H "Authorization: Bearer {JWT_TOKEN}"
```

#### 응답 (200 OK)
```json
{
  "success": true,
  "message": "동아리 일정 상세 조회 성공",
  "data": {
    "id": 15,
    "name": "2024년 정기 공연",
    "startDatetime": "2024-03-15T19:00:00",
    "endDatetime": "2024-03-15T21:30:00",
    "participants": [
      {
        "userId": 2,
        "userName": "김철수"
      },
      {
        "userId": 3,
        "userName": "이영희"
      },
      {
        "userId": 5,
        "userName": "박민수"
      }
    ]
  }
}
```

---

## 3. 동아리 일정 목록 조회 (월별)
### GET `/api/clubs/{clubId}/events/list/{year}/{month}`

#### 경로 파라미터
- `clubId` (integer, 필수): 동아리 ID
- `year` (integer, 필수): 조회할 연도 (예: 2024)
- `month` (integer, 필수): 조회할 월 (1-12)

#### 요청
```bash
curl -X GET "http://localhost:8080/api/clubs/1/events/list/2024/3" \
  -H "Authorization: Bearer {JWT_TOKEN}"
```

#### 응답 (200 OK)
```json
{
  "success": true,
  "message": "동아리 일정 목록 조회 성공",
  "data": [
    {
      "id": 15,
      "name": "2024년 정기 공연",
      "startDatetime": "2024-03-15T19:00:00",
      "endDatetime": "2024-03-15T21:30:00"
    },
    {
      "id": 16,
      "name": "합주 연습",
      "startDatetime": "2024-03-20T18:00:00",
      "endDatetime": "2024-03-20T20:00:00"
    }
  ]
}
```

---

## 4. 동아리 일정 삭제
### DELETE `/api/clubs/{clubId}/events/{eventId}`

#### 경로 파라미터
- `clubId` (integer, 필수): 동아리 ID
- `eventId` (integer, 필수): 삭제할 일정 ID

#### 요청
```bash
curl -X DELETE "http://localhost:8080/api/clubs/1/events/15" \
  -H "Authorization: Bearer {JWT_TOKEN}"
```

#### 응답 (200 OK)
```json
{
  "success": true,
  "message": "동아리 일정이 삭제되었습니다.",
  "data": null
}
```

#### 접근 권한
- 일정을 생성한 사용자, 동아리 대표자, 또는 ADMIN 권한 사용자만 삭제 가능
- 권한이 없는 경우 403 Forbidden 응답

---

## 공통 에러 응답

### 400 Bad Request - 잘못된 요청
```json
{
  "success": false,
  "message": "필수 필드가 누락되었습니다.",
  "errorCode": "BAD_REQUEST"
}
```
**발생 케이스**: 필수 필드 누락, 잘못된 날짜 형식, 종료일시가 시작일시보다 빠른 경우

### 401 Unauthorized - 인증 실패
```json
{
  "success": false,
  "message": "유효하지 않은 토큰입니다.",
  "errorCode": "INVALID_TOKEN"
}
```
**발생 케이스**: JWT 토큰이 없거나 유효하지 않은 경우

### 403 Forbidden - 권한 없음
```json
{
  "success": false,
  "message": "동아리 부원만 이벤트를 생성할 수 있습니다.",
  "errorCode": "FORBIDDEN"
}
```
**발생 케이스**: 
- 동아리 멤버가 아닌 사용자가 일정 생성 시도
- 일정 삭제 시 생성자, 동아리 대표자, ADMIN이 아닌 경우

### 404 Not Found - 리소스 없음
```json
{
  "success": false,
  "message": "해당 동아리에 속한 일정을 찾을 수 없습니다.",
  "errorCode": "NOT_FOUND"
}
```
**발생 케이스**: 존재하지 않는 동아리 ID 또는 일정 ID, 참여자 ID

---

## 데이터 모델

### ClubEventReqDTO (요청)
```typescript
interface ClubEventReqDTO {
  name: string;                    // 일정 이름 (필수, 최대 255자)
  startDatetime: string;           // 시작 시간 (필수, LocalDateTime)
  endDatetime: string;             // 종료 시간 (필수, LocalDateTime, 시작시간 이후)
  participantUserIds?: number[];   // 참여자 사용자 ID 목록 (선택)
}
```

### ClubEventRespDTO (응답 - 목록 조회용)
```typescript
interface ClubEventRespDTO {
  id: number;            // 일정 ID (Long 타입)
  name: string;          // 일정 이름
  startDatetime: string; // 시작 시간 (LocalDateTime)
  endDatetime: string;   // 종료 시간 (LocalDateTime)
}
```

### ClubEventDetailRespDTO (응답 - 상세 조회용)
```typescript
interface ClubEventDetailRespDTO {
  id: number;            // 일정 ID (Long 타입)
  name: string;          // 일정 이름
  startDatetime: string; // 시작 시간 (LocalDateTime)
  endDatetime: string;   // 종료 시간 (LocalDateTime)
  participants: ParticipantRespDTO[]; // 참여자 목록
}

interface ParticipantRespDTO {
  userId: number;        // 참여자 사용자 ID
  userName: string;      // 참여자 닉네임
}
```

## 참고사항

### 소프트 삭제
- 삭제된 일정과 참여자는 DB에서 물리적으로 제거되지 않고 `deletedAt` 필드로 관리
- 조회 시 삭제된 일정과 참여자는 자동으로 제외됨
- 일정 삭제 시 관련된 모든 참여자도 함께 소프트 삭제됨

### 권한 관리
- **ADMIN 권한**: Users 테이블의 admin_role이 'ADMIN'인 사용자는 모든 일정 삭제 가능
- **동아리 대표자**: ClubMember 테이블에서 role이 'REPRESENTATIVE'인 사용자는 해당 동아리의 모든 일정 삭제 가능
- **일반 사용자**: 본인이 생성한 일정만 삭제 가능
- **일정 생성**: 동아리 멤버만 가능 (ClubMember 테이블에 등록된 사용자)

### 참여자 관리
- 일정 생성 시 `participantUserIds` 배열로 참여자 지정 가능
- 참여자로 지정되는 사용자는 실제 존재하는 사용자여야 함
- 참여자 정보는 상세 조회 시에만 포함됨 (목록 조회에서는 제외)

### 데이터 타입
- **ID**: Entity는 Integer, 응답 DTO는 Long으로 변환
- **날짜시간**: LocalDateTime 타입 사용 (JSON 직렬화 시 ISO 8601 형식)
- **Builder 패턴**: 모든 DTO는 Builder 패턴 지원
