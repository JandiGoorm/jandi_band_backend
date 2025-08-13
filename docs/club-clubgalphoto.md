# Club Gallery Photo API

## Base URL
`/api/clubs/{clubId}/photo`

## 인증
JWT 인증 필요 (Spring Security + @AuthenticationPrincipal CustomUserDetails)

## 권한 관리
- **조회**: 동아리 멤버만 가능
- **생성/수정/삭제**: 동아리 멤버만 가능
- **핀 등록/해제**: 동아리 멤버만 가능

---

## 1. 동아리 사진 목록 조회
### GET `/api/clubs/{clubId}/photo`

#### 경로 파라미터
- `clubId` (integer, 필수): 동아리 ID

#### 요청
```bash
curl -X GET "http://localhost:8080/api/clubs/1/photo?page=0&size=5" \
  -H "Authorization: Bearer {JWT_TOKEN}"
```

#### 쿼리 파라미터
- `page` (integer): 페이지 번호 (기본값: 0)
- `size` (integer): 페이지 크기 (기본값: 5)

#### 응답 (200 OK)
```json
{
  "success": true,
  "message": "동아리 사진 목록 조회 성공",
  "data": {
    "content": [
      {
        "id": 1,
        "imageUrl": "https://example.com/photos/club1_photo1.jpg",
        "isPinned": true,
        "isPublic": true
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

## 2. 동아리 사진 상세 조회
### GET `/api/clubs/{clubId}/photo/{photoId}`

#### 경로 파라미터
- `clubId` (integer, 필수): 동아리 ID
- `photoId` (integer, 필수): 사진 ID

#### 요청
```bash
curl -X GET "http://localhost:8080/api/clubs/1/photo/1" \
  -H "Authorization: Bearer {JWT_TOKEN}"
```

#### 응답 (200 OK)
```json
{
  "success": true,
  "message": "동아리 사진 상세 조회 성공",
  "data": {
    "id": 1,
    "imageUrl": "https://example.com/photos/club1_photo1.jpg",
    "description": "동아리 정기 공연 사진입니다. 모든 멤버가 함께한 소중한 순간을 담았습니다.",
    "isPinned": true,
    "isPublic": true,
    "uploader": "홍길동",
    "uploadedAt": "2024-03-15T10:30:00"
  }
}
```

---

## 3. 동아리 사진 업로드
### POST `/api/clubs/{clubId}/photo`

#### Content-Type
`multipart/form-data`

#### 경로 파라미터
- `clubId` (integer, 필수): 동아리 ID

#### 요청
```bash
curl -X POST "http://localhost:8080/api/clubs/1/photo" \
  -H "Authorization: Bearer {JWT_TOKEN}" \
  -F "image=@/path/to/photo.jpg" \
  -F "description=동아리 정기 공연 사진" \
  -F "isPublic=true"
```

#### 요청 필드
- `image` (file, 필수): 업로드할 이미지 파일 (JPG/PNG 등)
- `description` (string, 선택): 사진 설명 (기본값: 빈 문자열)
- `isPublic` (boolean, 선택): 공개 여부 (기본값: true)

#### 응답 (200 OK)
```json
{
  "success": true,
  "message": "동아리 사진 생성 성공",
  "data": {
    "id": 1,
    "imageUrl": "https://example.com/photos/club1_photo1.jpg",
    "isPinned": false,
    "isPublic": true
  }
}
```

---

## 4. 동아리 사진 수정
### PATCH `/api/clubs/{clubId}/photo/{photoId}`

#### Content-Type
`multipart/form-data`

#### 경로 파라미터
- `clubId` (integer, 필수): 동아리 ID
- `photoId` (integer, 필수): 수정할 사진 ID

#### 요청
```bash
curl -X PATCH "http://localhost:8080/api/clubs/1/photo/1" \
  -H "Authorization: Bearer {JWT_TOKEN}" \
  -F "description=수정된 사진 설명" \
  -F "isPublic=false"
```

#### 요청 필드 (모든 필드 선택사항)
- `image` (file, 선택): 새 이미지 파일 (업로드 시 기존 이미지 교체)
- `description` (string, 선택): 사진 설명
- `isPublic` (boolean, 선택): 공개 여부

#### 응답 (200 OK)
```json
{
  "success": true,
  "message": "동아리 사진 수정 성공",
  "data": {
    "id": 1,
    "imageUrl": "https://example.com/photos/club1_photo1.jpg",
    "description": "수정된 사진 설명",
    "isPinned": false,
    "isPublic": false,
    "uploader": "홍길동",
    "uploadedAt": "2024-03-15T10:30:00"
  }
}
```

---

## 5. 동아리 사진 핀 등록/해제
### PATCH `/api/clubs/{clubId}/photo/{photoId}/pin`

#### 경로 파라미터
- `clubId` (integer, 필수): 동아리 ID
- `photoId` (integer, 필수): 핀 설정할 사진 ID

#### 요청
```bash
curl -X PATCH "http://localhost:8080/api/clubs/1/photo/1/pin" \
  -H "Authorization: Bearer {JWT_TOKEN}"
```

#### 응답 (200 OK) - 핀 등록 성공
```json
{
  "success": true,
  "message": "고정되었습니다.",
  "data": null
}
```

#### 응답 (200 OK) - 핀 해제 성공
```json
{
  "success": true,
  "message": "고정 해제되었습니다.",
  "data": null
}
```

**참고**: 토글 방식으로 동작하며, 현재 핀 상태에 따라 등록/해제가 자동으로 처리됩니다.

---

## 6. 동아리 사진 삭제
### DELETE `/api/clubs/{clubId}/photo/{photoId}`

#### 경로 파라미터
- `clubId` (integer, 필수): 동아리 ID
- `photoId` (integer, 필수): 삭제할 사진 ID

#### 요청
```bash
curl -X DELETE "http://localhost:8080/api/clubs/1/photo/1" \
  -H "Authorization: Bearer {JWT_TOKEN}"
```

#### 응답 (200 OK)
```json
{
  "success": true,
  "message": "동아리 사진 삭제 성공",
  "data": null
}
```

---

## 에러 응답

### 400 Bad Request - 잘못된 요청
```json
{
  "success": false,
  "message": "필수 필드가 누락되었습니다.",
  "data": null
}
```
**발생 케이스**: 필수 필드 누락, 지원하지 않는 이미지 형식

### 401 Unauthorized - 인증 실패
```json
{
  "success": false,
  "message": "유효하지 않은 토큰입니다.",
  "data": null
}
```
**발생 케이스**: JWT 토큰이 없거나 유효하지 않은 경우

### 403 Forbidden - 권한 없음
```json
{
  "success": false,
  "message": "동아리 멤버만 접근할 수 있습니다.",
  "data": null
}
```
**발생 케이스**: 동아리 멤버가 아닌 사용자의 접근 시도

### 404 Not Found - 리소스 없음
```json
{
  "success": false,
  "message": "해당 동아리 사진을 찾을 수 없습니다.",
  "data": null
}
```
**발생 케이스**: 존재하지 않는 동아리 ID 또는 사진 ID

---

## 데이터 모델

### ClubGalPhotoReqDTO (요청)
```typescript
interface ClubGalPhotoReqDTO {
  image?: File;          // 이미지 파일 (생성 시 필수, 수정 시 선택)
  description?: string;  // 사진 설명 (기본값: 빈 문자열)
  isPublic?: boolean;    // 공개 여부 (기본값: true)
}
```

### ClubGalPhotoRespDTO (응답 - 목록용)
```typescript
interface ClubGalPhotoRespDTO {
  id: number;            // 사진 ID
  imageUrl: string;      // 이미지 URL
  isPinned: boolean;     // 핀 등록 여부
  isPublic: boolean;     // 공개 여부
}
```

### ClubGalPhotoRespDetailDTO (응답 - 상세용)
```typescript
interface ClubGalPhotoRespDetailDTO {
  id: number;            // 사진 ID
  imageUrl: string;      // 이미지 URL
  description: string;   // 사진 설명
  isPinned: boolean;     // 핀 등록 여부
  isPublic: boolean;     // 공개 여부
  uploader: string;      // 업로더명
  uploadedAt: string;    // 생성일시 (ISO 8601)
}
```

---

## 참고 사항
- **동아리 멤버십**: 모든 API는 동아리 멤버만 접근 가능
- **이미지 형식**: JPG, PNG 등 일반적인 이미지 형식 지원
- **이미지 교체**: 수정 시 새 이미지 업로드하면 기존 이미지 자동 교체
- **부분 수정**: PATCH 방식으로 필요한 필드만 전송하면 나머지는 기존 값 유지
- **핀 기능**: 중요한 사진을 상단에 고정하는 기능 (토글 방식)
- **공개 설정**: isPublic 필드로 사진의 공개/비공개 설정 가능
- **소프트 삭제**: 삭제된 사진은 실제로는 숨김 처리되어 복구 가능
- **권한 확인**: 사진 수정/삭제는 업로더 본인 또는 동아리 대표만 가능, 핀 등록/해제는 동아리 대표만 가능 