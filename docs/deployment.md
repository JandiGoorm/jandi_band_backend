# 배포

CI/CD, Compose, 공개 설정은 이 저장소에서 관리한다. 배포는 `home-server`의 Compose나 스크립트를 호출하지 않는다. 공용 Nginx와 Docker 네트워크 `backend-net`은 현재 서버 것을 사용한다.

| 브랜치 | GitHub Environment | 컨테이너 | Spring profile | 주소 |
|---|---|---|---|---|
| main | production | jandi-band | prod | https://rhythmeet-be.yeonjae.kr |
| dev | development | jandi-band-dev | dev | https://rhythmeet-dev-be.yeonjae.kr |

PR에서는 Docker 테스트와 실행 이미지 빌드를 검증한다. main/dev push 또는 해당 브랜치 수동 실행은 검증 후 ARM64 이미지를 GHCR에 올리고, digest를 지정하여 배포한다. 각 환경의 Redis는 같은 OCI ARM에서 별도 Compose 프로젝트로 실행한다. 테스트에서는 임시 Redis를 사용한다.

## 공개 설정과 비밀값

- `config/application.properties`: 공통 설정과 `${DB_PASSWORD}` 등의 환경변수 참조
- `config/application-prod.properties`: 운영 설정
- `config/application-dev.properties`: 개발 설정
- `.env.example`: 필요한 환경변수 이름과 공개 연결 설정 예시
- `/opt/jandi-band/production/.env`, `/opt/jandi-band/development/.env`: 서버에서 수동 관리하는 실제 값

Spring의 `prod`/`dev` 프로파일이 공통 파일과 각 프로파일 파일을 읽는다. 두 환경 모두 `ddl-auto=validate`를 사용한다. 스키마 변경은 CI/CD에서 실행하지 않는다. `.env`의 DB 연결 대상도 운영과 개발을 구분한다.

`.env` 파일 권한은 600, 환경 디렉터리는 700으로 유지한다. Compose 2.30 이상의 `env_file.format: raw`를 사용하므로 값은 따옴표로 감싸지 않는다. 배포는 `.env`를 덮어쓰지 않으며, 실제 값은 이미지·Git·빌드 산출물에 포함하지 않는다.

## OCI 내부 Redis

| 환경 | 컨테이너 / 내부 호스트 | 데이터 볼륨 |
|---|---|---|
| production | `jandi-band-redis-production` | `jandi-band-redis-production_redis-data` |
| development | `jandi-band-redis-development` | `jandi-band-redis-development_redis-data` |

`compose.redis.yaml`과 `config/redis.conf`를 저장소에서 관리한다. Redis 8.6.7 공식 ARM64 이미지는 digest로 고정한다. 두 환경은 각각 DB 0을 쓰며 컨테이너·비밀번호·볼륨이 다르다. 논리 DB 번호로 환경을 나누지 않는다. `backend-net`에서만 접근하며 호스트의 6379 포트를 publish하지 않는다.

Redis에는 7일 초대 코드와 만료까지 유지하는 토큰 차단 기록이 들어간다. `maxmemory=128mb`, `noeviction`으로 만료 전 차단 기록이 메모리 압력 때문에 제거되지 않도록 한다. 컨테이너 메모리는 256MiB, CPU는 0.5개로 제한한다. 한도에 도달하면 새 쓰기가 실패하므로 사용량을 확인하고 한도를 조정한다.

AOF `everysec`과 RDB를 Docker named volume에 저장한다. 컨테이너 재생성 시 볼륨을 유지한다. `down -v`와 볼륨 삭제는 데이터를 지우므로 배포 절차에 사용하지 않는다. AOF는 최근 약 1초의 쓰기 손실 가능성이 있으며 별도 백업을 대체하지 않는다. 정기 백업 작업은 생성하지 않는다.

`.env`의 `SPRING_DATA_REDIS_HOST`는 표의 내부 호스트, `SPRING_DATA_REDIS_PORT`는 6379로 맞춘다. `REDIS_PASSWORD`는 환경별로 다른 32자 이상의 공백 없는 ASCII 문자열을 사용한다. `scripts/prepare-redis.py`는 이를 서버의 `redis-auth.conf`로 파생한다. 이 파일은 Git에 넣지 않으며 권한 640, 소유 그룹은 배포 사용자 그룹이다. Redis는 UID 999와 해당 그룹으로 실행한다.

기존 `redis-auth.conf`와 `.env` 비밀번호가 다르면 배포를 중단한다. 비밀번호를 바꿀 때는 앱을 정지한 유지보수 구간에서 `.env`를 수정하고 기존 인증 파일을 보호된 위치에 보관한 뒤 `prepare-redis.py`로 재생성한다. Redis와 앱을 함께 재생성하고 인증을 확인한다. 실행 중인 Redis 비밀번호를 일반 앱 배포가 임의로 변경하지 않는다.

앱 배포는 Redis 상태를 먼저 확인한 뒤 앱을 갱신한다. Redis 설정 마운트가 바뀌면 Redis가 재생성될 수 있지만 named volume은 유지한다. Redis healthcheck는 비인증 접근 거부를 확인하고, 앱의 `/actuator/health`는 실제 인증 연결을 확인한다. 앱과 Redis 모두 json-file 로그를 파일당 10MiB, 최대 3개로 제한한다.

```bash
cd /opt/jandi-band/production/current
docker compose --env-file deployment.env -f compose.redis.yaml ps
docker compose --env-file deployment.env -f compose.redis.yaml up -d --wait
```

## GitHub 설정

각 Environment에서 다음을 설정한다. production은 main, development는 dev 브랜치만 허용한다.

| 종류 | 이름 | 내용 |
|---|---|---|
| Secret | OCI_SSH_KEY | 환경별 배포 전용 SSH 개인키 |
| Variable | OCI_HOST | OCI의 Tailscale IPv4 주소 |
| Variable | OCI_USER | SSH 사용자 |
| Variable | OCI_KNOWN_HOSTS | 검증한 SSH 호스트 공개키 항목 |
| Variable | TS_CLIENT_ID | 해당 저장소·환경의 Tailscale OIDC Client ID |
| Variable | TS_AUDIENCE | Tailscale OIDC의 Audience |

GHCR은 자동 발급되는 `GITHUB_TOKEN`으로 `ghcr.io/jandigoorm/jandi-band`를 게시하고 읽는다. 사용자 PAT를 별도로 등록하지 않는다. 기존 패키지를 재사용하면 저장소 접근 권한을 먼저 부여한다.

## Tailscale 연결

배포 작업만 GitHub OIDC로 Tailscale에 임시 장치를 등록한다. `TS_CLIENT_ID`와 `TS_AUDIENCE`는 비밀값이 아닌 식별자다. Tailscale OAuth Secret이나 재사용 Auth Key는 저장하지 않는다. PR 검증과 이미지 게시 작업에는 `id-token: write` 권한이 없다.

서버에는 `tag:github-oci`, 임시 실행기에는 `tag:jandi-ci`를 지정한다. 접근 정책은 `tag:jandi-ci`에서 `tag:github-oci`의 `tcp:22`만 허용한다. 기본 전체 허용 규칙과 함께 사용하면 이 제한이 적용되지 않으므로 전체 허용 규칙을 제거한다. 기존 OpenSSH와 환경별 `OCI_SSH_KEY`를 사용하며, Tailscale SSH는 켜지 않는다.

운영·개발 OIDC 신뢰 설정은 각각 만든다. Issuer는 `https://token.actions.githubusercontent.com`, Scope는 `auth_keys`, Tag는 `tag:jandi-ci`다. Subject와 Custom claims는 다음 값에 정확히 일치해야 한다.

| 항목 | production | development |
|---|---|---|
| Subject | `repo:JandiGoorm/jandi_band_backend:environment:production` | `repo:JandiGoorm/jandi_band_backend:environment:development` |
| ref | `refs/heads/main` | `refs/heads/dev` |
| workflow_ref | `JandiGoorm/jandi_band_backend/.github/workflows/cicd.yml@refs/heads/main` | `JandiGoorm/jandi_band_backend/.github/workflows/cicd.yml@refs/heads/dev` |
| repository_id | `980357969` | `980357969` |
| repository_owner_id | `191837133` | `191837133` |

`OCI_KNOWN_HOSTS`의 주소도 Tailscale 주소와 일치시킨다. 호스트 공개키는 기존 관리용 SSH 경로에서 확인한 것을 사용한다. OCI 공인 IP의 화이트리스트는 유지한다. 서버와 실행기는 DNS 설정 및 다른 장치의 서브넷 경로를 받지 않는다. 실행기는 서버 연결을 확인한 뒤 배포하며, 종료 시 Tailscale 임시 장치를 제거한다.

## 배포와 복구

서버의 `/opt/jandi-band/{production,development}/releases/`에 Compose, 설정 파일, 배포 스크립트가 릴리스별로 저장된다. `current`는 마지막 정상 릴리스를 가리킨다. `deployment.env`는 비밀값 없이 이미지 digest와 컨테이너 이름 등만 저장한다.

이미지의 아키텍처·리비전 확인, 컨테이너 상태 확인, Nginx 검사와 reload가 성공해야 배포를 완료한다. 실패하면 이전 릴리스로 복구하고 CI 작업은 실패 상태를 유지한다. 최초 전환에서는 기존 컨테이너를 정지·이름 변경하여 복구용으로 보존하고 자동 재시작을 해제한다.

```bash
cd /opt/jandi-band/production/current
docker compose --env-file deployment.env -f compose.yaml ps
```

Nginx는 `jandi-band:8080`, `jandi-band-dev:8080`으로 연결한다. 공용 게이트웨이 관리 위치를 변경해도 앱의 배포 파일은 이 저장소에 유지한다.
