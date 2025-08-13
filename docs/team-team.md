# Team API

## Base URL
`/api`

## 인증
JWT 인증 필요 (Spring Security + @AuthenticationPrincipal CustomUserDetails)

## 페이지네이션 응답 구조
팀 목록 조회 API는 다음과 같은 페이지네이션 구조를 사용합니다:

```json
{
  "success": true,
  "message": "응답 메시지",
  "data": {
    "content": [...],  // 실제 팀 데이터 배열
    "pageInfo": {
      "page": 0,           // 현재 페이지 번호 (0부터 시작)
      "size": 5,           // 페이지 크기
      "totalElements": 100, // 총 팀 수
      "totalPages": 20,    // 총 페이지 수
      "first": true,       // 첫 번째 페이지 여부
      "last": false,       // 마지막 페이지 여부
      "empty": false       // 비어있는 페이지 여부
    }
  }
}
```

---

## 팀 관리
JWT 인증 필요

---

## 1. 팀 생성
### POST `/api/clubs/{clubId}/teams`

#### 요청
```bash
curl -X POST "http://localhost:8080/api/clubs/1/teams" \
  -H "Authorization: Bearer {JWT_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "밴드 팀"
  }'
```

#### 요청 필드
- `name`: 팀 이름

#### 응답 (201 Created)
```json
{
  "success": true,
  "message": "곡 팀이 성공적으로 생성되었습니다.",
  "data": {
    "id": 1,
    "name": "밴드 팀",
    "clubId": 1,
    "clubName": "락밴드 동아리",
    "creatorId": 1,
    "creatorName": "홍길동",
    "createdAt": "2024-03-15T10:30:00",
    "memberCount": 1,
    "members": [
      {
        "userId": 1,
        "nickname": "홍길동",
        "profilePhoto": "https://example.com/profile.jpg",
        "position": "GUITAR",
        "joinedAt": "2024-03-15T10:30:00",
        "timetableUpdatedAt": "2024-03-15T10:30:00",
        "isSubmitted": true,
        "timetableData": {
          "Mon": ["14:00", "15:00"],
          "Tue": ["18:00", "19:00"],
          "Wed": [],
          "Thu": ["16:00", "17:00"],
          "Fri": ["17:00", "18:00"],
          "Sat": ["10:00", "11:00"],
          "Sun": []
        }
      }
    ]
  }
}
```

### 실패 응답
- **400**: 중복된 팀 이름
- **403**: 동아리 멤버가 아님

---

## 2. 동아리 팀 목록 조회
### GET `/api/clubs/{clubId}/teams`

#### 요청
```bash
curl -X GET "http://localhost:8080/api/clubs/1/teams?page=0&size=5" \
  -H "Authorization: Bearer {JWT_TOKEN}"
```

#### 쿼리 파라미터
- `page`: 페이지 번호 (기본값: 0)
- `size`: 페이지 크기 (기본값: 5)

#### 응답 (200 OK)
```json
{
  "success": true,
  "message": "곡 팀 목록을 성공적으로 조회했습니다.",
  "data": {
    "content": [
      {
        "id": 1,
        "name": "밴드 팀",
        "clubId": 1,
        "clubName": "락밴드 동아리",
        "creatorId": 1,
        "creatorName": "홍길동",
        "createdAt": "2024-03-15T10:30:00",
        "memberCount": 4
      }
    ],
    "pageInfo": {
      "totalElements": 1,
      "totalPages": 1,
      "first": true,
      "last": true,
      "size": 5,
      "page": 0,
      "empty": false
    }
  }
}
```

---

## 3. 팀 상세 정보 조회
### GET `/api/teams/{teamId}`

#### 요청
```bash
curl -X GET "http://localhost:8080/api/teams/1" \
  -H "Authorization: Bearer {JWT_TOKEN}"
```

#### 응답 (200 OK)
```json
{
  "success": true,
  "message": "곡 팀 정보를 성공적으로 조회했습니다.",
  "data": {
    "id": 1,
    "name": "밴드 팀",
    "clubId": 1,
    "clubName": "락밴드 동아리",
    "creatorId": 1,
    "creatorName": "홍길동",
    "createdAt": "2024-03-15T10:30:00",
    "memberCount": 4,
    "suggestedScheduleAt": "2024-03-15T10:30:00",
    "submissionProgress": {
      "submittedMember": 2,
      "totalMember": 4
    },
    "members": [
      {
        "userId": 1,
        "nickname": "홍길동",
        "profilePhoto": "https://example.com/profile.jpg",
        "position": "GUITAR",
        "joinedAt": "2024-03-15T10:30:00",
        "timetableUpdatedAt": "2024-03-15T10:30:00",
        "isSubmitted": true,
        "timetableData": {
          "Mon": ["14:00", "15:00"],
          "Tue": ["18:00", "19:00"],
          "Wed": [],
          "Thu": ["16:00", "17:00"],
          "Fri": ["17:00", "18:00"],
          "Sat": ["10:00", "11:00"],
          "Sun": []
        }
      },
      {
        "userId": 2,
        "nickname": "김철수",
        "profilePhoto": "https://example.com/profile2.jpg",
        "position": "VOCAL",
        "joinedAt": "2024-03-16T14:20:00",
        "timetableUpdatedAt": "2024-03-16T15:00:00",
        "isSubmitted": false,
        "timetableData": null
      }
    ]
  }
}
```

### 실패 응답
- **403**: 동아리 멤버가 아님
- **404**: 존재하지 않는 팀

---

## 4. 팀 이름 수정
### PATCH `/api/teams/{teamId}`

#### 요청
```bash
curl -X PATCH "http://localhost:8080/api/teams/1" \
  -H "Authorization: Bearer {JWT_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "수정된 팀 이름"
  }'
```

#### 응답 (200 OK)
```json
{
  "success": true,
  "message": "곡 팀 이름이 성공적으로 수정되었습니다.",
  "data": {
    "id": 1,
    "name": "수정된 팀 이름",
    "clubId": 1,
    "clubName": "락밴드 동아리",
    "creatorId": 1,
    "creatorName": "홍길동",
    "createdAt": "2024-03-15T10:30:00",
    "memberCount": 4
  }
}
```

### 실패 응답
- **400**: 중복된 팀 이름
- **403**: 팀장이 아님
- **404**: 존재하지 않는 팀

---

## 5. 팀 삭제
### DELETE `/api/teams/{teamId}`

#### 요청
```bash
curl -X DELETE "http://localhost:8080/api/teams/1" \
  -H "Authorization: Bearer {JWT_TOKEN}"
```

#### 응답 (200 OK)
```json
{
  "success": true,
  "message": "곡 팀이 성공적으로 삭제되었습니다.",
  "data": null
}
```

### 실패 응답
- **403**: 팀장이 아님
- **404**: 존재하지 않는 팀

---

## 6. 팀 탈퇴
### DELETE `/api/teams/{teamId}/members/me`

#### 요청
```bash
curl -X DELETE "http://localhost:8080/api/teams/1/members/me" \
  -H "Authorization: Bearer {JWT_TOKEN}"
```

#### 응답 (200 OK)
```json
{
  "success": true,
  "message": "팀에서 성공적으로 탈퇴했습니다.",
  "data": null
}
```

### 실패 응답
- **400**: 팀장은 탈퇴할 수 없음
- **404**: 존재하지 않는 팀 또는 팀 멤버가 아님

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
- `201 Created`: 생성 성공
- `400 Bad Request`: 잘못된 요청
- `401 Unauthorized`: 인증 실패
- `403 Forbidden`: 권한 없음
- `404 Not Found`: 리소스 없음
