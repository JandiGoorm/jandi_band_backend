# 동아리 팀 목록 조회 API 문서

## 개요
특정 동아리에 속한 팀들의 목록을 조회하는 API입니다. 각 팀의 기본 정보와 현재 연습 중인 곡 정보를 함께 제공합니다.

## API 명세

### 요청
```
GET /api/v1/clubs/{clubId}/teams
```

### 경로 매개변수
- `clubId` (Integer): 조회할 동아리의 ID

### 쿼리 매개변수
- `page` (Integer, optional): 페이지 번호 (0부터 시작, 기본값: 0)
- `size` (Integer, optional): 페이지 크기 (기본값: 10)
- `sort` (String, optional): 정렬 기준 (기본값: createdAt,desc)

### 응답
```json
{
  "success": true,
  "message": "동아리 팀 목록 조회 성공",
  "data": {
    "content": [
      {
        "id": 1,
        "name": "락밴드팀 A",
        "description": "록 음악을 주로 연주하는 팀입니다.",
        "creatorId": 5,
        "creatorName": "김창수",
        "createdAt": "2024-01-15T09:00:00",
        "memberCount": 4,
        "currentPracticeSong": "Bohemian Rhapsody - Queen"
      },
      {
        "id": 2,
        "name": "어쿠스틱 듀오",
        "description": "어쿠스틱 기타와 보컬로 구성된 듀오팀",
        "creatorId": 8,
        "creatorName": "이민수",
        "createdAt": "2024-01-20T14:30:00",
        "memberCount": 2,
        "currentPracticeSong": "Hotel California - Eagles"
      },
      {
        "id": 3,
        "name": "재즈 앙상블",
        "description": "재즈 음악 전문 연주팀",
        "creatorId": 12,
        "creatorName": "박지은",
        "createdAt": "2024-02-01T11:15:00",
        "memberCount": 5,
        "currentPracticeSong": null
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 3,
    "totalPages": 1,
    "last": true
  }
}
```

### 응답 필드 설명

#### data 객체
- `content`: 팀 목록 배열
- `page`: 현재 페이지 번호
- `size`: 페이지 크기
- `totalElements`: 전체 팀 수
- `totalPages`: 전체 페이지 수
- `last`: 마지막 페이지 여부

#### content 배열의 각 팀 객체
- `id`: 팀 ID
- `name`: 팀 이름
- `description`: 팀 설명
- `creatorId`: 팀 생성자 ID
- `creatorName`: 팀 생성자 닉네임
- `createdAt`: 팀 생성일시
- `memberCount`: 팀 멤버 수
- `currentPracticeSong`: 현재 연습 중인 곡 ("곡명 - 아티스트" 형태, 없으면 null)

## cURL 예제

### 기본 조회
```bash
curl -X GET "http://localhost:8080/api/v1/clubs/1/teams"
```

### 페이지네이션 적용
```bash
curl -X GET "http://localhost:8080/api/v1/clubs/1/teams?page=0&size=5"
```

### 정렬 옵션 적용
```bash
curl -X GET "http://localhost:8080/api/v1/clubs/1/teams?sort=name,asc"
```

## 오류 응답

### 동아리를 찾을 수 없는 경우 (404)
```json
{
  "success": false,
  "message": "동아리를 찾을 수 없습니다.",
  "errorCode": "RESOURCE_NOT_FOUND"
}
```

### 잘못된 clubId 형식 (400)
```json
{
  "success": false,
  "message": "잘못된 요청입니다.",
  "errorCode": "BAD_REQUEST"
}
```

## 특이사항

1. **정렬 순서**: 
   - 기본적으로 팀 생성일시 역순 (최근 생성된 팀부터)
   - `sort` 매개변수로 다른 정렬 기준 지정 가능

2. **현재 연습 곡 로직**:
   - 현재 시간 기준으로 가장 가까운 미래 연습 일정의 곡
   - 연습 일정이 없거나 곡 연습이 아닌 경우 `null`
   - 곡 연습 일정은 `name` 필드에 " - "가 포함된 경우로 판단

3. **삭제된 팀 제외**: 
   - `deletedAt`이 설정된 팀은 목록에서 제외

4. **권한**: 
   - 인증 불필요 (공개 정보)
   - 삭제되지 않은 동아리만 조회 가능

5. **성능 최적화**:
   - 페이지네이션으로 대량 데이터 처리
   - Lazy Loading으로 필요한 데이터만 조회

## 사용 예시

이 API는 다음과 같은 상황에서 활용됩니다:

1. **동아리 상세 페이지**: 해당 동아리에 어떤 팀들이 있는지 보여줄 때
2. **팀 선택**: 사용자가 가입하고 싶은 팀을 찾을 때  
3. **동아리 관리**: 동아리 관리자가 소속 팀들을 관리할 때
4. **연습 현황 파악**: 각 팀이 어떤 곡을 연습하고 있는지 확인할 때 