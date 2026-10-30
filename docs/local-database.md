# 로컬 개발 DB

MySQL 26.10에서 사용할 개발용 SQL이다. 운영 DB의 테이블·컬럼·인덱스·외래키 정의를 사용하며, 운영 데이터 행은 포함하지 않는다.

| 파일 | 내용 |
| --- | --- |
| `database/local/001_schema.sql` | 27개 테이블의 DDL. 데이터 없이 구조만 필요하면 이 파일만 실행한다. |
| `database/local/002_mock_data.sql` | 26개 테이블에 가상 데이터 102건을 추가한다. |

## 가져오기

운영 DB와 연결되지 않은 로컬 MySQL의 **새 DB**에서 순서대로 한 번 실행한다. 기존 DB를 초기화하거나 덮어쓰는 스크립트가 아니다. 외래키 검사는 계속 켜 둔다.

저장소 루트의 터미널에서 아래 두 파일을 로컬 컨테이너로 복사한다. `<mysql-container>`는 본인의 로컬 MySQL 컨테이너 이름으로 바꾼다.

```powershell
docker cp database/local/001_schema.sql <mysql-container>:/tmp/001_schema.sql
docker cp database/local/002_mock_data.sql <mysql-container>:/tmp/002_mock_data.sql
docker exec -it <mysql-container> mysql --default-character-set=utf8mb4 -u root -p
```

MySQL 프롬프트에서 실행한다.

```sql
CREATE DATABASE rhythmeet_local CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE rhythmeet_local;
SOURCE /tmp/001_schema.sql;
SOURCE /tmp/002_mock_data.sql;
```

백엔드의 `DB_URL`은 새 로컬 DB를 가리키게 하고, `DB_USERNAME`·`DB_PASSWORD`에는 해당 로컬 DB 계정을 넣는다. 백엔드와 MySQL이 같은 Compose 네트워크에 있고 MySQL 서비스 이름이 `mysql`이면 URL은 다음과 같다.

```text
jdbc:mysql://mysql:3306/rhythmeet_local?allowPublicKeyRetrieval=true&useSSL=false
```

이 URL의 비암호화 옵션은 로컬 Docker 개발 전용이다. 개발용 properties에 `spring.jpa.hibernate.ddl-auto=validate`를 설정하면 스키마를 자동 변경하지 않고 호환 여부를 검사한다. `app_expiring_value`도 포함되어 있으므로 `database/001_expiring_values.sql`을 다시 실행할 필요는 없다.

## 목데이터 구성

- 사용자 8명: 보컬·기타·베이스·드럼·건반·기타 포지션, 관리자, 가입 미완료 계정.
- 가상 지역·대학교, 동아리·팀·멤버, 가입 신청·승인·거절 이력.
- 개인·팀 시간표, 동아리·팀 일정, 공개·멤버 전용 갤러리.
- 진행 중·마감·예정 투표, 가상 곡과 투표 결과.
- 홍보글·댓글·좋아요·신고, 활성·예정·중지 공지.
- 삭제된 동아리·팀·일정·사진·게시글과 강퇴된 멤버의 필터 확인용 데이터.

일정과 공지·투표 기간은 SQL 실행 시각을 기준으로 만든다. 가입 완료된 기본 사용자 ID는 `1`, 동아리 ID는 `1`, 팀 ID는 `1`이다. 관리자 ID는 `7`, 가입 미완료 사용자 ID는 `8`이다.

사용자의 `kakao_oauth_id`는 모두 `image-test-browser-dev-*` 형식의 가상 값이다. 실제 카카오 로그인으로 이 계정에 들어갈 수는 없다. 카카오 인증 서버를 로컬 테스트 제공자로 대체할 때만 가상 ID를 사용한다. 실제 카카오 로그인 개발은 본인 계정을 사용하고, 개인 식별값은 공유 SQL에 넣지 않는다.

사진은 SQL에 포함된 SVG 데이터 URL이다. 외부 이미지 서버나 운영 버킷을 호출하지 않는다. 실제 이미지 업로드·삭제를 개발할 때는 별도의 개발 버킷과 개인 환경 파일을 사용한다. 목데이터 SQL은 버킷에 파일을 만들지 않는다.

`app_expiring_value`는 비어 있다. 로그인 토큰·블랙리스트·초대코드 등 만료 데이터는 앱 실행 중 생성된다. 운영 데이터와 시크릿은 이 SQL로 복원되지 않는다.
