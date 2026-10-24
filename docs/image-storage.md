# 이미지 저장소

OCI Object Storage의 S3 호환 API를 사용한다.
`ObjectStorageConfig`, `ObjectStorageService`, `ImageFileManagementUtil`이 설정·통신·파일 정리를 담당한다.
별도 공급자 인터페이스나 전략 계층은 두지 않는다.

## 설정

| 환경변수 | 용도 |
|---|---|
| `IMAGE_STORAGE_ACCESS_KEY` | 앱 전용 객체 접근 키 |
| `IMAGE_STORAGE_SECRET_KEY` | 접근 키의 비밀값 |
| `IMAGE_STORAGE_ENDPOINT` | OCI S3 호환 endpoint |
| `IMAGE_STORAGE_REGION` | 버킷 리전 |
| `IMAGE_STORAGE_PATH_STYLE` | 경로 방식 사용 여부 |
| `IMAGE_STORAGE_BUCKET` | 운영 이미지 버킷 |
| `IMAGE_STORAGE_PUBLIC_URL` | 기존 공개 이미지 도메인 |

비밀값은 Git 외부 `.env`에서 관리한다. endpoint가 없으면 시작에 실패한다.
기존 공개 URL과 객체 key를 유지하므로 프론트엔드와 저장된 이미지 주소를 바꾸지 않는다.
실제 도메인·버킷 매핑과 백업 보관 정책은 개인 OCI 운영 문서에서 관리한다.

## 변경 순서

새 파일 검증·업로드 후 DB를 변경한다. DB rollback이면 새 객체를 정리한다.
기존 객체는 commit 이후 삭제한다. 기본 이미지와 외부 도메인의 이미지는 삭제하지 않는다.
사진 교체·갤러리·공지·홍보 글의 기존 응답 규격과 트랜잭션 동작을 유지한다.

## 검증

```powershell
pwsh -NoProfile -File scripts/test-image-storage.ps1
```

위 검사는 Docker 백엔드와 임시 객체 서버에서 인증 업로드·특수문자 key·파일 바이트·Content-Type·삭제 보호·최종 404를 검사한다.
외부 저장소 검증에는 Git 외부의 폐기 가능한 테스트 버킷 설정만 사용한다.
전체 도메인 흐름과 장애 처리는 `docs/image-api-tests.md`를 따른다.

[OCI S3 호환 API](https://docs.oracle.com/en-us/iaas/Content/Object/Tasks/s3compatibleapi.htm)
