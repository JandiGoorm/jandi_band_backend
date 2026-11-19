# 운영 이미지 보안 업데이트

`main`이 운영 기준이다. 매일 한국 시각 05시 29분에 `Scan running OCI image`가 SSH로 `jandi-band`의 실제 registry digest를 확인하고 `linux/amd64` 이미지를 검사한다. 각 실행의 요약과 JSON 아티팩트에 HIGH·CRITICAL 항목을 남긴다.

Dependabot이 앱 의존성과 Docker 베이스 이미지의 업데이트 PR을 연다. CI는 테스트, 빌드한 후보 이미지, GHCR에 게시한 정확한 digest를 검사한다. 수정 버전이 있는 HIGH·CRITICAL 항목이 있으면 배포를 중단한다. 매주 월요일 한국 시각 03시 29분에 운영 스테이지의 캐시를 갱신해 배포판 패치도 다시 확인한다.

자동 병합은 동일 저장소에서 Dependabot이 서명한 단일 커밋의 패치 버전 또는 동일 태그 digest 갱신에 적용한다. 허용 파일, PR 대상, CI가 검증한 HEAD와 브랜치 보호 규칙을 확인한 뒤 병합하고 CI/CD를 호출한다. 그 밖의 업데이트는 PR에서 검토한다. 배포가 실패하면 기존 앱 배포 스크립트가 이전 릴리스를 복구한다.

## 이 서버의 Nginx

`gateway/compose.yaml`은 이 서버에 적용할 이미지 참조를 담는다. 라우팅, 인증서, 볼륨, 리소스 설정은 호스트의 `/opt/gateway/compose.gateway.yaml`에서 관리한다. `Roll out host Nginx`는 앱 배포 잠금을 획득하고 `nginx -t`를 거친 뒤 이미지를 교체한다. 원본 서버의 TLS 인증서와 HTTP 200을 확인하며 실패하면 실제 직전 digest를 복구한다.

Python 서버, Spring 서버, ARM 서버 순서로 진행한다. 뒤의 서버는 앞 서버의 공개 GitHub Actions 실행에서 같은 digest의 성공 기록을 확인한다. 각 저장소의 `production` 환경과 SSH 키는 자기 서버에서만 사용한다. 정기 실행 외에는 Actions의 `Roll out host Nginx`에서 `main`을 선택해 다시 실행할 수 있다.
