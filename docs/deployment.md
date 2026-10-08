# 배포

CI/CD, Compose, 공개 설정은 이 저장소에서 관리한다. 배포는 `home-server`의 Compose나 스크립트를 호출하지 않는다. 공용 Nginx와 Docker 네트워크 `backend-net`은 현재 서버 것을 사용한다.

| 브랜치 | GitHub Environment | 컨테이너 | Spring profile | 주소 |
|---|---|---|---|---|
| main | production | jandi-band | prod | https://rhythmeet-be.yeonjae.kr |
| dev | development | jandi-band-dev | dev | https://rhythmeet-dev-be.yeonjae.kr |

PR에서는 Docker 테스트와 실행 이미지 빌드를 검증한다. main/dev push 또는 해당 브랜치 수동 실행은 검증 후 ARM64 이미지를 GHCR에 올리고, digest를 지정하여 배포한다. Redis는 Docker 테스트 단계에서만 임시로 실행한다.

## 공개 설정과 비밀값

- `config/application.properties`: 공통 설정과 `${DB_PASSWORD}` 등의 환경변수 참조
- `config/application-prod.properties`: 운영 설정
- `config/application-dev.properties`: 개발 설정
- `.env.example`: 필요한 비밀값의 이름만 제공
- `/opt/jandi-band/production/.env`, `/opt/jandi-band/development/.env`: 서버에서 수동 관리하는 실제 값

Spring의 `prod`/`dev` 프로파일이 공통 파일과 각 프로파일 파일을 읽는다. 두 환경 모두 `ddl-auto=validate`를 사용한다. 스키마 변경은 CI/CD에서 실행하지 않는다. `.env`의 DB 연결 대상도 운영과 개발을 구분한다.

`.env` 파일 권한은 600, 환경 디렉터리는 700으로 유지한다. Compose 2.30 이상의 `env_file.format: raw`를 사용하므로 값은 따옴표로 감싸지 않는다. 배포는 `.env`를 덮어쓰지 않으며, 실제 값은 이미지·Git·빌드 산출물에 포함하지 않는다.

## GitHub 설정

각 Environment에서 다음을 설정한다. production은 main, development는 dev 브랜치만 허용한다.

| 종류 | 이름 | 내용 |
|---|---|---|
| Secret | OCI_SSH_KEY | 환경별 배포 전용 SSH 개인키 |
| Variable | OCI_HOST | OCI SSH 호스트 |
| Variable | OCI_USER | SSH 사용자 |
| Variable | OCI_KNOWN_HOSTS | 검증한 SSH 호스트 공개키 항목 |

GHCR은 자동 발급되는 `GITHUB_TOKEN`으로 `ghcr.io/jandigoorm/jandi-band`를 게시하고 읽는다. 사용자 PAT를 별도로 등록하지 않는다. 기존 패키지를 재사용하면 저장소 접근 권한을 먼저 부여한다.

## 배포와 복구

서버의 `/opt/jandi-band/{production,development}/releases/`에 Compose, 설정 파일, 배포 스크립트가 릴리스별로 저장된다. `current`는 마지막 정상 릴리스를 가리킨다. `deployment.env`는 비밀값 없이 이미지 digest와 컨테이너 이름 등만 저장한다.

이미지의 아키텍처·리비전 확인, 컨테이너 상태 확인, Nginx 검사와 reload가 성공해야 배포를 완료한다. 실패하면 이전 릴리스로 복구하고 CI 작업은 실패 상태를 유지한다. 최초 전환에서는 기존 컨테이너를 정지·이름 변경하여 복구용으로 보존하고 자동 재시작을 해제한다.

```bash
cd /opt/jandi-band/production/current
docker compose --env-file deployment.env -f compose.yaml ps
```

Nginx는 `jandi-band:8080`, `jandi-band-dev:8080`으로 연결한다. 공용 게이트웨이 관리 위치를 변경해도 앱의 배포 파일은 이 저장소에 유지한다.
