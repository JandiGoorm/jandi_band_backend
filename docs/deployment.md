# 배포

`main`은 운영 환경 한 곳에 배포한다. `dev`와 PR은 Docker 검증만 수행한다.
운영 Spring 앱은 OCI AMD E2.1.Micro에서 실행하고, FastAPI는 별도 AMD 인스턴스에서 실행한다.

## 배포 흐름

1. Docker 단위·통합 테스트와 운영 이미지 빌드.
2. AMD64 이미지를 GHCR에 발행하고 commit SHA와 digest 고정.
3. Tailscale OIDC로 운영 호스트에 연결하고 서버의 SSH 호스트 키 검증.
4. 서버의 `/opt/jandi-band/production/.env`와 저장소의 Compose·properties 결합.
5. 새 컨테이너의 정상 상태 확인 후 Nginx 재연결. 실패하면 이전 release 복구.

GitHub `production` 환경의 `OCI_HOST`, `OCI_USER`, `OCI_KNOWN_HOSTS` 변수와
`OCI_SSH_KEY` 시크릿을 사용한다. 레지스트리는 작업별 `GITHUB_TOKEN`을 사용한다.
Tailscale OIDC 연결은 `TS_CLIENT_ID`, `TS_AUDIENCE` 변수로 구성한다.
런타임 DB·JWT·카카오·이미지 자격증명은 서버 `.env`에만 둔다.

## 자원과 데이터

앱 컨테이너 메모리는 700MiB, Java heap은 320MiB로 제한한다.
Hikari 연결 풀은 최대 5개, 최소 유휴 연결은 1개다.
관리형 MySQL과의 운영 연결은 TLS를 사용한다.
전용 CA를 넣은 `/opt/jandi-band/production/tls/mysql-truststore.p12`를 `/app/tls`에 읽기 전용으로 마운트한다.
JDBC URL의 `sslMode=VERIFY_CA`와 Connector/J 전용 truststore 옵션을 사용한다. JVM 전체 신뢰 저장소는 교체하지 않는다.

Redis 없이 `app_expiring_value` 테이블에 초대 코드와 토큰 차단 기록을 저장한다.
읽을 때 만료 여부를 검사하고, 매시간 만료 레코드를 정리한다.
첫 배포 전에 `database/001_expiring_values.sql`을 해당 운영 DB에 적용해야 한다.
앱 DB 사용자에게 스키마 변경 권한을 주지 않는다.

기존 초대 코드와 토큰 차단 기록을 이전할 때는 남은 TTL을 유지한다.
JWT 키·토큰 규격은 인프라 이전을 이유로 변경하지 않는다.

## API 호환성 확인

`ApiContractIntegrationTest`는 기존 운영 명세의 경로·요청·응답·인증·스키마를 비교한다.
서버 주소와 설명 문구는 비교 대상에서 제외한다.
이미지 HTTP 검사는 `scripts/test-image-storage.ps1`로 실행한다.
개발 환경에는 배포하지 않는다.
