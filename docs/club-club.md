# Club API

## 동아리 관리
모든 API는 JWT 인증 필요

---

## 1. 동아리 생성
```
POST /api/clubs
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json
```

### 요청 예시
```bash
curl -X POST "http://localhost:8080/api/clubs" \
  -H "Authorization: Bearer {JWT_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "락밴드 동아리",
    "description": "음악을 사랑하는 사람들의 모임",
    "universityId": 1,
    "chatroomUrl": "https://open.kakao.com/o/example",
    "instagramId": "rockband_club",
    "photoUrl": null
  }'
```

### 요청 필드
- `name` (string, 필수): 동아리 이름 (최대 100자)
- `description` (string, 선택): 동아리 설명
- `universityId` (integer, 선택): 대학교 ID (null인 경우 연합동아리)
- `chatroomUrl` (string, 선택): 카카오톡 채팅방 링크 (최대 255자)
- `instagramId` (string, 선택): 인스타그램 아이디 (최대 50자)
- `photoUrl` (string, 선택): 동아리 사진 URL

### 성공 응답 (201)
```json
{
  "success": true,
  "message": "동아리가 성공적으로 생성되었습니다",
  "data": {
    "id": 1,
    "name": "락밴드 동아리",
    "description": "음악을 사랑하는 사람들의 모임",
    "photoUrl": null,
    "universityName": "서울대학교",
    "isUnionClub": false,
    "memberCount": 1,
    "createdAt": "2024-03-15T10:30:00"
  }
}
```

### 실패 응답
- **400**: 필수 필드 누락 또는 중복된 동아리명
- **404**: 존재하지 않는 대학교 ID

---

## 2. 동아리 목록 조회
```
GET /api/clubs?page=0&size=5&sort=createdAt,desc
```

### 요청 예시
```bash
curl "http://localhost:8080/api/clubs?page=0&size=5"
```

### 쿼리 파라미터
- `page`: 페이지 번호 (기본값: 0)
- `size`: 페이지 크기 (기본값: 5)
- `sort`: 정렬 (기본값: createdAt,desc)

### 성공 응답 (200)
```json
{
  "success": true,
  "message": "동아리 목록 조회 성공",
  "data": {
    "content": [
      {
        "id": 1,
        "name": "락밴드 동아리",
        "universityName": "서울대학교",
        "isUnionClub": false,
        "photoUrl": "https://example.com/photo.jpg",
        "memberCount": 15
      }
    ],
    "pageInfo": {
      "page": 0,
      "size": 5,
      "totalElements": 1,
      "totalPages": 1,
              "first": true,
        "last": true,
        "empty": false
      }
  }
}
```

---

## 3. 동아리 상세 조회
```
GET /api/clubs/{clubId}
```

### 요청 예시
```bash
curl "http://localhost:8080/api/clubs/1"
```

### 성공 응답 (200)
```json
{
  "success": true,
  "message": "동아리 상세 정보 조회 성공",
  "data": {
    "id": 1,
    "name": "락밴드 동아리",
    "university": {
      "id": 1,
      "name": "서울대학교",
      "region": "서울특별시"
    },
    "isUnionClub": false,
    "chatroomUrl": "https://open.kakao.com/o/example",
    "description": "음악을 사랑하는 사람들의 모임",
    "instagramId": "rockband_club",
    "photoUrl": "https://example.com/photo.jpg",
    "memberCount": 15,
    "representativeId": 1,
    "createdAt": "2024-03-15T10:30:00",
    "updatedAt": "2024-03-15T10:30:00"
  }
}
```

### 실패 응답
- **404**: 존재하지 않는 동아리

---

## 4. 동아리 부원 명단 조회
```
GET /api/clubs/{clubId}/members
```

### 요청 예시
```bash
curl "http://localhost:8080/api/clubs/1/members"
```

### 성공 응답 (200)
```json
{
  "success": true,
  "message": "동아리 부원 명단 조회 성공",
  "data": {
    "id": 1,
    "members": [
      {
        "userId": 1,
        "name": "김철수",
        "position": "보컬"
      },
      {
        "userId": 2,
        "name": "이영희",
        "position": "기타"
      }
    ],
    "vocalCount": 1,
    "guitarCount": 1,
    "keyboardCount": 0,
    "bassCount": 0,
    "drumCount": 0,
    "totalMemberCount": 2
  }
}
```

---

## 5. 동아리 수정
```
PATCH /api/clubs/{clubId}
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json
```

### 요청 예시
```bash
curl -X PATCH "http://localhost:8080/api/clubs/1" \
  -H "Authorization: Bearer {JWT_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "수정된 동아리명",
    "description": "수정된 설명"
  }'
```

### 성공 응답 (200)
```json
{
  "success": true,
  "message": "동아리 정보가 성공적으로 수정되었습니다",
  "data": {
    "id": 1,
    "name": "수정된 동아리명",
    "description": "수정된 설명",
    "photoUrl": "https://example.com/photo.jpg",
    "universityName": "서울대학교",
    "isUnionClub": false,
    "memberCount": 15,
    "createdAt": "2024-03-15T10:30:00"
  }
}
```

### 실패 응답
- **403**: 동아리 대표자가 아님

---

## 6. 동아리 대표자 위임
```
PATCH /api/clubs/{clubId}/representative
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json
```

### 요청 예시
```bash
curl -X PATCH "http://localhost:8080/api/clubs/1/representative" \
  -H "Authorization: Bearer {JWT_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "newRepresentativeUserId": 2
  }'
```

### 요청 필드
- `newRepresentativeUserId`: 새로운 대표자로 지정할 사용자 ID

### 성공 응답 (200)
```json
{
  "success": true,
  "message": "동아리 대표자 권한이 성공적으로 위임되었습니다",
  "data": null
}
```

### 실패 응답
- **400**: 자기 자신에게 위임하는 경우
- **403**: 동아리 대표자가 아님
- **404**: 위임할 사용자가 동아리 멤버가 아님

---

## 7. 동아리 삭제
```
DELETE /api/clubs/{clubId}
Authorization: Bearer {JWT_TOKEN}
```

### 요청 예시
```bash
curl -X DELETE "http://localhost:8080/api/clubs/1" \
  -H "Authorization: Bearer {JWT_TOKEN}"
```

### 성공 응답 (200)
```json
{
  "success": true,
  "message": "동아리가 성공적으로 삭제되었습니다",
  "data": null
}
```

### 삭제 처리 내용
동아리 삭제 시 다음 항목들이 연쇄적으로 소프트 삭제됩니다:
- 동아리에 속한 모든 팀, 팀 멤버, 팀 일정 정보
- 동아리 멤버, 일정, 갤러리 사진들, 대표 사진 정보

### 실패 응답
- **403**: 동아리 대표자가 아님
- **404**: 존재하지 않는 동아리

---

## 8. 동아리 탈퇴
```
DELETE /api/clubs/{clubId}/members/me
Authorization: Bearer {JWT_TOKEN}
```

### 요청 예시
```bash
curl -X DELETE "http://localhost:8080/api/clubs/1/members/me" \
  -H "Authorization: Bearer {JWT_TOKEN}"
```

### 성공 응답 (200)
```json
{
  "success": true,
  "message": "동아리에서 성공적으로 탈퇴했습니다",
  "data": null
}
```

### 실패 응답
- **400**: 동아리 대표자는 탈퇴 불가 (먼저 대표자 권한 위임 필요)
- **404**: 동아리 멤버가 아님

---

## 9. 동아리 부원 강퇴
```
DELETE /api/clubs/{clubId}/members/{userId}
Authorization: Bearer {JWT_TOKEN}
```

### 요청 예시
```bash
curl -X DELETE "http://localhost:8080/api/clubs/1/members/2" \
  -H "Authorization: Bearer {JWT_TOKEN}"
```

### 성공 응답 (200)
```json
{
  "success": true,
  "message": "해당 부원이 성공적으로 강퇴되었습니다",
  "data": null
}
```

### 실패 응답
- **400**: 대표자를 강퇴하려는 경우
- **403**: 동아리 대표자가 아님
- **404**: 강퇴할 사용자가 동아리 멤버가 아님

---

## 10. 동아리 대표 사진 업로드
```
POST /api/clubs/{clubId}/main-image
Authorization: Bearer {JWT_TOKEN}
Content-Type: multipart/form-data
```

### 요청 예시
```bash
curl -X POST "http://localhost:8080/api/clubs/1/main-image" \
  -H "Authorization: Bearer {JWT_TOKEN}" \
  -F "image=@/path/to/image.jpg"
```

### 요청 필드
- `image`: 업로드할 이미지 파일

### 성공 응답 (200)
```