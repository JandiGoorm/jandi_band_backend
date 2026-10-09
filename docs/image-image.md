# Image API

## 이미지 관리
JWT 인증 + ADMIN 권한 필요

---

## 1. 이미지 업로드
```
POST /api/images/upload
Authorization: Bearer {JWT_TOKEN}
Content-Type: multipart/form-data
```

### 요청 예시
```bash
curl -X POST "http://localhost:8080/api/images/upload" \
  -H "Authorization: Bearer {JWT_TOKEN}" \
  -F "file=@/path/to/image.jpg" \
  -F "dirName=profiles"
```

### 요청 필드
- `file`: 업로드할 이미지 파일
- `dirName`: 저장할 디렉토리 이름

### 성공 응답 (200)
```json
{
  "success": true,
  "message": "이미지 업로드 성공",
  "data": "https://images.example.com/profiles/image-uuid.jpg"
}
```

### 응답 필드
- `data`: 업로드된 이미지의 URL

### 실패 응답
- **401**: 인증 실패
- **403**: ADMIN 권한 없음
- **400**: 지원하지 않는 파일 형식
- **413**: 파일 크기 초과

---

## 2. 이미지 삭제
```
DELETE /api/images?fileUrl={IMAGE_URL}
Authorization: Bearer {JWT_TOKEN}
```

### 요청 예시
```bash
curl -X DELETE "http://localhost:8080/api/images?fileUrl=https://images.example.com/profiles/image-uuid.jpg" \
  -H "Authorization: Bearer {JWT_TOKEN}"
```

### 쿼리 파라미터
- `fileUrl`: 삭제할 이미지 URL

### 성공 응답 (200)
```json
{
  "success": true,
  "message": "이미지 삭제 성공",
  "data": null
}
```

### 실패 응답
- **401**: 인증 실패
- **403**: ADMIN 권한 없음
- 없는 파일과 외부·이전 URL·공용 기본 이미지 삭제는 성공 응답으로 끝나며 파일을 변경하지 않는다.

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
- `403 Forbidden`: 권한 없음 (ADMIN 아님)
- `404 Not Found`: 리소스 없음
- `413 Payload Too Large`: 파일 크기 초과

## 지원 형식
- JPG, JPEG, PNG, GIF, WebP
- 최대 크기: 10MiB, 최대 4천만 픽셀
- 확장자·Content-Type·실제 이미지 형식 일치 및 첫 프레임 디코딩 확인
- 용도: 프로필 사진, 동아리 사진, 홍보글 이미지

## 참고사항
- **권한**: ADMIN 권한을 가진 사용자만 접근 가능
- **디렉토리**: dirName 파라미터로 R2 내 저장 경로 지정
- **파일명**: UUID로 자동 생성되어 중복 방지
- **CDN**: Cloudflare R2 공개 이미지 도메인
