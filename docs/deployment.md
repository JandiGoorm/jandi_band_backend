# 배포

`main`은 운영 환경 한 곳에 배포한다. `dev`와 PR은 Docker 검증만 수행한다.
운영 Spring 앱은 OCI AMD E2.1.Micro에서 실행하고, FastAPI는 별도 AMD 인스턴스에서 실행한다.

## 배포 흐름

1. Docker 단위·통합 테스트와 운영 이미지 빌드.
2. AMD64 이미지를 GHCR에 발행하고 commit SHA와 digest 고정.
3. Tailscale OIDC로 운영 호스트에 연결하고 서버의 SSH 호스트 키 검증.
4. 서버의 `/opt/jandi-band/production/.env`와 저장소의 Compose·properties 결합.
5. 새 컨테이너의 정상 상태 확인 후 Nginx 재연결. 실패하면 이전 release 복구.

Spring 배포와 복구는 각각 최대 300초 동안 `/actuator/health`의 성공 응답을 기다린다. 처음 180초 동안에는 기동 중 실패를 재시도 횟수에서 제외하고, 그 안에 성공하면 즉시 배포를 계속한다. 이전 릴리스를 복구할 때도 배포 중인 릴리스의 `compose.override.yaml`을 적용한다.

### Spring 기동 기록

배포 작업 로그에서 `Spring startup diagnostics` 경로를 확인한다. OCI의 `/opt/jandi-band/production/startup-diagnostics/release.XXXXXXXX/` 아래에 새 컨테이너는 `candidate/`, Compose로 복구한 컨테이너는 `rollback/`으로 나누어 저장한다. 배포 계정만 읽을 수 있고, 다음 배포 때 14일이 지난 기록을 정리한다.

- `container.json`에 컨테이너 ID, 이미지 digest, 커밋, 시작 시각, 종료 코드, OOM 여부와 최근 Docker 헬스체크 결과를 기록한다. `result.json`에는 Compose 종료 코드와 시각을 기록한다.
- `samples.jsonl`은 기동 중 약 5초마다 해당 컨테이너의 cgroup v2에서 CPU 사용·제한 시간, CPU·메모리·I/O 대기 시간(마이크로초), 메모리·스왑 사용량(바이트), 스왑 입출력(누적 페이지 수)을 읽는다. 같은 ID와 PID의 두 표본 사이 CPU 사용 시간 증가분을 경과 시간으로 나누면 1코어 기준 사용률을 구할 수 있다. 커널이 제공하지 않는 카운터는 `null` 또는 누락으로 표시한다.
- `container.log`는 Docker 타임스탬프가 있는 표준 출력·오류다. 롤백으로 컨테이너가 사라지기 전에 호스트에 저장한다. `grep 'STARTUP ' container.log`로 Spring 이벤트의 누적 시간, 100ms 이상 걸린 초기화 단계와 빈 이름, 기동 중 5초 간격의 메인 스레드 호출 위치를 확인한다. 중첩 단계의 시간은 서로 겹칠 수 있다. 이 기록 기능이 적용되기 전 이미지의 단계는 기존 애플리케이션 로그에서 확인한다.

운영 이미지 빌드 시 APT는 Azure의 Ubuntu HTTPS 미러를 먼저 사용하고, 파일을 받을 수 없으면 kernel.org 미러에서 다시 시도한다. 일반 패키지와 보안 업데이트 모두 Ubuntu 아카이브 서명을 검증한다.

GitHub `production` 환경의 `OCI_HOST`, `OCI_USER`, `OCI_KNOWN_HOSTS` 변수와
`OCI_SSH_KEY` 시크릿을 사용한다. 레지스트리는 작업별 `GITHUB_TOKEN`을 사용한다.
Tailscale OIDC 연결은 `TS_CLIENT_ID`, `TS_AUDIENCE` 변수로 구성한다.
Nginx 배포에는 `production` 환경 변수 `TS_NGINX_CLIENT_ID`, `TS_NGINX_AUDIENCE`를 사용한다. 이 OIDC 자격 증명은 이 저장소의 `main`에 있는 `nginx-rollout.yml`만 허용한다.
런타임 DB·JWT·카카오·이미지 자격증명은 서버 `.env`에만 둔다.
배포 시 공개 `config/`는 755, properties는 644로 설정해 컨테이너의 비루트 사용자가 읽을 수 있게 한다. `.env`와 레지스트리 토큰은 이 권한 변경에 포함하지 않는다.

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
