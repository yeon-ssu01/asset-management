# 1단계 완료 보고 — 자산 CRUD

날짜: 2026-09-29

---

## 바꾼·추가한 파일

| 파일 | 역할 |
|---|---|
| `AssetManagementApplication.java` | 패키지를 `com.yeonseong.assetmanagement`으로 수정 (CLAUDE.md 기준) |
| `global/config/JpaAuditingConfig.java` | `@EnableJpaAuditing` — createdAt/updatedAt 자동 기록 활성화 |
| `global/entity/BaseTimeEntity.java` | createdAt, updatedAt 공통 필드. 모든 엔티티가 상속 |
| `global/error/ErrorCode.java` | 에러 코드 enum (HTTP 상태코드 포함). 새 규칙 위반은 여기에 추가 |
| `global/error/BusinessException.java` | 비즈니스 규칙 위반 시 서비스가 던지는 예외 |
| `global/error/ErrorResponse.java` | API 에러 응답 형식 `{code, message, errors}` |
| `global/error/GlobalExceptionHandler.java` | 모든 컨트롤러 예외를 한 곳에서 받아 통일된 형식으로 응답 |
| `asset/domain/AssetCategory.java` | DESKTOP, LAPTOP, MONITOR, PERIPHERAL, ETC |
| `asset/domain/AssetStatus.java` | IN_STOCK, IN_USE, REPAIR, DISPOSED, LOST |
| `asset/domain/Asset.java` | asset 테이블 엔티티. `update()`, `changeStatus()` 메서드로만 수정 가능 |
| `asset/repository/AssetRepository.java` | JPA 레포지토리 + 목록 필터용 JpaSpecificationExecutor |
| `asset/dto/AssetCreateRequest.java` | 등록 요청 DTO (Bean Validation 포함) |
| `asset/dto/AssetUpdateRequest.java` | 수정 요청 DTO (status 제외) |
| `asset/dto/AssetStatusChangeRequest.java` | 상태 변경 요청 DTO |
| `asset/dto/AssetResponse.java` | 단건·목록 공통 응답 DTO |
| `asset/dto/PageResponse.java` | 페이징 응답 (content, page, size, totalElements, totalPages) |
| `asset/service/AssetService.java` | 6개 API의 비즈니스 로직 + 목록 필터 Specification |
| `asset/controller/AssetController.java` | 6개 엔드포인트 (`/api/v1/assets`) |
| `asset/service/AssetServiceTest.java` | 서비스 단위 테스트 5개 (Mock 레포지토리 사용) |
| `http/asset.http` | 성공·400·404·409 케이스 20개 |

---

## 대표 요청 처리 흐름

`POST /api/v1/assets` (자산 등록)

```
AssetController.create(@Valid @RequestBody)
  → AssetService.create(request)
      → request.toEntity()                  # assetTag trim, 빈 serialNo → null
      → AssetRepository.existsByAssetTag()  # SELECT WHERE asset_tag = ?
      → AssetRepository.existsBySerialNo()  # SELECT WHERE serial_no = ?
      → AssetRepository.save(asset)         # INSERT INTO asset ...
  ← AssetResponse.from(saved)
← ResponseEntity 201 Created + Location: /api/v1/assets/{id}
```

---

## DB에 생긴 테이블·컬럼

| 테이블 | 컬럼 | 비고 |
|---|---|---|
| `asset` | id, asset_tag, serial_no, category, manufacturer, model_name, purchase_date, purchase_price, warranty_end_date, status, location, memo, created_at, updated_at | `ddl-auto: update`로 앱 첫 실행 시 자동 생성 |

- `asset_tag`: NOT NULL, UNIQUE
- `serial_no`: UNIQUE, NULL 허용 (빈 문자열은 null로 변환해 저장)
- `category`, `status`: VARCHAR (EnumType.STRING)
- `purchase_price`: DECIMAL(12, 0)

---

## 직접 확인하는 방법

### 1. 앱 실행
```bash
# 먼저 DB 컨테이너 확인
docker compose ps

# 앱 실행 (IntelliJ 또는 터미널)
./gradlew bootRun
```

### 2. http/asset.http 요청 순서
| 요청 번호 | 설명 | 기대 결과 |
|---|---|---|
| 1번 | 자산 등록 | 201 + `Location: /api/v1/assets/1` + 응답 body |
| 6번 | 단건 조회 | 200 + status: "IN_STOCK" 확인 |
| 3번 | 자산번호 중복 | 409 `{"code":"DUPLICATE_ASSET_TAG",...}` |
| 4번 | 필수값 누락 | 400 + `errors` 배열에 필드별 사유 |
| 8번 | 전체 목록 | 200 + content 배열, totalElements |
| 15번 | REPAIR 상태 변경 | 200 + status: "REPAIR" |
| 17번 | IN_USE 직접 변경 | 409 ASSET_STATUS_CHANGE_NOT_ALLOWED |
| 19번 | 삭제 | 먼저 16번으로 IN_STOCK 복구 후 2번 자산 삭제 → 204 |

### 3. DBeaver에서 볼 것
- `asset_db` → `asset` 테이블 존재 확인
- 등록 후: `created_at`, `updated_at`이 자동으로 채워졌는지 확인
- `category`, `status` 컬럼이 숫자가 아닌 문자열(`LAPTOP`, `IN_STOCK`)로 저장됐는지 확인

---

## 에러가 나면 확인할 곳

| 증상 | 확인할 곳 |
|---|---|
| 앱이 시작 안 됨 | IntelliJ 콘솔 — DB 연결 실패(`Connection refused`) 또는 `.env` 파일 없음 |
| DB 연결 오류 | `docker compose ps` — asset-mysql 컨테이너 Up 상태인지 확인 |
| MySQL 로그 | `docker compose logs mysql` |
| 테이블이 안 생김 | 콘솔에서 `CREATE TABLE` SQL 확인 (show-sql: true) |
| 500 에러 | 콘솔 스택 트레이스 확인 |

---

## 이해 확인 질문

1. **DTO를 왜 쓰나?** 엔티티를 API로 직접 주고받으면 어떤 문제가 생길까? (힌트: `id`, `status`, 테이블 구조 노출)

2. **`@Enumerated(EnumType.STRING)` vs `ORDINAL`** — ORDINAL은 왜 위험한가? (힌트: enum 순서를 바꾸면 기존 데이터가 어떻게 되는가)

3. **`existsByAssetTag`와 `save` 사이에 같은 자산번호로 동시 요청이 들어오면?** 서비스 체크는 통과하지만 DB UNIQUE 제약이 막는다. 그때 어떤 예외가 나고, `GlobalExceptionHandler`의 어느 메서드가 처리하는가?

4. **`@Transactional(readOnly = true)`를 클래스에 달고 쓰기 메서드에만 `@Transactional`을 단 이유는?** 조회 메서드에서 읽기 전용 트랜잭션을 쓰면 DB 입장에서 무엇이 달라지나?

5. **`delete` 메서드가 지금은 IN_STOCK이 아니면 막는데, 2단계에서 뭘 추가해야 하나?** (힌트: assignment 테이블에서 해당 자산의 이력 존재 여부 확인)
