# ClubPending API

## 동아리 가입 신청 관리
모든 API는 JWT 인증 필요

### 경로 매개변수 검증
- 모든 `clubId`와 `pendingId`는 양수(1 이상)여야 함
- 잘못된 값 입력 시 400 Bad Request 응답

---

## 1. 동아리 가입 신청
```
POST /api/clubs/{clubId}/pendings
Authorization: Bearer {JWT_TOKEN}
```

### 요청 예시
```bash
curl -X POST "http://localhost:8080/api/clubs/1/pendings" \
  -H "Authorization: Bearer {JWT_TOKEN}"
```

### 성공 응답 (201)
```json
{
  "success": true,
  "message": "가입 신청이 완료되었습니다.",
  "data": {
    "pendingId": 1,
    "clubId": 1,
    "clubName": "락밴드 동아리",
    "userId": 2,
    "userNickname": "홍길동",
    "status": "PENDING",
    "appliedAt": "2024-03-15T10:30:00",
    "processedAt": null,
    "expiresAt": "2024-03-22T10:30:00",
    "processedBy": null,
    "processedByNickname": null
  }
}
```

### 실패 응답
- **400**: 이미 가입한 동아리
- **400**: 이미 신청한 동아리
- **403**: 강퇴된 사용자의 재가입 시도
- **404**: 존재하지 않는 동아리

### 비즈니스 규칙
- 가입 신청은 7일 후 자동 만료
- 거부되거나 만료된 신청이 있어도 새로운 신청 가능
- 강퇴된 회원은 재가입 불가
- 동시에 PENDING 상태의 신청은 하나만 가능

---

## 2. 동아리 대기 목록 조회 (동아리장 전용)
```
GET /api/clubs/{clubId}/pendings
Authorization: Bearer {JWT_TOKEN}
```

### 요청 예시
```bash
curl "http://localhost:8080/api/clubs/1/pendings" \
  -H "Authorization: Bearer {JWT_TOKEN}"
```

### 성공 응답 (200)
```json
{
  "success": true,
  "message": "대기 목록 조회 성공",
  "data": {
    "clubId": 1,
    "clubName": "락밴드 동아리",
    "pendingMembers": [
      {
        "pendingId": 1,
        "clubId": 1,
        "clubName": "락밴드 동아리",
        "userId": 2,
        "userNickname": "홍길동",
        "status": "PENDING",
        "appliedAt": "2024-03-15T10:30:00",
        "processedAt": null,
        "expiresAt": "2024-03-22T10:30:00",
        "processedBy": null,
        "processedByNickname": null
      },
      {
        "pendingId": 2,
        "clubId": 1,
        "clubName": "락밴드 동아리",
        "userId": 3,
        "userNickname": "김철수",
        "status": "PENDING",
        "appliedAt": "2024-03-16T14:20:00",
        "processedAt": null,
        "expiresAt": "2024-03-23T14:20:00",
        "processedBy": null,
        "processedByNickname": null
      }
    ],
    "totalCount": 2,
    "pendingCount": 2
  }
}
```

### 실패 응답
- **403**: 동아리 대표자가 아님
- **404**: 존재하지 않는 동아리

---

## 3. 내 가입 신청 조회
```
GET /api/clubs/{clubId}/pendings/my
Authorization: Bearer {JWT_TOKEN}
```

**참고**: 현재 대기중(PENDING)인 신청만 조회됩니다.

### 요청 예시
```bash
curl "http://localhost:8080/api/clubs/1/pendings/my" \
  -H "Authorization: Bearer {JWT_TOKEN}"
```

### 성공 응답 (200)

**대기중인 신청이 있는 경우:**
```json
{
  "success": true,
  "message": "신청 상태 조회 성공",
  "data": {
    "pendingId": 1,
    "clubId": 1,
    "clubName": "락밴드 동아리",
    "userId": 2,
    "userNickname": "홍길동",
    "status": "PENDING",
    "appliedAt": "2024-03-15T10:30:00",
    "processedAt": null,
    "expiresAt": "2024-03-22T10:30:00",
    "processedBy": null,
    "processedByNickname": null
  }
}
```

**대기중인 신청이 없는 경우:**
```json
{
  "success": true,
  "message": "대기중인 신청이 없습니다",
  "data": null
}
```

---

## 4. 가입 신청 승인/거부 (동아리장 전용)
```
PATCH /api/clubs/pendings/{pendingId}
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json
```

### 요청 예시
```bash
# 승인
curl -X PATCH "http://localhost:8080/api/clubs/pendings/1" \
  -H "Authorization: Bearer {JWT_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "approve": true
  }'

# 거부
curl -X PATCH "http://localhost:8080/api/clubs/pendings/1" \
  -H "Authorization: Bearer {JWT_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "approve": false
  }'
```

### 요청 필드
- `approve` (boolean, 필수): true=승인, false=거부

### 성공 응답 (200)
```json
{
  "success": true,
  "message": "가입 신청이 승인되었습니다.",
  "data": {
    "pendingId": 1,
    "clubId": 1,
    "clubName": "락밴드 동아리",
    "userId": 2,
    "userNickname": "홍길동",
    "status": "APPROVED",
    "appliedAt": "2024-03-15T10:30:00",
    "processedAt": "2024-03-16T09:00:00",
    "expiresAt": "2024-03-22T10:30:00",
    "processedBy": 1,
    "processedByNickname": "동아리장"
  }
}
```

### 실패 응답
- **403**: 동아리 대표자가 아님
- **404**: 존재하지 않는 신청
- **400**: 이미 처리된 신청
- **400**: 만료된 신청

### 비즈니스 규칙
- 승인 시 자동으로 ClubMember로 등록
- 이전에 탈퇴했던 회원은 재활성화 처리

---

## 5. 가입 신청 취소
```
DELETE /api/clubs/pendings/{pendingId}
Authorization: Bearer {JWT_TOKEN}
```

### 요청 예시
```bash
curl -X DELETE "http://localhost:8080/api/clubs/pendings/1" \
  -H "Authorization: Bearer {JWT_TOKEN}"
```

### 성공 응답 (200)
```json
{
  "success": true,
  "message": "가입 신청이 취소되었습니다.",
  "data": null
}
```

### 실패 응답
- **403**: 본인의 신청이 아님
- **404**: 존재하지 않는 신청
- **400**: 대기중인 신청만 취소 가능

---

## 가입 신청 상태

### PendingStatus 열거형
- `PENDING`: 대기중
- `APPROVED`: 승인됨
- `REJECTED`: 거부됨
- `EXPIRED`: 만료됨

### 자동 만료 처리
- 매일 자정(00:00:00)에 7일이 지난 PENDING 상태의 신청을 EXPIRED로 변경
- 스케줄러를 통한 자동 처리
- 예외 발생 시에도 스케줄러가 중단되지 않도록 예외 처리 구현
- 만료 처리 건수에 대한 로깅 기능 포함

### 동시성 처리
- 중복 신청 방지를 위한 유니크 제약조건 (club_id + user_id + status)
- DataIntegrityViolationException 처리를 통한 동시성 제어

### 재신청 처리
- REJECTED 또는 EXPIRED 상태의 신청이 있어도 새로운 신청 생성 가능
- 각 신청은 독립적인 레코드로 관리되어 신청 이력 보존
- PENDING 상태의 신청이 이미 있으면 중복 신청 불가
- 승인 후 탈퇴한 회원도 새로운 신청 가능

---

## 권한 요구사항

### 동아리장 권한이 필요한 API
- 대기 목록 조회 (GET /api/clubs/{clubId}/pendings)
- 가입 신청 처리 (PATCH /api/clubs/pendings/{pendingId})

### 본인 확인이 필요한 API
- 가입 신청 취소 (DELETE /api/clubs/pendings/{pendingId})
- 내 신청 조회 (GET /api/clubs/{clubId}/pendings/my)

---

## 성능 최적화

### 쿼리 최적화
- 대기 목록 조회 시 JOIN FETCH를 사용하여 N+1 쿼리 문제 해결
- 회원 상태 확인을 한 번의 쿼리로 통합하여 DB 조회 최소화

### 보안 강화
- 모든 경로 매개변수에 대한 입력 검증 추가
- 양수 검증으로 잘못된 ID 값 방지
