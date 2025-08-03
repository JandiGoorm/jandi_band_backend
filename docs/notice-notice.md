# Notice API

## 공지사항 관리
팝업 형태로 노출되는 공지사항 관리 시스템

---

## 1. 현재 활성 공지사항 조회 (팝업용)
```
GET /api/notices/active
```

> **응답 DTO**: `NoticeRespDTO`

### 요청 예시
```bash
curl "http://localhost:8080/api/notices/active"
```

### 성공 응답 (200)
```json
{
  "success": true,
  "message": "활성 공지사항 조회 성공",
  "data": [
    {
      "id": 1,
      "title": "시스템 점검 안내",
      "content": "오늘 밤 12시부터 새벽 2시까지 시스템 점검이 있습니다.",
      "startDatetime": "2024-12-10T00:00:00",
      "endDatetime": "2024-12-10T23:59:59",
      "isPaused": false,
      "imageUrl": "https://example.com/notice-photo/image.jpg"
    }
  ]
}
```

### 활성 공지사항 조건
- `deletedAt` 값이 null (삭제되지 않음)
- `isPaused` 값이 false (일시정지되지 않음)
- `startDatetime` ≤ 현재시각 ≤ `endDatetime` (노출 기간 내)

---

## 2. 공지사항 목록 조회 (관리자 전용)
```
GET /api/notices?page=0&size=20&sort=createdAt,desc
Authorization: Bearer {JWT_TOKEN}
```

> **응답 DTO**: `NoticeRespDTO`

### 요청 예시
```bash
curl "http://localhost:8080/api/notices?page=0&size=20" \
  -H "Authorization: Bearer {JWT_TOKEN}"
```

### 쿼리 파라미터
- `page`: 페이지 번호 (기본값: 0)
- `size`: 페이지 크기 (기본값: 20)
- `sort`: 정렬 (기본값: createdAt,desc)

### 성공 응답 (200)
```json
{
  "success": true,
  "message": "공지사항 목록 조회 성공",
  "data": {
    "content": [
      {
        "id": 1,
        "title": "시스템 점검 안내",
        "content": "오늘 밤 12시부터 새벽 2시까지 시스템 점검이 있습니다.",
        "startDatetime": "2024-12-10T00:00:00",
        "endDatetime": "2024-12-10T23:59:59",
        "isPaused": false,
        "imageUrl": "https://example.com/notice-photo/image.jpg"
      }
    ],
    "pageInfo": {
      "page": 0,
      "size": 20,
      "totalElements": 1,
      "totalPages": 1,
      "first": true,
      "last": true,
      "empty": false
    }
  }
}
```

### 실패 응답
- **401**: 인증되지 않은 사용자
- **403**: 관리자 권한 없음

---

## 3. 공지사항 상세 조회 (관리자 전용)
```
GET /api/notices/{noticeId}
Authorization: Bearer {JWT_TOKEN}
```

> **응답 DTO**: `NoticeDetailRespDTO`

### 요청 예시
```bash
curl "http://localhost:8080/api/notices/1" \
  -H "Authorization: Bearer {JWT_TOKEN}"
```

### 성공 응답 (200)
```json
{
  "success": true,
  "message": "공지사항 상세 조회 성공",
  "data": {
    "id": 1,
    "title": "시스템 점검 안내",
    "content": "오늘 밤 12시부터 새벽 2시까지 시스템 점검이 있습니다.",
    "startDatetime": "2024-12-10T00:00:00",
    "endDatetime": "2024-12-10T23:59:59",
    "isPaused": false,
    "creatorId": 1,
    "creatorName": "관리자",
    "imageUrl": "https://example.com/notice-photo/image.jpg",
    "createdAt": "2024-12-09T10:30:00",
    "updatedAt": "2024-12-09T15:45:00",
    "deletedAt": null
  }
}
```

### 실패 응답
- **404**: 존재하지 않는 공지사항 또는 삭제된 공지사항

---

## 4. 공지사항 생성 (관리자 전용)
```
POST /api/notices
Authorization: Bearer {JWT_TOKEN}
Content-Type: multipart/form-data
```

> **응답 DTO**: `NoticeDetailRespDTO`

### 요청 예시
```bash
curl -X POST "http://localhost:8080/api/notices" \
  -H "Authorization: Bearer {JWT_TOKEN}" \
  -F "title=시스템 점검 안내" \
  -F "content=오늘 밤 12시부터 새벽 2시까지 시스템 점검이 있습니다." \
  -F "startDatetime=2024-12-10T00:00:00" \
  -F "endDatetime=2024-12-10T23:59:59" \
  -F "image=@/path/to/image.jpg"
```

### 요청 필드
- `title` (string, 필수): 공지사항 제목 (최대 255자)
- `content` (string, 필수): 공지사항 내용
- `startDatetime` (datetime, 필수): 팝업 노출 시작 시각
- `endDatetime` (datetime, 필수): 팝업 노출 종료 시각
- `isPaused` (boolean, 선택): 일시정지 여부 (생략 시 자동으로 false 설정)
- `image` (file, 선택): 첨부 이미지 파일

### 성공 응답 (201)
```json
{
  "success": true,
  "message": "공지사항이 성공적으로 생성되었습니다",
  "data": {
    "id": 1,
    "title": "시스템 점검 안내",
    "content": "오늘 밤 12시부터 새벽 2시까지 시스템 점검이 있습니다.",
    "startDatetime": "2024-12-10T00:00:00",
    "endDatetime": "2024-12-10T23:59:59",
    "isPaused": false,
    "creatorId": 1,
    "creatorName": "관리자",
    "imageUrl": "https://example.com/notice-photo/image.jpg",
    "createdAt": "2024-12-09T10:30:00",
    "updatedAt": "2024-12-09T10:30:00",
    "deletedAt": null
  }
}
```

### 실패 응답
- **400**: 필수 필드 누락 또는 종료 시각이 시작 시각보다 이른 경우
- **403**: 관리자 권한 없음

### 이미지 업로드 참고사항
- 이미지는 S3에 `notice-photo` 폴더에 저장됩니다
- 지원하는 이미지 형식: JPG, PNG, GIF 등 일반적인 이미지 포맷
- 이미지 업로드 실패 시 전체 공지사항 생성이 롤백됩니다
- `image` 필드를 생략하면 `imageUrl`은 `null`로 설정됩니다

---

## 5. 공지사항 수정 (관리자 전용)
```
PATCH /api/notices/{noticeId}
Authorization: Bearer {JWT_TOKEN}
Content-Type: multipart/form-data
```

> **응답 DTO**: `NoticeDetailRespDTO`

### 요청 예시
```bash
# 제목과 내용만 수정
curl -X PATCH "http://localhost:8080/api/notices/1" \
  -H "Authorization: Bearer {JWT_TOKEN}" \
  -F "title=수정된 점검 안내" \
  -F "content=수정된 내용입니다."

# 이미지만 변경
curl -X PATCH "http://localhost:8080/api/notices/1" \
  -H "Authorization: Bearer {JWT_TOKEN}" \
  -F "image=@/path/to/new-image.jpg"

# 시작/종료 시각만 변경
curl -X PATCH "http://localhost:8080/api/notices/1" \
  -H "Authorization: Bearer {JWT_TOKEN}" \
  -F "endDatetime=2024-12-11T23:59:59"

# 이미지 삭제
curl -X PATCH "http://localhost:8080/api/notices/1" \
  -H "Authorization: Bearer {JWT_TOKEN}" \
  -F "deleteImage=true"
```

### 성공 응답 (200)
```json
{
  "success": true,
  "message": "공지사항이 성공적으로 수정되었습니다",
  "data": {
    "id": 1,
    "title": "수정된 점검 안내",
    "content": "수정된 내용입니다.",
    "startDatetime": "2024-12-10T00:00:00",
    "endDatetime": "2024-12-10T23:59:59",
    "isPaused": false,
    "creatorId": 1,
    "creatorName": "관리자",
    "imageUrl": "https://example.com/notice-photo/new-image.jpg",
    "createdAt": "2024-12-09T10:30:00",
    "updatedAt": "2024-12-09T16:20:00",
    "deletedAt": null
  }
}
```

### 요청 필드 (모두 선택적)
- `title` (string, 선택): 공지사항 제목 (최대 255자)
- `content` (string, 선택): 공지사항 내용
- `startDatetime` (datetime, 선택): 팝업 노출 시작 시각
- `endDatetime` (datetime, 선택): 팝업 노출 종료 시각
- `image` (file, 선택): 새로운 첨부 이미지 파일
- `deleteImage` (boolean, 선택): 이미지 삭제 여부 (true로 설정 시 기존 이미지 삭제)

### 부분 수정 특징
- **모든 필드가 선택적입니다** - 변경하고 싶은 필드만 포함하여 요청
- 포함되지 않은 필드는 기존 값이 유지됩니다
- 빈 문자열이나 null 값으로 필드를 비울 수 없습니다 (제목의 경우 빈 문자열 전송 시 에러)

### 실패 응답
- **400**:
  - 빈 제목 (공백만 있는 경우)
  - 종료 시각이 시작 시각보다 이른 경우
  - 이미지 파일 크기가 10MB 초과
  - 이미지가 아닌 파일 업로드
- **403**: 관리자 권한 없음
- **404**: 존재하지 않는 공지사항

### 이미지 처리 참고사항
- **이미지 삭제**: `deleteImage=true`로 설정하여 기존 이미지 제거 가능
- **이미지 교체**: 새 이미지 파일을 전송하면 기존 이미지는 자동 삭제
- **동시 요청 시**: `deleteImage=true`와 새 이미지를 동시에 보내면 새 이미지 업로드가 우선
- **일시정지 상태(`isPaused`)는 수정되지 않습니다** - 별도의 토글 API 사용
- 이미지 크기는 10MB로 제한되며, 이미지 파일만 업로드 가능

---

## 6. 공지사항 삭제 (관리자 전용)
```
DELETE /api/notices/{noticeId}
Authorization: Bearer {JWT_TOKEN}
```

### 요청 예시
```bash
curl -X DELETE "http://localhost:8080/api/notices/1" \
  -H "Authorization: Bearer {JWT_TOKEN}"
```

### 성공 응답 (200)
```json
{
  "success": true,
  "message": "공지사항이 성공적으로 삭제되었습니다",
  "data": null
}
```

### 삭제 처리 방식
- **소프트 삭제**: `deletedAt` 필드에 삭제 시각 설정
- 삭제된 공지사항은 모든 조회 API에서 제외됨
- 팝업 노출에서도 자동으로 제외됨
- **첨부 이미지도 S3에서 자동으로 삭제됩니다**

### 실패 응답
- **403**: 관리자 권한 없음
- **404**: 존재하지 않는 공지사항

---

## 7. 공지사항 일시정지/재개 토글 (관리자 전용)
```
PATCH /api/notices/{noticeId}/toggle-pause
Authorization: Bearer {JWT_TOKEN}
```

> **응답 DTO**: `NoticeRespDTO`

### 요청 예시
```bash
curl -X PATCH "http://localhost:8080/api/notices/1/toggle-pause" \
  -H "Authorization: Bearer {JWT_TOKEN}"
```

### 성공 응답 (200)
```json
{
  "success": true,
  "message": "공지사항 일시정지 상태가 성공적으로 변경되었습니다",
  "data": {
    "id": 1,
    "title": "시스템 점검 안내",
    "content": "오늘 밤 12시부터 새벽 2시까지 시스템 점검이 있습니다.",
    "startDatetime": "2024-12-10T00:00:00",
    "endDatetime": "2024-12-10T23:59:59",
    "isPaused": true,
    "imageUrl": "https://example.com/notice-photo/image.jpg"
  }
}
```

### 기능 설명
- 현재 `isPaused` 값을 반전시킴 (true ↔ false)
- 일시정지된 공지사항은 팝업에 노출되지 않음
- 노출 기간 내에서도 일시정지 상태에 따라 제어 가능

### 실패 응답
- **403**: 관리자 권한 없음
- **404**: 존재하지 않는 공지사항

---

## 권한 관리

### 관리자 권한 검증
모든 관리자 전용 API는 다음 조건을 확인합니다:
- JWT 토큰의 유효성
- 사용자의 `adminRole`이 `ADMIN`인지 확인

### 권한 없음 시 응답
```json
{
  "success": false,
  "message": "관리자만 접근할 수 있습니다.",
  "data": null
}
```

---

## 비즈니스 로직

### 활성 공지사항 조건
1. **소프트 삭제 확인**: `deletedAt IS NULL`
2. **일시정지 확인**: `isPaused = false`
3. **노출 기간 확인**: `startDatetime ≤ 현재시각 ≤ endDatetime`

### 시간 검증
- 종료 시각은 시작 시각보다 늦어야 함
- 시각 정보는 `LocalDateTime` 형식으로 처리

### 이미지 관리
- **업로드**: S3의 `notice-photo` 폴더에 저장
- **교체**: 새 이미지 업로드 후 기존 이미지 삭제
- **삭제**: 공지사항 삭제 시 첨부 이미지도 함께 삭제
- **롤백**: DB 저장 실패 시 업로드된 이미지 자동 삭제
- **오류 처리**: 이미지 삭제 실패는 경고 로그로 기록 (서비스 중단하지 않음)

### 로깅 정책
- 공지사항 생성, 수정, 삭제, 상태 변경 시 로그 기록
- 제목은 50자로 제한하여 로깅 (민감정보 보호)
- 이미지 처리 오류는 별도 경고 로그 생성

---

## 에러 응답

### 표준 에러 형식
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
- `400 Bad Request`: 잘못된 요청 (필수 필드 누락, 시간 범위 오류, 이미지 업로드 실패)
- `401 Unauthorized`: 인증 실패
- `403 Forbidden`: 권한 부족 (관리자 아님)
- `404 Not Found`: 리소스 없음 (공지사항 없음, 삭제된 공지사항)
- `500 Internal Server Error`: 서버 오류

---

## 응답 DTO 구조

### NoticeRespDTO (간소화 버전)
팝업용, 목록 조회, 일시정지 토글에서 사용되는 기본 응답 DTO

**필드:**
- `id` (Integer): 공지사항 ID
- `title` (String): 공지사항 제목
- `content` (String): 공지사항 내용
- `startDatetime` (LocalDateTime): 팝업 노출 시작 시각
- `endDatetime` (LocalDateTime): 팝업 노출 종료 시각
- `isPaused` (Boolean): 일시정지 여부
- `imageUrl` (String): 첨부 이미지 URL (null 가능)

**사용 API:**
- `GET /api/notices/active` (현재 활성 공지사항 조회)
- `GET /api/notices` (공지사항 목록 조회)
- `PATCH /api/notices/{noticeId}/toggle-pause` (일시정지/재개 토글)

### NoticeDetailRespDTO (상세 버전)
관리자용 상세 조회, 생성, 수정에서 사용되는 상세 응답 DTO

**추가 필드 (NoticeRespDTO 포함):**
- `creatorId` (Integer): 작성자 ID
- `creatorName` (String): 작성자명
- `createdAt` (LocalDateTime): 생성일시
- `updatedAt` (LocalDateTime): 수정일시
- `deletedAt` (LocalDateTime): 삭제일시

**사용 API:**
- `GET /api/notices/{noticeId}` (공지사항 상세 조회)
- `POST /api/notices` (공지사항 생성)
- `PATCH /api/notices/{noticeId}` (공지사항 수정)

---

## 데이터베이스 스키마

### notice 테이블
```sql
CREATE TABLE notice (
    notice_id INT PRIMARY KEY AUTO_INCREMENT,
    creator_user_id INT NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    start_datetime DATETIME NOT NULL,
    end_datetime DATETIME NOT NULL,
    is_paused BOOLEAN NOT NULL DEFAULT FALSE,
    image_url VARCHAR(512) NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    deleted_at DATETIME NULL,
    FOREIGN KEY (creator_user_id) REFERENCES users(user_id)
);
```
