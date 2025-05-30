# Club API 명세서

## Base URL
`/api/clubs`

## 인증
생성, 수정, 삭제는 JWT 인증 필요 (Spring Security + @AuthenticationPrincipal CustomUserDetails). 조회는 인증 불필요.

## 권한 관리
- **생성**: 로그인한 모든 사용자 (생성자가 자동으로 대표자가 됨)
- **수정**: 동아리 멤버 또는 ADMIN 권한 사용자
- **삭제**: 동아리 대표자 또는 ADMIN 권한 사용자
- **대표자 위임**: 동아리 대표자 또는 ADMIN 권한 사용자
- **부원 강퇴**: 동아리 대표자 또는 ADMIN 권한 사용자
- **동아리 탈퇴**: 동아리 멤버 (단, 대표자는 탈퇴 불가)

---

## 1. 동아리 생성
### POST `/api/clubs`

#### 요청
```bash
curl -X POST "http://localhost:8080/api/clubs" \
  -H "Authorization: Bearer {JWT_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "락밴드 동아리",
    "universityId": 1,
    "chatroomUrl": "https://open.kakao.com/o/example",
    "description": "음악을 사랑하는 사람들이 모인 락밴드 동아리입니다.",
    "instagramId": "rockband_club"
  }'
```

#### 요청 필드
- `name` (string, 필수): 동아리 이름 (최대 100자)
- `universityId` (integer, 선택): 대학교 ID (null이면 연합동아리)
- `chatroomUrl` (string, 선택): 카카오톡 채팅방 링크 (최대 255자)
- `description` (string, 선택): 동아리 설명
- `instagramId` (string, 선택): 인스타그램 아이디 (최대 50자)

#### 응답 (201 Created)
```json
{
  "success": true,
  "message": "동아리가 성공적으로 생성되었습니다",
  "data": {
    "id": 1,
    "name": "락밴드 동아리",
    "university": {
      "id": 1,
      "name": "서울대학교"
    },
    "isUnionClub": false,
    "chatroomUrl": "https://open.kakao.com/o/example",
    "description": "음악을 사랑하는 사람들이 모인 락밴드 동아리입니다.",
    "instagramId": "rockband_club",
    "photoUrl": null,
    "memberCount": 1,
    "representativeId": 1,
    "createdAt": "2024-03-15T10:30:00",
    "updatedAt": "2024-03-15T10:30:00"
  }
}
```

---

## 2. 동아리 목록 조회
### GET `/api/clubs`

#### 요청
```bash
curl -X GET "http://localhost:8080/api/clubs?page=0&size=5"
```

#### 쿼리 파라미터
- `page` (integer): 페이지 번호 (기본값: 0)
- `size` (integer): 페이지 크기 (기본값: 5)

#### 응답 (200 OK)
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
        "photoUrl": null,
        "memberCount": 5
      }
    ],
    "pageInfo": {
      "totalElements": 1,
      "totalPages": 1,
      "first": true,
      "last": true,
      "size": 5,
      "number": 0
    }
  }
}
```

---

## 3. 동아리 상세 조회
### GET `/api/clubs/{clubId}`

#### 요청
```bash
curl -X GET "http://localhost:8080/api/clubs/1"
```

#### 응답 (200 OK)
```json
{
  "success": true,
  "message": "동아리 상세 정보 조회 성공",
  "data": {
    "id": 1,
    "name": "락밴드 동아리",
    "university": {
      "id": 1,
      "name": "서울대학교"
    },
    "isUnionClub": false,
    "chatroomUrl": "https://open.kakao.com/o/example",
    "description": "음악을 사랑하는 사람들이 모인 락밴드 동아리입니다.",
    "instagramId": "rockband_club",
    "photoUrl": null,
    "memberCount": 5,
    "representativeId": 1,
    "createdAt": "2024-03-15T10:30:00",
    "updatedAt": "2024-03-15T10:30:00"
  }
}
```

---

## 4. 동아리 부원 명단 조회
### GET `/api/clubs/{clubId}/members`

#### 요청
```bash
curl -X GET "http://localhost:8080/api/clubs/1/members"
```

#### 응답 (200 OK)
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

## 5. 동아리 정보 수정
### PATCH `/api/clubs/{clubId}`

#### 요청
```bash
curl -X PATCH "http://localhost:8080/api/clubs/1" \
  -H "Authorization: Bearer {JWT_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "수정된 동아리 이름",
    "universityId": 2,
    "description": "수정된 동아리 설명입니다.",
    "instagramId": "new_instagram_id"
  }'
```

#### 요청 필드 (모두 선택적)
- `name`: 동아리 이름 (최대 100자)
- `universityId`: 대학교 ID (null이면 연합동아리)
- `chatroomUrl`: 채팅방 URL (최대 255자)
- `description`: 동아리 설명
- `instagramId`: 인스타그램 ID (최대 50자)

#### 응답 (200 OK)
```json
{
  "success": true,
  "message": "동아리 정보가 성공적으로 수정되었습니다",
  "data": {
    "id": 1,
    "name": "수정된 동아리 이름",
    "university": {
      "id": 2,
      "name": "연세대학교"
    },
    "isUnionClub": false,
    "chatroomUrl": "https://open.kakao.com/o/example",
    "description": "수정된 동아리 설명입니다.",
    "instagramId": "new_instagram_id",
    "photoUrl": null,
    "memberCount": 5,
    "representativeId": 1,
    "createdAt": "2024-03-15T10:30:00",
    "updatedAt": "2024-03-15T11:00:00"
  }
}
```

---

## 6. 동아리 대표자 위임
### PATCH `/api/clubs/{clubId}/representative`

#### 요청
```bash
curl -X PATCH "http://localhost:8080/api/clubs/1/representative" \
  -H "Authorization: Bearer {JWT_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "newRepresentativeUserId": 2
  }'
```

#### 요청 필드
- `newRepresentativeUserId` (integer, 필수): 새로운 대표자로 지정할 사용자 ID

#### 응답 (200 OK)
```json
{
  "success": true,
  "message": "동아리 대표자 권한이 성공적으로 위임되었습니다",
  "data": null
}
```

#### 권한별 동작 방식
- **대표자가 실행**: 본인 사임 → 새 대표자 선임
- **ADMIN이 실행**: 기존 대표자 해임 → 새 대표자 선임 (ADMIN이 동아리 멤버가 아니어도 가능)

#### 에러 케이스
- 자기 자신에게 위임하는 경우: `400 Bad Request`
- 위임할 사용자가 동아리 멤버가 아닌 경우: `404 Not Found`
- 대표자나 ADMIN이 아닌 사용자가 요청하는 경우: `403 Forbidden`

---

## 7. 동아리 탈퇴
### DELETE `/api/clubs/{clubId}/members/me`

#### 요청
```bash
curl -X DELETE "http://localhost:8080/api/clubs/1/members/me" \
  -H "Authorization: Bearer {JWT_TOKEN}"
```

#### 응답 (200 OK)
```json
{
  "success": true,
  "message": "동아리에서 성공적으로 탈퇴했습니다",
  "data": null
}
```

#### 에러 케이스
- 동아리 멤버가 아닌 경우: `404 Not Found`
- 대표자가 탈퇴하려는 경우: `400 Bad Request` - "동아리 대표자는 탈퇴할 수 없습니다. 먼저 다른 멤버에게 대표자 권한을 위임해주세요."

---

## 8. 동아리 부원 강퇴
### DELETE `/api/clubs/{clubId}/members/{userId}`

#### 요청
```bash
curl -X DELETE "http://localhost:8080/api/clubs/1/members/2" \
  -H "Authorization: Bearer {JWT_TOKEN}"
```

#### 경로 파라미터
- `clubId` (integer): 동아리 ID
- `userId` (integer): 강퇴할 사용자 ID

#### 응답 (200 OK)
```json
{
  "success": true,
  "message": "해당 부원이 성공적으로 강퇴되었습니다",
  "data": null
}
```

#### 에러 케이스
- 대표자가 아닌 사용자가 요청하는 경우: `403 Forbidden`
- 강퇴할 사용자가 동아리 멤버가 아닌 경우: `404 Not Found`
- 대표자를 강퇴하려는 경우: `400 Bad Request` - "동아리 대표자를 강퇴할 수 없습니다. 먼저 권한을 위임해주세요."

---

## 9. 동아리 삭제
### DELETE `/api/clubs/{clubId}`

#### 요청
```bash
curl -X DELETE "http://localhost:8080/api/clubs/1" \
  -H "Authorization: Bearer {JWT_TOKEN}"
```

#### 응답 (200 OK)
```json
{
  "success": true,
  "message": "동아리가 성공적으로 삭제되었습니다",
  "data": null
}
```

#### 삭제 시 연관 데이터 처리
동아리 삭제 시 다음 리소스들이 순차적으로 소프트 삭제됩니다:

1. **동아리 대표 사진**: S3에서 물리적 삭제 (기본 이미지는 제외)
2. **ClubPhoto**: 동아리 대표 사진 DB 레코드 소프트 삭제
3. **ClubMember**: 모든 동아리 멤버 관계 소프트 삭제
4. **ClubEvent**: 동아리의 모든 일정 소프트 삭제
5. **Club**: 동아리 자체 소프트 삭제

#### 권한
- **동아리 대표자**: 해당 동아리 삭제 가능
- **ADMIN**: 모든 동아리 삭제 가능

---

## 10. 동아리 대표 사진 업로드
### POST `/api/clubs/{clubId}/main-image`

#### 요청
```bash
curl -X POST "http://localhost:8080/api/clubs/1/main-image" \
  -H "Authorization: Bearer {JWT_TOKEN}" \
  -H "Content-Type: multipart/form-data" \
  -F "image=@/path/to/image.jpg"
```

#### 요청 파라미터
- `image` (file, 필수): 업로드할 이미지 파일 (multipart/form-data)

#### 응답 (200 OK)
```json
{
  "success": true,
  "message": "동아리 대표 사진이 성공적으로 업로드되었습니다",
  "data": "https://example.com/images/club-photo.jpg"
}
```

---

## 11. 동아리 대표 사진 삭제
### DELETE `/api/clubs/{clubId}/main-image`

#### 요청
```bash
curl -X DELETE "http://localhost:8080/api/clubs/1/main-image" \
  -H "Authorization: Bearer {JWT_TOKEN}"
```

#### 응답 (200 OK)
```json
{
  "success": true,
  "message": "동아리 대표 사진이 성공적으로 삭제되었습니다",
  "data": null
}
```

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
- `409 Conflict`: 리소스 충돌

## 참고사항
- **연합동아리**: `universityId`가 null이면 연합동아리 (`isUnionClub: true`)
- **권한**: 생성은 모든 인증된 사용자, 수정은 동아리 멤버, 삭제는 동아리 대표자만 (ADMIN 권한은 모든 작업 가능)
- **자동 멤버 추가**: 동아리 생성자는 자동으로 대표자(REPRESENTATIVE)로 등록
- **소프트 삭제**: 실제 삭제가 아닌 deletedAt 설정 (동아리 탈퇴, 강퇴, 동아리 삭제 시 연관 데이터 포함)
- **연관 데이터 삭제**: 동아리 삭제 시 ClubMember, ClubEvent, ClubPhoto가 함께 소프트 삭제됨
- **페이지네이션**: 기본 크기 5개, PagedRespDTO 구조 사용
- **이미지 업로드**: 별도의 multipart/form-data 엔드포인트 사용
- **대표자 특징1**: 항상 동아리당 대표자 1명만 존재 (위임 시 기존 대표자 자동 해임)
- **대표자 특징2**: 대표자는 탈퇴 불가, 강퇴 안당함 (먼저 권한 위임 필요)
- **대표자 권한 위임**: 자기 자신에게는 위임 불가, 동아리 멤버에게만 위임 가능
- **ADMIN 특권**: 동아리 멤버가 아니어도 모든 관리 작업 가능 (시스템 관리 목적)

### ADMIN이 할 수 있는 기능
- ✅ **동아리 정보 수정**: 동아리 멤버가 아니어도 수정 가능
- ✅ **동아리 삭제**: 동아리 대표자가 아니어도 삭제 가능
- ✅ **대표자 위임**: 동아리 대표자가 아니어도 위임 가능 (기존 대표자 자동 해임)
- ✅ **부원 강퇴**: 동아리 대표자가 아니어도 강퇴 가능 (단, 존재하는 멤버만)
- ✅ **동아리 사진 업로드/삭제**: 동아리 멤버가 아니어도 가능
- ❌ **동아리 탈퇴**: ADMIN도 동아리 멤버인 경우에만 탈퇴 가능
