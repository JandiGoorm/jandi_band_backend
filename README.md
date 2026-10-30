# Jandi Band Backend - 대학 밴드 동아리 매칭 플랫폼

대학생 밴드 동아리를 위한 매칭 및 커뮤니티 플랫폼의 백엔드 서버입니다.

## 주요 기능

- **동아리 관리**: 동아리 생성, 가입, 멤버 관리
- **팀 관리**: 팀 생성 및 연습 스케줄 관리
- **홍보/모집**: 동아리 홍보 게시판, 멤버 모집
- **투표 시스템**: 동아리 내 투표 기능
- **초대 시스템**: 동아리/팀 초대 링크 생성
- **인증/인가**: JWT 기반 인증, 카카오 소셜 로그인

---

## 기술 스택

| 구분 | 기술 |
|:-----|:-----|
| **Language** | Java 25 LTS |
| **Framework** | Spring Boot 4.1.2, Spring Security, Spring Data JPA |
| **Database** | 관리형 MySQL |
| **Storage** | OCI Object Storage (S3 호환 API) |
| **Infra** | Docker, GitHub Actions |
| **Test** | JUnit, Mockito, H2 (테스트 DB) |
| **Docs** | Swagger (SpringDoc OpenAPI) |

---

## 프로젝트 구조

```
jandi_band_backend/
├── src/main/java/com/jandi/band_backend/
│   ├── auth/               # 인증 (카카오 OAuth)
│   ├── club/               # 동아리 관리
│   ├── clubpending/        # 동아리 가입 대기
│   ├── team/               # 팀 관리
│   ├── invite/             # 초대 시스템
│   ├── poll/               # 투표 시스템
│   ├── promo/              # 홍보 게시판
│   ├── notice/             # 공지사항
│   ├── user/               # 사용자 관리
│   ├── univ/               # 대학교 정보
│   ├── image/              # 이미지 업로드
│   ├── health/             # 헬스체크
│   ├── config/             # 설정
│   ├── security/           # 보안 설정
│   └── global/             # 공통 유틸리티
├── src/test/               # 테스트 코드
├── build.gradle            # Gradle 빌드 설정
├── Dockerfile              # Docker 이미지 빌드
└── README.md
```

---

## 로컬 개발: 프론트엔드·Spring·FastAPI·MySQL을 Docker로 실행

Docker Desktop(Linux 컨테이너)과 Docker Compose v2를 사용한다. 호스트에 Java·Node.js·Python·MySQL을 따로 설치할 필요는 없다. 프론트 컨테이너는 원래의 `npm run dev`로 Vite 개발 서버를 실행한다. 로컬 파일 변경은 핫리로드로 반영한다. Vite는 `/api` 요청을 Spring으로, `/scraper` 요청을 접두사 제거 후 FastAPI로 전달한다. Redis는 필요 없다.

### 1. 저장소 준비

세 저장소를 같은 부모 폴더에 둔다. 이미 클론했다면 이 단계는 건너뛴다.

```powershell
git clone --branch dev https://github.com/JandiGoorm/jandi_band_backend.git
git clone https://github.com/JandiGoorm/jandi_band_frontend.git
git clone --branch dev https://github.com/JandiGoorm/jandi_band_py.git
cd jandi_band_backend
```

```text
개발 폴더/
├── jandi_band_backend/
├── jandi_band_frontend/
└── jandi_band_py/
```

이하 명령은 백엔드 저장소 루트에서 실행한다.

### 2. 백엔드·프론트 환경 파일 준비

```powershell
Copy-Item .env.local.example .env.local
Copy-Item ../jandi_band_frontend/.env.example ../jandi_band_frontend/.env
Copy-Item ../jandi_band_py/.env.example ../jandi_band_py/.env
```

**OCI 개발 버킷의 접속 정보 7개는 저장소 관리자에게 별도로 받아야 한다.** 전달받은 `IMAGE_STORAGE_*` 값을 `.env.local`에 넣는다. 운영 버킷 자격증명을 사용하지 않는다. 이 파일은 Git 추적 대상이 아니며, 실제 값은 README·공개 저장소·Notion에 기록하지 않는다.

카카오 로그인은 운영과 같은 카카오 앱의 키를 사용한다. 관리자에게 받은 `KAKAO_REST_API_KEY`와 `KAKAO_ADMIN_KEY`를 백엔드 `.env.local`에 넣는다. MySQL 계정과 JWT 키는 Compose에 명시된 로컬 전용 데모 값을 쓴다. OCI 자격증명은 백엔드 실행 시에만 주입하며 프론트 컨테이너에 전달하지 않는다.

`.env.local`는 Spring 백엔드 전용이다. 프론트의 주소와 `VITE_KAKAO_JAVASCRIPT_KEY`·`VITE_KAKAOMAP_APP_KEY` 등 `VITE_*` 설정은 **`../jandi_band_frontend/.env`에서 별도로 관리**한다. 프론트의 `VITE_KAKAO_CLIENT_ID`에는 백엔드의 `KAKAO_REST_API_KEY`와 같은 값을 넣는다. FastAPI 설정은 `../jandi_band_py/.env`에서 관리한다. Spring의 공통 properties는 이 저장소의 `config/`에 둔다. Compose가 각 환경 파일을 해당 컨테이너에만 주입한다. 프론트 환경 파일도 Git에서 제외되며 이미지에 복사하지 않는다. 지도·공유 기능의 JavaScript 키와 localhost 도메인 설정도 프론트 담당자에게 확인한다. `KAKAO_ADMIN_KEY`와 OCI·DB 자격증명은 프론트 환경 파일에 넣지 않는다.

### 3. 이미지 빌드와 실행

```powershell
docker compose -f local/compose.yaml up --build -d --wait
```

| 대상 | 접속 주소 |
| --- | --- |
| 프론트엔드 | http://localhost:3000 → Vite 컨테이너 내부 5173 |
| 백엔드 API | http://localhost:8080/api |
| Swagger | http://localhost:8080/swagger-ui.html |
| FastAPI / 문서 | http://localhost:8000 / http://localhost:8000/docs |
| MySQL | `localhost:3306`, DB `rhythmeet_local` (컨테이너 간 접속은 `mysql:3306`) |

호스트의 3000·3306·8080·8000 포트는 localhost에만 연다. MySQL 로컬 계정은 `rhythmeet_local` / `local-db-only`이며 운영에 사용하지 않는다. **브라우저에서는 `localhost`를 사용한다.** 프론트 API·콜백 주소도 이 호스트 이름을 사용한다.

MySQL 데이터 볼륨이 처음 만들어지면 `database/local/001_schema.sql`과 `002_mock_data.sql`을 순서대로 실행한다. 운영과 같은 27개 테이블 구조와 가상 데이터 102건을 넣는다. 이후 재시작에서는 데이터를 유지한다. 구성과 수동 가져오기 방법은 [로컬 개발 DB](docs/local-database.md)를 참고한다.

### 4. 카카오 로그인

로컬에서도 실제 카카오 계정으로 로그인한다. 운영과 같은 카카오 앱을 사용하고, 로그인 후 회원 정보와 토큰은 로컬 MySQL에 저장한다. [카카오 설정 안내](https://developers.kakao.com/docs/ko/kakaologin/prerequisite)를 참고한다.

1. 해당 카카오 앱의 REST API 키에 `http://localhost:3000/callback`을 리다이렉트 URI로 등록한다. 기존 운영 콜백은 유지한다.
2. 백엔드 `.env.local`의 `KAKAO_REST_API_KEY`와 프론트 `.env`의 `VITE_KAKAO_CLIENT_ID`에 같은 REST API 키를 넣는다. `KAKAO_ADMIN_KEY`는 백엔드에만 넣는다.
3. 프론트 `.env`의 `VITE_KAKAO_REDIRECT_URI=http://localhost:3000/callback`을 유지한다. Compose는 백엔드의 토큰 교환 콜백도 같은 주소로 설정한다.
4. 아래 명령으로 컨테이너를 재생성한 뒤 프론트의 **카카오 계정으로 계속하기** 버튼으로 로그인한다.

```powershell
docker compose -f local/compose.yaml up --build -d --wait
```

실제 로그인으로 생성한 로컬 회원은 목데이터의 가상 회원과 별개다. 목데이터 계정으로 실제 카카오 로그인을 할 수는 없다. 실제 계정의 로컬 동아리·팀은 새로 만들거나 로컬 DB에서 권한을 준비한다. 기본 목데이터의 동아리·팀 ID는 각각 `1`이다. SQL의 사진은 자체 SVG 표시용 이미지이며, 실제 업로드에는 개발 버킷을 사용한다.

같은 카카오 앱을 공유하므로 로컬 회원탈퇴의 카카오 연결 끊기는 운영에서 사용하는 카카오 연결에도 영향을 준다. 실제 계정으로 회원탈퇴를 시험할 때는 이 영향을 확인한다.

### 5. FastAPI 시간표 수집과 Spring 저장

FastAPI는 에브리타임 공유 URL을 받아 `Mon`부터 `Sun`까지 30분 단위의 **비어 있는 시간**을 반환한다. 기본 컨테이너는 실제 에브리타임 서버를 호출한다. 조회에는 본인이 테스트할 수 있는 공개 공유 URL을 사용한다.

```powershell
curl.exe --get http://localhost:8000/timetable --data-urlencode "url=https://everytime.kr/@공유식별자"
# 같은 API를 Vite 프록시로 호출
curl.exe --get http://localhost:3000/scraper/timetable --data-urlencode "url=https://everytime.kr/@공유식별자"
```

응답의 `data.timetableData`를 로그인한 사용자의 `POST /api/users/me/timetables`에 `{"name":"시간표 이름","timetableData":...}` 형태로 보내면 개인 시간표로 저장한다. 저장된 ID는 `POST /api/teams/{teamId}/members/me/timetable`의 `userTimetableId`로 제출한다. 수집은 FastAPI가, 저장·조회와 권한 검사는 Spring·MySQL이 담당한다.

**현재 프론트의 시간표 클라이언트는 운영 주소가 고정되어 있다.** 로컬 포트 설정만으로 화면의 에브리타임 버튼이 로컬 FastAPI를 호출하지는 않는다. 프론트 소스를 유지한 이 구성에서는 위 API로 수집·저장을 확인한다. 화면까지 로컬에 연결하려면 프론트 담당자가 시간표 클라이언트의 주소를 로컬 설정으로 바꿔야 한다.

### 로그·재빌드·종료

```powershell
docker compose -f local/compose.yaml ps
docker compose -f local/compose.yaml logs --tail 100 backend
```

Spring 코드를 바꾸면 런타임 이미지와 로컬 서비스를 다시 빌드한다. 프론트 소스는 로컬 폴더를 연결하며, 호스트에서 수정한 내용은 Vite가 자동으로 반영한다. Linux용 `node_modules`는 별도 Docker 볼륨에 둔다. FastAPI의 `app.py`·`service/`도 연결되어 코드 변경 시 Uvicorn이 재시작한다. Python 의존성을 바꾸면 `up --build`로 다시 빌드한다.

```powershell
docker compose -f local/compose.yaml up --build -d --wait
```

프론트 의존성을 바꾼 경우에는 아래 명령으로 컨테이너의 의존성을 다시 설치한다.

```powershell
docker compose -f local/compose.yaml exec frontend npm ci --legacy-peer-deps
```

컨테이너를 종료해도 MySQL 데이터 볼륨은 유지된다.

```powershell
docker compose -f local/compose.yaml down
```

**로컬 DB를 초기화하고 목데이터를 다시 넣을 때만** `down --volumes`를 쓴다. 이 명령은 이 Compose 프로젝트의 로컬 DB 데이터를 삭제한다. OCI 버킷 객체는 별도 저장소이므로 DB 초기화로 지워지지 않는다. 개발 중 업로드한 객체는 앱에서 삭제하거나 관리자의 정리 절차를 따른다.

### 지원 범위

- 카카오 로그인에는 관리자에게 받은 키와 localhost 콜백 등록이 필요하다. 지도·공유 기능은 같은 카카오 앱의 JavaScript 키와 웹 도메인 설정도 확인한다.
- 에브리타임 공유 시간표의 수집 API·Spring 저장·MySQL 조회는 로컬에서 확인할 수 있다. 화면의 시간표 버튼은 프론트 주소 설정을 별도로 바꿔야 한다. 실제 에브리타임 공개 여부·외부 응답은 사용한 공유 URL에 따라 확인한다.
- 프론트 개발 서버 실행은 `npm run build`의 타입 검사·배포 빌드를 대신하지 않는다.

---

## Docker 빌드와 테스트

```bash
docker build --target test -t jandi-band:test .
docker build --target runtime -t jandi-band:local .
```

테스트는 이미지의 `test` 단계에서 실행한다. 실제 비밀값은 빌드에 전달하지 않는다.

## CI/CD와 운영 설정

이 저장소의 GitHub Actions는 PR을 검증하고, `main` 변경만 운영 환경에 배포한다. `dev` 전용 서버는 없다.
Compose와 공개 설정은 이 저장소에서 관리하고, 실제 `.env`는 서버에서 수동 관리한다.
환경별 주소, GitHub Secrets/Variables, 서버 경로와 복구 절차는 [배포 문서](docs/deployment.md)를 참고한다.

이미지 저장소의 Object Storage 설정 계약, 이미지 변경 순서와 Docker HTTP 검증는
[이미지 저장소 문서](docs/image-storage.md)에서 관리한다. 실제 환경 파일은 사용자 관리 대상이다.

---

## 커밋 컨벤션

### 기본 포맷

```
태그(스코프): 제목 (50자 내외)

- 본문 (선택 사항)
```

### 스코프 (Scope)

| 스코프 | 설명 |
|:-------|:-----|
| `be` | Backend 관련 코드 |
| `infra` | 배포, Docker, CI/CD 등 |

### 태그 (Type)

| 태그 | 설명 | 예시 |
|:-----|:-----|:-----|
| `feat` | 새로운 기능 추가 | API 개발 |
| `fix` | 버그 수정 | 로직 오류 수정 |
| `docs` | 문서 수정 | README, Swagger |
| `style` | 코드 포맷팅 | 들여쓰기 정렬 |
| `refactor` | 코드 리팩토링 | 구조 개선 |
| `test` | 테스트 코드 | 테스트 추가/수정 |
| `chore` | 기타 잡무 | 빌드 설정 |

### 예시

```
feat(be): 동아리 초대 링크 생성 API 구현
fix(be): 투표 마감 시간 검증 오류 수정
docs(be): Swagger API 문서 업데이트
chore(infra): Dockerfile 최적화
```

---
