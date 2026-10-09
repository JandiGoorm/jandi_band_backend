# 이미지 저장소

백엔드는 Cloudflare R2에 이미지를 저장한다. 코드·설정 계약·검증 방법은 이 저장소에서 관리한다. 실제 버킷·도메인 매핑과 백업 위치는 개인 Notion의 OCI 운영 문서에서 관리하며, 자격증명과 사용자 이미지 목록은 문서에 넣지 않는다.

## 구현

- 기존 설정·서비스·파일 유틸 구조를 유지하면서 이름을 `R2Config`, `R2Service`, `R2FileManagementUtil`로 맞춘다. 별도 공급자 인터페이스·팩토리·전략 계층은 없다.
- `software.amazon.awssdk:s3`는 R2 통신용 SDK다. AWS 버킷 기본값·AWS 자격증명 fallback·이전 공개 URL 별칭은 제거했다. endpoint가 없으면 시작에 실패한다.
- API endpoint는 서명된 쓰기·삭제 요청용이다. 공개 URL은 DB와 API 응답에 저장하는 주소다.
- 업로드 key는 `디렉터리/UUID.확장자`다. Content-Type과 Content-Length를 보존하고 서명·전송마다 multipart 스트림을 다시 열 수 있도록 제공한다. 버킷 ACL 변경은 요청하지 않는다.
- `ImageUrls`는 현재 공개 URL의 scheme·host·port·경로 경계만 허용한다. UTF-8을 한 번 디코딩하고 공백·한글·`+`·리터럴 `%`를 구분한다. 외부·이전 URL, query/fragment, 잘못된 인코딩과 경로 이탈 요청은 삭제하지 않는다.
- 기본 이미지 `club-photo/rhythmeet.webp`와 카카오 프로필 등 외부 사진은 삭제하지 않는다.
- DB는 전체 URL을 유지한다. 스키마 변경 없이 홍보 사진의 소프트 삭제 행을 재사용한다. 일회성 복사·DB 변환 도구는 Git 이력에서만 찾을 수 있다.

## 설정 계약

공개 설정은 `config/application.properties`, 빈 비밀값 예제는 `.env.example`에 있다. 실제 환경 파일은 사용자가 저장소 밖에서 수동 관리한다.

| 환경변수 | 용도 |
| --- | --- |
| `IMAGE_STORAGE_ACCESS_KEY` | 해당 환경 R2 객체 접근 키, 필수 |
| `IMAGE_STORAGE_SECRET_KEY` | 해당 키의 비밀값, 필수 |
| `IMAGE_STORAGE_ENDPOINT` | R2 API endpoint, 필수 |
| `IMAGE_STORAGE_BUCKET` | 해당 환경 버킷, 필수 |
| `IMAGE_STORAGE_PUBLIC_URL` | 해당 환경 공개 이미지 URL, 필수 |
| `IMAGE_STORAGE_REGION` | 기본 `auto` |
| `IMAGE_STORAGE_PATH_STYLE` | 기본 `true` |

SDK는 chunked encoding을 끄고 요청·응답 체크섬을 `WHEN_REQUIRED`로 설정한다. endpoint·버킷·공개 주소·접근 권한은 같은 환경을 가리켜야 한다. 사용하지 않는 AWS 키와 이전 공개 URL 변수는 앱이 읽지 않는다.

## 업로드 검증

- JPEG/JPG, PNG, GIF, WebP를 허용한다. 대소문자 확장자는 정규화한다.
- 확장자, Content-Type, 실제 디코더 형식이 일치해야 한다. 빈 파일·위장 파일·디코딩 실패는 HTTP 400이다.
- 파일은 최대 10MiB, 첫 프레임은 최대 4천만 픽셀이다. multipart 요청 한도는 파일 외 필드를 포함해 11MiB다. 컨테이너 또는 프록시가 먼저 크기를 차단하면 HTTP 413이다.
- Java ImageIO로 첫 프레임을 읽으며 WebP 디코더는 TwelveMonkeys를 사용한다. 모든 애니메이션 프레임을 검사하거나 파일을 재인코딩하는 기능은 아니다.

## DB와 객체의 변경 순서

1. 새 파일을 검증하고 R2에 업로드한다.
2. DB 참조를 변경한다. 프로필 행, 동아리 행, 공지·홍보 행의 쓰기 잠금으로 같은 대상의 이미지 변경을 직렬화한다. 갤러리 고정과 공지 일시정지도 같은 잠금을 사용한다.
3. DB commit 후 이전 객체를 삭제한다. 삭제 대상은 트랜잭션별로 중복 제거한다.
4. DB rollback 시 이번 트랜잭션에서 업로드를 시도한 key만 삭제한다. 기존 객체는 유지한다.

갤러리 설명·공개 여부만 바꾸면 기존 객체를 유지한다. 사용자 정보와 프로필 사진은 하나의 트랜잭션으로 처리한다. 동시 변경과 rollback의 파일 손실을 막기 위한 처리이며 영속 큐·재시도 작업·분산 트랜잭션은 도입하지 않는다.

DB commit 후 객체 삭제가 실패하면 `502 / IMAGE_CLEANUP_FAILED`를 반환하며 DB와 새 이미지는 이미 저장된 상태다. 모든 삭제를 시도하고 실패 key를 서버 로그에 남긴다. 클라이언트는 생성·수정 요청을 곧바로 반복하지 말고 최신 리소스를 조회한다. 관리자는 DB의 활성 참조를 확인한 후 잔여 객체를 정리한다.

Rollback 정리 실패는 원래 오류를 가리지 않도록 서버 로그에 남긴다. 프로세스 강제 종료나 지속적인 저장소 장애에서 잔여 파일까지 자동 복구하지는 않는다. 관리자 단독 업로드·삭제는 DB 트랜잭션 없이 처리한다.

## 검증

```powershell
# 단위·통합 테스트와 운영 패키징
docker build --target test -t jandi-band:test .
docker build --target runtime -t jandi-band:local .

# 호스트에서 테스트 API 호출, 가짜 저장소 HTTP 서버 사용
pwsh -NoProfile -File ./scripts/test-image-storage.ps1

# 실제 개발 R2와 폐기 가능한 MySQL·Redis에서 전체 이미지 흐름 검사
pwsh -NoProfile -File ./scripts/test-image-api.ps1 -R2EnvFile '<Git 외부의 개발 r2.env 절대 경로>'
```

전체 API 시나리오와 한계는 [이미지 API 기능 검사](image-api-tests.md)를 따른다. 실제 배포본은 이 테스트와 별도로 배포 SHA와 이미지 조회·업로드·삭제를 확인한다. 테스트 fixture는 운영 JAR에 포함되지 않는다.

## 참고

- [Cloudflare R2 S3 API 호환성](https://developers.cloudflare.com/r2/api/s3/api/)
- [Cloudflare R2 endpoint와 권한](https://developers.cloudflare.com/r2/api/s3/tokens/)
- [TwelveMonkeys ImageIO](https://github.com/haraldk/TwelveMonkeys)
