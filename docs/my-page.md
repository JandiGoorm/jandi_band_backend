# 마이페이지 API 문서

## 개요
사용자가 참가한 팀과 동아리 목록을 조회하는 API입니다.

## 1. 내가 참가한 동아리 목록 조회

### 요청
```
GET /api/v1/my/clubs
Authorization: Bearer {access_token}
```

### 응답
```json
{
  "success": true,
  "message": "내가 참가한 동아리 목록 조회 성공",
  "data": [
    {
      "id": 1,
      "name": "서울대학교 밴드동아리",
      "description": "서울대학교 최고의 밴드동아리입니다.",
      "photoUrl": "https://s3.amazonaws.com/club-photo/1.jpg",
      "universityName": "서울대학교",
      "isUnionClub": false,
      "myRole": "MEMBER",
      "joinedAt": "2024-01-15T10:30:00",
      "memberCount": 25
    },
    {
      "id": 2,
      "name": "연합 록밴드 동아리",
      "description": "여러 대학 학생들이 함께하는 록밴드 동아리",
      "photoUrl": null,
      "universityName": null,
      "isUnionClub": true,
      "myRole": "REPRESENTATIVE",
      "joinedAt": "2024-02-01T14:20:00",
      "memberCount": 42
    }
  ]
}
```

### 응답 필드 설명
- `id`: 동아리 ID
- `name`: 동아리 이름
- `description`: 동아리 설명
- `photoUrl`: 동아리 대표 사진 URL (없으면 null)
- `universityName`: 대학 이름 (연합동아리면 null)
- `isUnionClub`: 연합동아리 여부
- `myRole`: 내 권한 (`REPRESENTATIVE`, `MEMBER`)
- `joinedAt`: 동아리 가입 일시
- `memberCount`: 동아리 총 멤버 수

## 2. 내가 참가한 팀 목록 조회

### 요청
```
GET /api/v1/my/teams
Authorization: Bearer {access_token}
```

### 응답
```json
{
  "success": true,
  "message": "내가 참가한 팀 목록 조회 성공",
  "data": [
    {
      "id": 1,
      "name": "락밴드팀 A",
      "description": "록 음악을 주로 연주하는 팀입니다.",
      "clubId": 1,
      "clubName": "서울대학교 밴드동아리",
      "creatorId": 5,
      "creatorName": "김창수",
      "joinedAt": "2024-01-20T16:45:00",
      "createdAt": "2024-01-15T09:00:00",
      "memberCount": 4
    },
    {
      "id": 3,
      "name": "어쿠스틱 듀오",
      "description": "어쿠스틱 기타와 보컬로 구성된 듀오팀",
      "clubId": 2,
      "clubName": "연합 록밴드 동아리",
      "creatorId": 12,
      "creatorName": "이민수",
      "joinedAt": "2024-02-10T11:30:00",
      "createdAt": "2024-02-05T13:15:00",
      "memberCount": 2
    }
  ]
}
```

### 응답 필드 설명
- `id`: 팀 ID
- `name`: 팀 이름
- `description`: 팀 설명
- `clubId`: 소속 동아리 ID
- `clubName`: 소속 동아리 이름
- `creatorId`: 팀 생성자 ID
- `creatorName`: 팀 생성자 닉네임
- `joinedAt`: 팀 가입 일시
- `createdAt`: 팀 생성 일시
- `memberCount`: 팀 총 멤버 수

## cURL 예제

### 내가 참가한 동아리 목록 조회
```bash
curl -X GET "http://localhost:8080/api/v1/my/clubs" \
     -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

### 내가 참가한 팀 목록 조회
```bash
curl -X GET "http://localhost:8080/api/v1/my/teams" \
     -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

## 오류 응답

### 인증 오류 (401)
```json
{
  "success": false,
  "message": "토큰이 유효하지 않습니다.",
  "errorCode": "INVALID_TOKEN"
}
```

## 특이사항
1. **정렬 순서**: 
   - 동아리 목록: 가입일시 역순 (최근 가입한 것부터)
   - 팀 목록: 가입일시 역순 (최근 가입한 것부터)

2. **연합동아리 처리**: 
   - `universityName`이 `null`이고 `isUnionClub`이 `true`인 경우

3. **삭제된 동아리/팀 제외**: 
   - `deletedAt`이 설정된 동아리나 팀은 목록에서 제외됨

4. **권한**: 
   - JWT 토큰으로 인증된 사용자만 자신의 목록 조회 가능
   - 다른 사용자의 목록은 조회할 수 없음 