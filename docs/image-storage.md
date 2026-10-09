# 이미지 저장소 개발·이전

이 문서는 백엔드 구현, 설정 계약, 테스트와 DB URL 이전 설계를 관리한다. R2 버킷·도메인 준비, 점검 시간, 백업 보관 위치와 운영 전환 일정은 개인 Notion의 OCI 문서에서 관리한다. 실제 자격 증명과 사용자 이미지 목록은 어느 문서에도 넣지 않는다.

## 현재 구현

- `ImageStorageProperties`가 S3 호환 접속 설정을 바인딩하고 `S3Config`가 AWS SDK Java v2 클라이언트를 생성한다. SDK BOM 버전은 `build.gradle`에서 고정한다.
- API endpoint와 공개 이미지 URL은 별개다. 전자는 서명된 객체 쓰기·삭제 요청용이고 후자는 DB와 API 응답에 저장하는 주소다.
- `S3Service`는 기존 업로드·삭제 인터페이스를 유지한다. 업로드는 `디렉터리/UUID.확장자` key, 입력 스트림, Content-Type과 Content-Length를 사용한다. SDK가 서명·전송마다 multipart 스트림을 새로 열도록 제공하고 SDK가 스트림을 닫는다. 버킷 ACL·존재 확인을 매번 요청하지 않는다.
- `ImageUrls`가 URL 생성과 삭제 대상 key 판별을 담당한다. 정확한 scheme·host·port·경로 경계를 확인하고 UTF-8을 한 번만 디코딩한다. 공백·한글·`+`·리터럴 `%`를 구분한다. 경로 이탈·외부 URL·query/fragment·잘못된 인코딩은 삭제 대상에서 제외한다.
- 이전 공개 URL을 등록하면 그 URL도 key로 해석하지만, 삭제 요청은 항상 **현재 활성 버킷**으로만 보낸다. 양쪽 저장소에 쓰거나 삭제하는 기능은 없다. 따라서 이전 URL 허용은 객체 복사·DB 변환을 대신하지 않는다.
- 공용 기본 사진 `club-photo/rhythmeet.webp`는 현재/이전 도메인 모두 삭제하지 않는다. 동아리 생성·사진 초기화 시 현재 공개 URL에서 기본 사진 주소를 만든다. 이전할 때 이 객체도 반드시 복사한다.
- 카카오 등 외부 프로필은 삭제하지 않는다. 회원 정리 스케줄러 등 모든 `S3Service.deleteImage` 호출에 같은 판별을 적용한다.
- S3 SDK의 실패는 서비스에서 그대로 전달한다. 기존 `S3FileManagementUtil`의 삭제 오류 로깅 후 계속 진행하는 동작은 유지하므로, API 성공 응답만으로 저장소 정리를 완료했다고 판단하지 않는다.
- DB는 전체 URL을 유지한다. 이번 변경에서 객체 key 전용 스키마, 브라우저 직접 업로드, presigned URL, 이중 쓰기는 도입하지 않는다.

현재 운영 설정은 S3다. 이 코드 구현과 실제 R2 연결·데이터 이전·배포 완료는 별개다. `jandi_band_py`에는 직접 S3를 호출하는 코드가 없어 변경하지 않는다.

## 설정 계약

공개 설정은 `config/application.properties`, 비밀값을 비운 예제는 `.env.example`에 있다. Compose·properties·배포 스크립트는 이 저장소에서 관리한다. 실제 환경 파일은 사용자가 수동 관리하며 커밋하지 않는다.

| 환경변수 | 의미와 기본 동작 |
| --- | --- |
| `IMAGE_STORAGE_ACCESS_KEY` | 미설정 시 기존 `AWS_ACCESS_KEY_ID` 사용 |
| `IMAGE_STORAGE_SECRET_KEY` | 미설정 시 기존 `AWS_SECRET_ACCESS_KEY` 사용 |
| `IMAGE_STORAGE_ENDPOINT` | 미설정 시 SDK의 AWS endpoint 사용. R2는 해당 계정의 S3 API endpoint |
| `IMAGE_STORAGE_REGION` | 기본 `ap-northeast-2`, R2는 `auto` |
| `IMAGE_STORAGE_PATH_STYLE` | 기본 `false`, R2는 `true` |
| `IMAGE_STORAGE_BUCKET` | 기본 기존 S3 버킷. R2 환경별 버킷 이름으로 재정의 |
| `IMAGE_STORAGE_PUBLIC_URL` | 기본 기존 S3 공개 주소. R2는 HTTPS 이미지 도메인. 끝 슬래시 생략 가능 |
| `IMAGE_STORAGE_LEGACY_PUBLIC_URLS` | 선택 사항. 같은 key를 사용하는 이전 공개 주소의 쉼표 구분 목록 |

기존 S3 설정을 유지할 때는 선택적인 `IMAGE_STORAGE_*` 항목을 **설정하지 않는다**. 빈 값으로 정의하면 기본값을 덮을 수 있으므로 예제의 주석을 그대로 둔다. R2를 설정할 때는 접근 키 두 개와 endpoint·region·bucket·public URL·path style을 환경별로 함께 설정한다. `.env.example`은 실제 값을 담지 않는 계약이며 그대로 배포용 환경 파일이 되지 않는다.

애플리케이션 클라이언트는 path style 선택과 별도로 chunked encoding을 끄고, 요청/응답 체크섬을 SDK의 `WHEN_REQUIRED`로 설정한다. 테스트는 서명 scope, 비청크 PUT 본문, 객체 삭제와 403 전파를 검증한다. 실제 R2의 권한·서명 수락·공개 도메인·캐시 동작은 준비된 개발 버킷에서 추가 검증해야 한다.

## Docker 검증

```powershell
# 모든 단위·통합 테스트 (Redis 포함)
docker build --target test -t jandi-band:test .

# Windows 호스트에서 실제 API 호출까지 검증 (PowerShell 7)
pwsh -NoProfile -File ./scripts/test-image-storage.ps1

# 운영 패키징 검증
docker build --target runtime -t jandi-band:local .
```

HTTP 검증 스크립트는 `storage-smoke` 테스트 이미지를 빌드하고 임시 컨테이너를 만든다. H2·Redis·MockWebServer만 사용하며 실제 클라우드나 운영 DB에 연결하지 않는다. 앱과 테스트 저장소를 각각 `127.0.0.1:18081`, `127.0.0.1:19091`에 바인딩하므로 두 포트가 비어 있어야 한다. 성공·실패 후 자신이 만든 컨테이너를 제거한다.

검증 순서는 테스트 관리자 JWT로 PNG 업로드 → 한글·공백·플러스·퍼센트를 포함한 key의 공개 URL 조회 → 바이트와 Content-Type 대조 → 외부 URL·공용 기본 사진 삭제 방지 → 이전 도메인 URL로 삭제 → 객체 404 확인이다. 테스트 계정·JWT·DB는 컨테이너 안에서만 생성하며 운영 JAR에는 테스트 fixture가 포함되지 않는다. 이 검증은 실제 R2나 프론트 화면 검증을 대신하지 않는다.

단위 테스트는 기존 `S3ServiceTest`와 설정·동아리 생성 테스트를 확장했다. URL 판별과 실제 SDK의 HTTP 요청은 독립 테스트로 분리해 잘못된 삭제 및 공급자 호환 실패를 확인한다.

## 데이터 이전 도구

`scripts/storage/`의 Docker 이미지에서 일회성 복사, 논리 백업과 DB 주소 변환을 실행한다. 실제 URL 대응표·덤프·접근키는 저장소 밖의 보호된 디렉터리에 보관한다. 도구는 기본적으로 DB 변경을 수행하지 않는다.

| 기능 | 객체 디렉터리 | URL 컬럼 |
| --- | --- | --- |
| 프로필 | `user-photo/` | `user_photo.image_url` |
| 동아리 대표·기본 사진 | `club-photo/` | `club_photo.image_url` |
| 동아리 갤러리 | `club-gal-photo/` | `club_gal_photo.image_url` |
| 공지 | `notice-photo/` | `notice.image_url` |
| 공연 홍보 | `promo-photo/` | `promo_photo.image_url` |

관리자 업로드는 디렉터리를 입력받으므로 위 경로 밖의 객체도 전체 목록에서 조사한다. 운영·개발의 DB와 대상 버킷은 각각 지정한다.

### 복사와 검증

1. 최종 원본 key·크기·메타데이터 목록과 DB 논리 백업을 만든다. 현재 객체와 과거 버전 보관은 별도 범위다.
2. 삭제 동기화 없이 `copy` 방식으로 key를 유지해 복사한다. Content-Type·Content-Disposition·Cache-Control·사용자 메타데이터도 보존한다. 원본 AWS 권한은 읽기 전용으로 제한한다.
3. 원본과 대상의 전체 페이지를 순회해 key·크기를 비교하고 실제 객체 바이트의 SHA-256을 대조한다. ETag를 무조건 파일 해시로 취급하지 않는다.
4. 1차 복사 이후 원본에서 삭제된 사진은 최종 목록과 비교해 공개 대상에서 제외한다. 과거 복사본 때문에 사용자가 삭제한 사진을 다시 공개하지 않는다.

### 도구 실행 계약

```powershell
docker build -t rhythmeet-storage-tools -f scripts/storage/Dockerfile scripts/storage
```

도구 실행 시 `/receipts`는 저장소 밖의 보호된 결과 디렉터리를 마운트한다. 환경 파일은 `--env-file`로 전달하고 내용을 로그로 출력하지 않는다. `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`는 앱과 같은 설정을 사용한다. 컨테이너가 DB에 접근할 수 있는 네트워크에서 실행한다.

- `backup_database.py /receipts/before.sql`: 단일 트랜잭션으로 스키마·데이터를 덤프하고 SHA-256 확인서를 만든다. 기존 파일을 덮지 않는다. DB 사용자·서버 권한·이벤트·루틴 전체를 보관하는 도구는 아니다.
- `copy_objects.py /backup /receipts/copy.json`: 표준입력 JSON의 `source`·`target`에 접속 설정을 받고, 검증된 다운로드 manifest를 사용한다. 각 설정은 `access_key`, `secret_key`, `region`, `bucket`, 선택적인 `endpoint`, `addressing_style`을 사용한다. JSON 원문은 Git·로그에 남기지 않는다.
- 복사 전후 원본 목록이 다운로드 시점과 같은지 검사하고 대상의 SHA-256·길이·메타데이터를 검증한다. 기존 대상 객체는 덮지 않고 검증하며, 예상하지 못한 대상 객체가 있으면 중단한다. 원본 삭제·동기화 삭제는 수행하지 않는다.
- `migrate_urls.py --manifest /receipts/plan.json --objects /receipts/copy.json --source <기존 HTTPS URL> --target <새 HTTPS URL>`: 기본 dry-run. 검증 완료한 복사 확인서만 허용한다.
- `migrate_urls.py --mode apply --manifest /receipts/plan.json --expected <행 수>`: 검토한 대응표를 적용한다. `--mode rollback`은 같은 대응표를 조건부 역변환한다.
- 기존 manifest를 덮지 않는다. 최종 전환 시 앱 쓰기와 배치를 중지한 뒤 새로운 백업·확인서·대응표를 생성한다. 오래된 대응표를 그대로 최종 전환에 사용하지 않는다.

DB 변환 도구의 안전 조건:

- 기본 dry-run. `테이블·PK·이전 URL·새 URL` 대응표, 예상 행 수, 미존재 객체 목록을 보호된 로컬 파일로 출력한다. 실제 대응표는 Git·Notion에 올리지 않는다.
- 관리 대상의 정확한 S3 URL만 변환한다. 카카오 등 외부 주소·빈 값은 제외한다. 소프트 삭제·과거 사진의 누락 객체는 기록하고 원래 주소를 유지한다. 활성 사진의 객체가 누락되면 적용을 차단한다. 모든 후보 URL의 512자 컬럼 한계를 검사한다.
- 적용은 PK와 이전 URL이 둘 다 일치할 때만 수행한다. 예상 행 수가 다르면 트랜잭션을 중단한다. 재실행 시 이미 바뀐 URL을 다시 바꾸지 않는다.
- 역변환도 현재 값에 대한 조건부 갱신을 사용한다. 변경 이후 생긴 게시글·회원 정보가 사라지지 않도록 운영 DB 전체를 과거 덤프로 덮어쓰지 않는다.

`test_migrate_urls.py`는 폐기 가능한 `migration_test` MySQL DB에서 URL 경계·UTF-8, 외부 사진 보존, 적용/재실행/역변환, 예상 행 수 불일치, 다른 행 변경 시 전체 트랜잭션 취소, 활성/비활성 누락 객체를 검증한다. 실제 DB 이름이면 테스트가 중단된다.

### 실제 개발 버킷 HTTP 검증

```powershell
pwsh -NoProfile -File ./scripts/test-image-storage.ps1 -R2EnvFile '<Git 외부의 개발 R2 환경 파일>'
```

명시적으로 지정한 개발 버킷에만 임시 테스트 PNG를 업로드한다. 버킷 이름은 `-dev`로 끝나야 한다. H2 테스트 계정으로 실제 HTTP 업로드·공개 URL 조회·현재/이전 URL 삭제·기본 사진 보호를 확인한다. 운영 DB에는 연결하지 않는다. 파일은 `smoke/` 경로에 생성하고 테스트 중 삭제한다. 실패한 경우 남은 테스트 객체를 확인한다. 인증값은 출력하지 않는다.

공개 도메인의 캐시는 삭제 검증에 포함한다. 객체 삭제 후 같은 URL이 404여야 한다. CDN 캐시를 사용할 경우 삭제 시 purge 처리를 별도로 구현해야 하며, 현재 운영 인프라는 이미지 도메인만 edge·browser 캐시를 우회한다. 실제 도메인·버킷 매핑과 규칙 관리는 OCI 운영 문서에서 관리한다.

### 전환·복귀 검증

- 먼저 이 호환 코드를 S3 설정으로 검증한다. 다음으로 R2 개발 버킷에서 서명된 PUT/HEAD/GET/DELETE와 공개 도메인 GET을 확인한다.
- 최종 복사·DB URL 적용 중에는 쓰기 API와 `UserHardDeleteScheduler` 등 이미지 삭제 배치를 중지한다. 같은 원본을 쓰는 개발 환경도 포함한다.
- R2 설정과 DB URL을 함께 전환한 뒤 프로필·동아리 대표/갤러리·공지·홍보 사진의 조회·업로드·교체·삭제를 시험한다. 기본 객체와 외부 카카오 사진도 확인한다.
- 쓰기 재개 전 실패는 대응표와 S3 설정으로 복귀한다. 재개 후 실패는 다시 쓰기를 멈추고 R2에서 새로 생긴 현재 사용 객체를 S3로 역복사·검증한 뒤 현재 DB의 URL만 역변환한다.
- 양쪽의 오래된 URL과 CDN 캐시에는 삭제된 사진이 남을 수 있다. 실제 도메인·객체 상태·캐시 purge까지 운영 전환 기록에 남긴다.
- 프론트 담당자 작업은 별도다. 백엔드 HTTP 검증과 화면에서의 로그인·이미지 표시·교체 흐름 완료를 구분한다.

## 참고

- [Cloudflare Java SDK v2 예제](https://developers.cloudflare.com/r2/examples/aws/aws-sdk-java/)
- [R2 S3 API 호환 범위](https://developers.cloudflare.com/r2/api/s3/api/)
- [Cloudflare rclone 예제](https://developers.cloudflare.com/r2/examples/rclone/) · [rclone copy](https://rclone.org/commands/rclone_copy/) · [S3 ETag·해시 주의점](https://rclone.org/s3/)
- [AWS SDK Java v1 지원 종료](https://aws.amazon.com/blogs/developer/announcing-end-of-support-for-aws-sdk-for-java-v1-x-on-december-31-2025/)
- [AWS SDK 반복 가능한 스트림 계약](https://docs.aws.amazon.com/java/api/latest/software/amazon/awssdk/http/ContentStreamProvider.html)
