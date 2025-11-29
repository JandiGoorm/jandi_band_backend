# Promo API

## Base URL
`/api/promos`

## 인증
생성, 수정, 삭제는 JWT 인증 필요 (Spring Security + @AuthenticationPrincipal CustomUserDetails). 조회/검색은 인증 불필요.

## 페이지네이션 응답 구조
모든 목록 조회 API는 다음과 같은 페이지네이션 구조를 사용합니다:

```json
{
  "success": true,
  "message": "응답 메시지",
  "data": {
    "content": [...],  // 실제 데이터 배열
    "pageInfo": {
      "page": 0,           // 현재 페이지 번호 (0부터 시작)
      "size": 20,          // 페이지 크기
      "totalElements": 100, // 총 요소 수
      "totalPages": 5,     // 총 페이지 수
      "first": true,       // 첫 번째 페이지 여부
      "last": false,       // 마지막 페이지 여부
      "empty": false       // 비어있는 페이지 여부
    }
  }
}
```

---

## 1. 공연 홍보 목록 조회
### GET `/api/promos`

#### 요청
```bash
curl -X GET "http://localhost:8080/api/promos?page=0&size=20&sort=createdAt,desc"
```

#### 쿼리 파라미터
- `page` (integer): 페이지 번호 (기본값: 0)
- `size` (integer): 페이지 크기 (기본값: 20)
- `sort` (string): 정렬 기준 (기본값: "createdAt,desc")

#### 응답 (200 OK)
```json
{
  "success": true,
  "message": "공연 홍보 목록 조회 성공",
  "data": {
    "content": [
      {
        "id": 1,
        "teamName": "락밴드 팀",
        "creatorId": 1,
        "creatorName": "홍길동",
        "title": "락밴드 정기공연",
        "admissionFee": 10000,
        "eventDatetime": "2024-03-15T19:00:00",
        "location": "홍대 클럽",
        "address": "서울시 마포구 홍익로 123",
        "latitude": 37.5563,
        "longitude": 126.9236,
        "description": "락밴드 팀의 정기 공연입니다.",
        "viewCount": 100,
        "commentCount": 5,
        "likeCount": 20,
        "isLikedByUser": true,
        "createdAt": "2024-03-01T10:00:00",
        "updatedAt": "2024-03-01T10:00:00",
        "photoUrls": ["https://example.com/photo.jpg"]
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

---

## 2. 공연 홍보 상세 조회
### GET `/api/promos/{promoId}`

#### 요청
```bash
curl -X GET "http://localhost:8080/api/promos/1"
```

#### 응답 (200 OK)
```json
{
  "success": true,
  "message": "공연 홍보 상세 조회 성공",
  "data": {
    "id": 1,
    "teamName": "락밴드 팀",
    "creatorId": 1,
    "creatorName": "홍길동",
    "title": "락밴드 정기공연",
    "admissionFee": 10000,
    "eventDatetime": "2024-03-15T19:00:00",
    "location": "홍대 클럽",
    "address": "서울시 마포구 홍익로 123",
    "latitude": 37.5563,
    "longitude": 126.9236,
    "description": "락밴드 팀의 정기 공연입니다.",
    "viewCount": 100,
    "commentCount": 5,
    "likeCount": 20,
    "isLikedByUser": true,
    "createdAt": "2024-03-01T10:00:00",
    "updatedAt": "2024-03-01T10:00:00",
    "photoUrls": ["https://example.com/photo.jpg"]
  }
}
```

#### 응답 필드
- `teamName` (string): 팀명 (모든 팀에 있음)
- `latitude` (decimal): 위도 좌표
- `longitude` (decimal): 경도 좌표
- `photoUrls` (array): 공연 이미지 URL 목록 (최대 1개)
- `isLikedByUser` (boolean): 현재 사용자의 좋아요 상태 (true: 좋아요 누름, false: 좋아요 안 누름, null: 인증되지 않은 사용자)

---

## 3. 공연 홍보 생성
### POST `/api/promos`

#### Content-Type
`multipart/form-data`

#### 요청
```bash
curl -X POST "http://localhost:8080/api/promos" \
  -H "Authorization: Bearer {JWT_TOKEN}" \
  -F "teamName=락밴드 팀" \
  -F "title=락밴드 정기공연" \
  -F "admissionFee=10000" \
  -F "eventDatetime=2024-03-15T19:00:00" \
  -F "location=홍대 클럽" \
  -F "address=서울시 마포구 홍익로 123" \
  -F "latitude=37.5563" \
  -F "longitude=126.9236" \
  -F "description=락밴드 팀의 정기 공연입니다." \
  -F "image=@/path/to/image.jpg"
```

#### 요청 필드
- `teamName` (string, 필수): 팀명 (최대 255자)
- `title` (string, 필수): 공연 제목 (최대 255자)
- `admissionFee` (decimal, 선택): 입장료 (0 이상의 값, 음수 불가)
- `eventDatetime` (string, 선택): 공연 일시 (ISO 8601)
- `location` (string, 선택): 장소명 (최대 255자)
- `address` (string, 선택): 상세 주소 (최대 255자)
- `latitude` (decimal, 선택): 위도 좌표
- `longitude` (decimal, 선택): 경도 좌표
- `description` (string, 선택): 공연 설명
- `image` (file, 선택): 공연 이미지 파일 (JPG/PNG 등, 1개만)

#### 응답 (200 OK)
```json
{
  "success": true,
  "message": "공연 홍보 생성 성공!",
  "data": {
    "id": 25
  }
}
```

**참고**: 생성된 공연 홍보의 상세 정보가 필요한 경우, 반환된 ID로 상세 조회 API(`GET /api/promos/{promoId}`)를 호출하세요.

---

## 4. 공연 홍보 수정
### PATCH `/api/promos/{promoId}`

#### Content-Type
`multipart/form-data`

#### 요청
```bash
curl -X PATCH "http://localhost:8080/api/promos/1" \
  -H "Authorization: Bearer {JWT_TOKEN}" \
  -F "teamName=수정된 팀명" \
  -F "title=수정된 공연 제목" \
  -F "latitude=37.5563" \
  -F "longitude=126.9236"
```

**참고**: 필요한 필드만 전송하면 됩니다. 전송되지 않은 필드는 기존 값이 유지됩니다.

#### 요청 필드 (모든 필드 선택사항)
- `teamName` (string, 선택): 팀명 (최대 255자)
- `title` (string, 선택): 공연 제목 (최대 255자)
- `admissionFee` (decimal, 선택): 입장료 (0 이상의 값, 음수 불가)
- `eventDatetime` (string, 선택): 공연 일시 (ISO 8601)
- `location` (string, 선택): 장소명 (최대 255자)
- `address` (string, 선택): 상세 주소 (최대 255자)
- `latitude` (decimal, 선택): 위도 좌표
- `longitude` (decimal, 선택): 경도 좌표
- `description` (string, 선택): 공연 설명
- `image` (file, 선택): 새 이미지 파일 (업로드 시 기존 이미지 자동 교체)
- `deleteImageUrl` (string, 선택): 삭제할 이미지 URL (이미지만 삭제하고 싶을 때)

#### 응답 (200 OK)
```json
{
  "success": true,
  "message": "공연 홍보 수정 성공!",
  "data": null
}
```

**참고**: 수정된 공연 홍보의 상세 정보가 필요한 경우, 상세 조회 API(`GET /api/promos/{promoId}`)를 호출하세요.

---

## 5. 공연 홍보 삭제
### DELETE `/api/promos/{promoId}`

#### 요청
```bash
curl -X DELETE "http://localhost:8080/api/promos/1" \
  -H "Authorization: Bearer {JWT_TOKEN}"
```

#### 응답 (200 OK)
```json
{
  "success": true,
  "message": "공연 홍보 삭제 성공!",
  "data": null
}
```

**참고**: 공연 홍보 삭제는 관련된 이미지와 함께 처리됩니다.

---

## 6. 공연 홍보 검색
### GET `/api/promos/search`

#### 요청
```bash
curl -X GET "http://localhost:8080/api/promos/search?keyword=락밴드&page=0&size=20&sort=createdAt,desc"
```

#### 쿼리 파라미터
- `keyword` (string, 필수): 검색 키워드
- `page`, `size`, `sort`: 페이지네이션

#### 응답 (200 OK)
```json
{
  "success": true,
  "message": "공연 홍보 검색 성공",
  "data": {
    "content": [
      {
        "id": 1,
        "teamName": "락밴드 팀",
        "creatorId": 1,
        "creatorName": "홍길동",
        "title": "락밴드 정기공연",
        "admissionFee": 10000,
        "eventDatetime": "2024-03-15T19:00:00",
        "location": "홍대 클럽",
        "address": "서울시 마포구 홍익로 123",
        "latitude": 37.5563,
        "longitude": 126.9236,
        "description": "락밴드 팀의 정기 공연입니다.",
        "viewCount": 100,
        "commentCount": 5,
        "likeCount": 20,
        "isLikedByUser": true,
        "createdAt": "2024-03-01T10:00:00",
        "updatedAt": "2024-03-01T10:00:00",
        "photoUrls": ["https://example.com/photo.jpg"]
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

---

## 7. 공연 홍보 필터
### GET `/api/promos/filter`

#### 요청
```bash
curl -X GET "http://localhost:8080/api/promos/filter?teamName=락밴드&page=0&size=20&sort=createdAt,desc"
```

#### 쿼리 파라미터
- `startDate` (string, 선택): 시작 일자 (ISO 8601)
- `endDate` (string, 선택): 종료 일자 (ISO 8601)
- `teamName` (string, 선택): 팀명
- `page`, `size`, `sort`: 페이지네이션

#### 응답 (200 OK)
```json
{
  "success": true,
  "message": "공연 홍보 필터링 성공",
  "data": {
    "content": [
      {
        "id": 1,
        "teamName": "락밴드 팀",
        "creatorId": 1,
        "creatorName": "홍길동",
        "title": "락밴드 정기공연",
        "admissionFee": 10000,
        "eventDatetime": "2024-03-15T19:00:00",
        "location": "홍대 클럽",
        "address": "서울시 마포구 홍익로 123",
        "latitude": 37.5563,
        "longitude": 126.9236,
        "description": "락밴드 팀의 정기 공연입니다.",
        "viewCount": 100,
        "commentCount": 5,
        "likeCount": 20,
        "isLikedByUser": true,
        "createdAt": "2024-03-01T10:00:00",
        "updatedAt": "2024-03-01T10:00:00",
        "photoUrls": ["https://example.com/photo.jpg"]
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

---

## 8. 공연 홍보 지도상 검색
### GET `/api/promos/map`

#### 요청
```bash
curl -X GET "http://localhost:8080/api/promos/map?startLatitude=37.5000&startLongitude=126.9000&endLatitude=37.6000&endLongitude=127.0000&page=0&size=20&sort=createdAt,desc"
```

#### 쿼리 파라미터
- `startLatitude` (decimal, 필수): 검색 영역 시작 위도
- `startLongitude` (decimal, 필수): 검색 영역 시작 경도
- `endLatitude` (decimal, 필수): 검색 영역 끝 위도
- `endLongitude` (decimal, 필수): 검색 영역 끝 경도
- `page`, `size`, `sort`: 페이지네이션

#### 응답 (200 OK)
위의 필터링 응답과 동일한 구조

---

## 9. 공연 홍보 좋아요 추가/취소
### POST `/api/promos/{promoId}/like`

#### 요청
```bash
curl -X POST "http://localhost:8080/api/promos/1/like" \
  -H "Authorization: Bearer {JWT_TOKEN}"
```

#### 응답 (200 OK) - 좋아요 추가 성공
```json
{
  "success": true,
  "message": "공연 홍보 좋아요가 추가되었습니다.",
  "data": "liked"
}
```

#### 응답 (200 OK) - 좋아요 취소 성공
```json
{
  "success": true,
  "message": "공연 홍보 좋아요가 취소되었습니다.",
  "data": "unliked"
}
```

---

## 10. 공연 홍보 좋아요 상태 조회
### GET `/api/promos/{promoId}/like/status`

#### 요청
```bash
curl -X GET "http://localhost:8080/api/promos/1/like/status" \
  -H "Authorization: Bearer {JWT_TOKEN}"
```

#### 응답 (200 OK)
```json
{
  "success": true,
  "message": "공연 홍보 좋아요 상태 조회 성공",
  "data": true
}
```

#### 응답 필드
- `data` (boolean): 사용자의 좋아요 상태 (true: 좋아요 누름, false: 좋아요 안 누름)

---

## 11. 공연 홍보 좋아요 수 조회
### GET `/api/promos/{promoId}/like/count`

#### 요청
```bash
curl -X GET "http://localhost:8080/api/promos/1/like/count"
```

#### 응답 (200 OK)
```json
{
  "success": true,
  "message": "공연 홍보 좋아요 수 조회 성공",
  "data": 25
}
```

#### 응답 필드
- `data` (integer): 공연 홍보의 좋아요 수

---

## 13. 공연 홍보 필터링
### GET `/api/promos/filter`

#### 요청
```bash
curl -X GET "http://localhost:8080/api/promos/filter?startDate=2024-03-01&endDate=2024-03-31&teamName=락밴드&page=0&size=20&sort=createdAt,desc"
```

#### 쿼리 파라미터
- `startDate` (string, 선택): 시작 날짜 (ISO DATE 형식: YYYY-MM-DD)
- `endDate` (string, 선택): 종료 날짜 (ISO DATE 형식: YYYY-MM-DD)
- `teamName` (string, 선택): 팀명
- `page` (integer): 페이지 번호 (기본값: 0)
- `size` (integer): 페이지 크기 (기본값: 20)
- `sort` (string): 정렬 기준 (기본값: "createdAt,desc")

#### 응답 (200 OK)
위의 검색 응답과 동일한 구조

---

## 14. 공연 홍보 지도 검색
### GET `/api/promos/map`

#### 요청
```bash
curl -X GET "http://localhost:8080/api/promos/map?startLatitude=37.5000&startLongitude=126.9000&endLatitude=37.6000&endLongitude=127.0000&page=0&size=20&sort=createdAt,desc"
```

#### 쿼리 파라미터
- `startLatitude` (decimal, 필수): 검색 영역 시작 위도
- `startLongitude` (decimal, 필수): 검색 영역 시작 경도
- `endLatitude` (decimal, 필수): 검색 영역 끝 위도
- `endLongitude` (decimal, 필수): 검색 영역 끝 경도
- `page` (integer): 페이지 번호 (기본값: 0)
- `size` (integer): 페이지 크기 (기본값: 20)
- `sort` (string): 정렬 기준 (기본값: "createdAt,desc")

#### 응답 (200 OK)
위의 검색 응답과 동일한 구조

---

## 15. 제목으로 공연 홍보 검색
### GET `/api/promos/search/title`

#### 요청
```bash
curl -X GET "http://localhost:8080/api/promos/search/title?title=정기공연&page=0&size=20"
```

#### 쿼리 파라미터
- `title` (string, 필수): 검색할 제목
- `page` (integer): 페이지 번호 (기본값: 0)
- `size` (integer): 페이지 크기 (기본값: 20)

#### 응답 (200 OK)
위의 검색 응답과 동일한 구조

---

## 16. 팀명으로 공연 홍보 검색
### GET `/api/promos/search/team`

#### 요청
```bash
curl -X GET "http://localhost:8080/api/promos/search/team?teamName=락밴드&page=0&size=20"
```

#### 쿼리 파라미터
- `teamName` (string, 필수): 검색할 팀명
- `page` (integer): 페이지 번호 (기본값: 0)
- `size` (integer): 페이지 크기 (기본값: 20)

#### 응답 (200 OK)
위의 검색 응답과 동일한 구조

---

## 17. 장소로 공연 홍보 검색
### GET `/api/promos/search/location`

#### 요청
```bash
curl -X GET "http://localhost:8080/api/promos/search/location?location=홍대&page=0&size=20"
```

#### 쿼리 파라미터
- `location` (string, 필수): 검색할 장소
- `page` (integer): 페이지 번호 (기본값: 0)
- `size` (integer): 페이지 크기 (기본값: 20)

#### 응답 (200 OK)
위의 검색 응답과 동일한 구조

---

## 18. 모든 공연 홍보 조회
### GET `/api/promos/search/all`

#### 요청
```bash
curl -X GET "http://localhost:8080/api/promos/search/all?page=0&size=20"
```

#### 쿼리 파라미터
- `page` (integer): 페이지 번호 (기본값: 0)
- `size` (integer): 페이지 크기 (기본값: 20)

#### 응답 (200 OK)
위의 검색 응답과 동일한 구조


---

## 20. 공연 상태별 필터링
### GET `/api/promos/status`

#### 설명
공연 상태에 따라 필터링하여 조회합니다. 진행 중인 공연, 예정된 공연, 종료된 공연을 구분하여 볼 수 있습니다.

#### 요청
```bash
# 진행 중인 공연만 조회
curl -X GET "http://localhost:8080/api/promos/status?status=ongoing&page=0&size=20&sort=eventDatetime,asc"

# 예정된 공연 중에서 키워드 검색
curl -X GET "http://localhost:8080/api/promos/status?status=upcoming&keyword=락밴드&page=0&size=20&sort=eventDatetime,asc"

# 특정 팀의 종료된 공연 조회
curl -X GET "http://localhost:8080/api/promos/status?status=ended&teamName=밴드A&page=0&size=20&sort=eventDatetime,desc"
```

#### 쿼리 파라미터
- `status` (string, 필수): 공연 상태
  - `ongoing`: 진행 중인 공연 (오늘 날짜와 일치하는 공연)
  - `upcoming`: 예정된 공연 (오늘 이후의 공연)
  - `ended`: 종료된 공연 (오늘 이전의 공연)
- `keyword` (string, 선택): 검색 키워드 (제목, 팀명, 설명, 위치, 주소에서 검색)
- `teamName` (string, 선택): 팀명으로 필터링
- `page` (integer): 페이지 번호 (기본값: 0)
- `size` (integer): 페이지 크기 (기본값: 20)
- `sort` (string): 정렬 기준 (기본값: "eventDate,asc")

#### 응답 (200 OK)
```json
{
  "success": true,
  "message": "진행 중인 공연 필터링 성공",
  "data": {
    "content": [
      {
        "id": 1,
        "teamName": "락밴드 팀",
        "title": "락밴드 정기공연",
        "description": "락밴드 팀의 정기 공연입니다.",
        "location": "홍대 클럽",
        "address": "서울시 마포구 홍익로 123",
        "latitude": 37.5563,
        "longitude": 126.9236,
        "admissionFee": 10000,
        "eventDatetime": "2024-03-15T19:00:00",
        "createdAt": "2024-03-01T10:00:00",
        "updatedAt": "2024-03-01T10:00:00",
        "likeCount": 20,
        "isLikedByUser": true,
        "photoUrls": ["https://example.com/photo.jpg"]
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

#### 공연 상태 구분 기준
- **진행 중 (ongoing)**: 공연 날짜가 오늘과 일치하는 공연
- **예정 (upcoming)**: 공연 날짜가 오늘 이후인 공연
- **종료 (ended)**: 공연 날짜가 오늘 이전인 공연

#### 사용 예시
1. **진행 중인 공연만 조회**: `status=ongoing`
2. **예정된 공연 중에서 키워드 검색**: `status=upcoming&keyword=콘서트`
3. **특정 팀의 종료된 공연 조회**: `status=ended&teamName=밴드B`
4. **예정된 공연 중에서 키워드와 팀명으로 필터링**: `status=upcoming&keyword=공연&teamName=밴드C&page=0&size=10`

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
- `403 Forbidden`: 권한 없음
- `404 Not Found`: 리소스 없음

### 주요 에러 메시지
- `관람료는 음수가 될 수 없습니다.`: 관람료에 음수값을 입력한 경우 (400 Bad Request)

---

## 참고 항목
- **권한**: 생성/수정/삭제는 인증된 사용자만, 조회/검색은 모든 사용자 가능
- **팀명**: teamName 필드가 사용되며 모든 공연 홍보에 필수
- **위치 정보**: latitude, longitude 필드로 지도상 검색 기능 지원
- **이미지**: 생성/수정 시 multipart/form-data 형식으로 처리, 1개만 허용
- **이미지 형식**: JPG, PNG 등 일반적인 이미지 형식 지원
- **이미지 교체**: 수정 시 새 이미지 업로드하면 기존 이미지 자동 교체
- **부분 수정**: PATCH 방식으로 필요한 필드만 전송하면 나머지는 기존 값 유지
- **관람료 검증**: 관람료는 0 이상의 값만 허용, 음수값 입력 시 BadRequest 에러 발생
- **동적 계산**: viewCount, commentCount, likeCount는 실시간 동기화
- **소프트 삭제**: deletedAt 필드 사용, 이미지도 함께 삭제 처리
- **좋아요**: 토글 방식으로 추가/취소 (같은 API), 중복 좋아요 방지
- **좋아요 상태**: 공연 홍보 목록/상세 조회 시 `isLikedByUser` 필드 포함
- **정렬 옵션**: `sort` 파라미터로 정렬 기준 지정 가능 (기본값: `createdAt,desc`)
- **검색**:
  - **JPA 기반 검색**: `/api/promos/search`, `/api/promos/filter`, `/api/promos/map`, `/api/promos/status`
  - **검색 범위**: 제목, 팀명, 설명, 위치, 주소에서 통합 검색
  - **날짜 형식**: 필터링 관련 날짜는 ISO DATE/ISO DATETIME 형식을 사용
  - **데이터 동기화**: 별도의 DB-검색 엔진 동기화 기능은 제공하지 않습니다
- **응답 최적화**: 
  - **생성**: ID만 반환 (성능 최적화)
  - **수정/삭제**: null 반환
  - **세부 정보 필요 시**: 별도 조회 API 호출하여 최신 데이터 확인
