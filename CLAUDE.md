# CLAUDE.md — PC 자산관리 웹앱

이 파일은 Claude Code가 이 저장소에서 작업할 때 항상 따르는 기준이다.
설계와 규칙이 여기 있으니, 여기와 다르게 구현하고 싶으면 먼저 사용자에게 이유를 설명하고 승인을 받는다.

## 1. 프로젝트와 사용자

- 회사 IT 자산(PC·노트북·모니터 등 하드웨어 + 소프트웨어 라이선스)의 등록, 지급·회수 이력, 라이선스 배정을 관리하는 사내 웹앱.
- 핵심 가치: "지금 누가 무엇을 쓰고 있고, 과거엔 누가 썼는가"를 언제든 추적.
- 사용자(저장소 주인)는 IT 운영 직무 지원자다. 헬프데스크·SAP·데이터 관리 실무 경험이 있고 개발자 포지션을 목표로 하지 않는다.
  - 목표는 **동작하는 시스템을 만들고, 배포·운영까지 해본 뒤, 설계와 운영 방식을 면접에서 설명하는 것**.
  - 코드 한 줄 한 줄보다 데이터 구조, 요청 흐름, 장애 확인 방법, 설계 이유를 이해시키는 데 집중한다.
- 사용자와는 한국어로 소통한다. 코드 식별자(클래스, 변수, 테이블, 컬럼, API 경로)는 영어, 주석·커밋 메시지·문서는 한국어.

## 2. 작업 방식 (반드시 지킬 것)

1. **한 번에 한 단계만** 진행한다. 사용자가 요청한 단계(아래 7장 로드맵)를 넘어서 구현하지 않는다.
2. 단계를 시작하기 전에 만들 파일 목록과 순서를 짧게 제시하고 진행한다.
3. 단계를 마치면 아래 **단계 완료 보고**를 반드시 남긴다.
   - 바꾼·추가한 파일 목록과 각 파일의 역할 (한 줄씩)
   - 대표 요청 하나의 처리 흐름: Controller → Service → Repository → DB (화살표로)
   - 이번 단계에서 DB에 생기거나 바뀐 테이블·컬럼
   - 직접 확인하는 방법: 실행 명령, `http/*.http`의 어떤 요청을 보내고 어떤 결과를 기대하는지, DBeaver에서 볼 것
   - 에러가 나면 확인할 곳 (콘솔 로그, `docker compose logs mysql` 등)
   - 사용자가 답해봐야 할 질문 3~5개 (설계 이유, 운영 관점 위주)
   - 완료 보고는 화면에 출력하는 동시에 `docs/progress/YYYY-MM-DD-단계명.md` 파일로도 저장한다. (예: `docs/progress/2026-09-29-step1-asset-crud.md`)
4. 커밋 전에 `./gradlew test`가 통과해야 한다. 커밋은 사용자가 확인한 뒤에 한다.
5. 모르는 요구사항은 추측해서 채우지 말고 물어본다. 설계서에 없는 테이블·컬럼·API를 새로 만들 때도 먼저 제안하고 승인받는다.
6. 사용자가 이해하지 못한 채 넘어가지 않도록, 설명은 짧고 구체적으로. 전문 용어를 처음 쓸 땐 한 줄 설명을 붙인다.

## 3. 기술 스택과 환경

- Java 25, Spring Boot 4.x (최신 안정 버전), Gradle (Groovy DSL)
- 의존성: Spring Web, Spring Data JPA, MySQL Driver, Validation, Lombok
  - Spring Security는 **3단계에서** 추가한다. 그 전엔 넣지 않는다.
  - 엑셀은 **4단계에서** Apache POI를 추가한다.
  - Spring Boot의 Docker Compose Support 의존성은 쓰지 않는다. DB는 사용자가 직접 `docker compose up -d`로 띄운다.
- 개발 환경: Apple Silicon Mac, IntelliJ, DBeaver
- DB: MySQL 8.4를 Docker Compose로 실행 (`docker-compose.yml`, 컨테이너 이름 `asset-mysql`, DB `asset_db`, 앱 계정 `asset_user`)
- 비밀값은 프로젝트 최상위 `.env`에만 둔다. `.env`는 `.gitignore`에 있고 **절대 커밋하지 않는다.** 코드·설정 파일에 비밀번호를 직접 쓰지 않는다.
- 설정 파일은 `src/main/resources/application.yml` (YAML). 현재 내용:

```yaml
spring:
  application:
    name: asset-management
  config:
    import: optional:file:.env[.properties]
  datasource:
    url: jdbc:mysql://localhost:3306/asset_db?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=Asia/Seoul
    username: ${MYSQL_USER}
    password: ${MYSQL_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
```

- `ddl-auto: update`는 개발용이다. 6단계(운영)에서 운영 프로필은 `validate`로 바꾸고 스키마 관리 방식을 사용자와 정한다.

## 4. 코드 구조와 규칙

### 패키지 구조 (기능별)

```
com.yeonseong.assetmanagement
├── global
│   ├── config        # JPA Auditing 등 공통 설정
│   ├── entity        # BaseTimeEntity
│   └── error         # ErrorCode, BusinessException, ErrorResponse, GlobalExceptionHandler
├── asset             # domain / repository / dto / service / controller
├── department
├── employee
├── assignment
├── auth              # 3단계
└── license           # 5단계
```

각 기능 패키지 안은 `domain`, `repository`, `dto`, `service`, `controller`로 나눈다.

### 엔티티

- PK는 `Long id`, `@GeneratedValue(strategy = GenerationType.IDENTITY)`.
- 사람이 쓰는 번호(자산번호, 사번, 시리얼, 로그인 아이디)는 PK로 쓰지 않고 UNIQUE 제약으로만 건다.
- enum은 항상 `@Enumerated(EnumType.STRING)`.
- `@Setter` 금지. 생성은 `@Builder`를 붙인 private 생성자, 변경은 `update(...)`, `changeStatus(...)`처럼 의미 있는 메서드로만.
- `@NoArgsConstructor(access = AccessLevel.PROTECTED)`.
- 이력 테이블(assignment, license_allocation)을 뺀 엔티티는 `BaseTimeEntity`(created_at, updated_at, JPA Auditing)를 상속한다.
- 폐기·퇴사는 삭제가 아니라 상태 변경으로 처리한다. 이력의 FK가 깨지지 않게 하기 위해서다.

### DTO와 API

- 요청·응답은 `record` DTO로 분리한다. 엔티티를 API로 직접 주고받지 않는다.
- 요청 DTO에 Bean Validation(`@NotBlank`, `@Size` 등)과 한국어 메시지를 단다. 컨트롤러 파라미터엔 `@Valid`.
- 빈 문자열로 들어온 선택 값(예: serialNo)은 null로 정리해서 저장한다.
- 모든 API는 `/api/v1` 아래. 날짜·시각은 ISO-8601.
- 상태 코드: 조회·수정 200, 등록 201 + `Location` 헤더, 삭제 204, 검증 실패 400, 미로그인 401, 권한 없음 403, 없음 404, 비즈니스 규칙 위반 409.
- 목록은 `?page=0&size=20&sort=createdAt,desc`, 응답에 `content`, `page`, `size`, `totalElements`, `totalPages`.
- 에러 응답은 한 형식으로 통일한다.

```json
{ "code": "ASSET_ALREADY_ASSIGNED", "message": "이미 사용 중인 자산입니다.", "errors": [{ "field": "assetTag", "reason": "필수 입력입니다." }] }
```

- 에러 코드는 `ErrorCode` enum에 추가하고, 서비스에서 `BusinessException`을 던지며, `GlobalExceptionHandler`가 응답으로 바꾼다.

### 서비스·트랜잭션

- 생성자 주입(`@RequiredArgsConstructor` + `private final`).
- 서비스 클래스에 `@Transactional(readOnly = true)`, 쓰기 메서드에만 `@Transactional`.
- 중복 체크 후 저장 사이의 동시 요청은 DB UNIQUE 제약이 최종적으로 막는다. `DataIntegrityViolationException`도 409로 처리한다.

### 테스트와 확인 파일

- 단계마다 서비스 단위 테스트(성공 + 규칙 위반 케이스)를 만든다.
- `http/` 폴더에 기능별 `.http` 파일을 두고, 성공·400·404·409 케이스를 모두 넣는다. 사용자는 이걸로 직접 호출해 본다.

## 5. 데이터 모델

### 테이블

| 테이블 | 주요 컬럼 | 제약·규칙 |
| --- | --- | --- |
| asset | asset_tag, serial_no, category, manufacturer, model_name, purchase_date, purchase_price DECIMAL(12,0), warranty_end_date, status, location, memo | asset_tag NOT NULL UK, serial_no UK NULL 허용, status 기본 IN_STOCK |
| department | code, name | code UK |
| employee | emp_no, name, email, department_id, status | emp_no UK, email UK, department_id FK NOT NULL, status ACTIVE·RESIGNED |
| user_account | login_id, password_hash, role, employee_id, enabled | login_id UK, password_hash는 BCrypt, role ADMIN·USER, employee_id FK UK NULL 허용 |
| assignment | asset_id, employee_id, assigned_at, returned_at, assigned_by, note | returned_at NULL = 사용 중, assigned_by FK → user_account |
| software_license | name, vendor, license_type, total_seats, purchase_date, expires_at, cost, license_key | license_type PER_USER·PER_DEVICE, total_seats > 0, expires_at NULL = 영구, license_key는 응답에서 마스킹 |
| license_allocation | license_id, asset_id, employee_id, allocated_at, revoked_at | asset_id와 employee_id 중 정확히 하나만 채움(CHECK), revoked_at NULL = 사용 중 |

### enum 값

- AssetCategory: DESKTOP, LAPTOP, MONITOR, PERIPHERAL, ETC
- AssetStatus: IN_STOCK, IN_USE, REPAIR, DISPOSED, LOST
- EmployeeStatus: ACTIVE, RESIGNED
- Role: ADMIN, USER
- LicenseType: PER_USER, PER_DEVICE

### 관계

- department 1 : N employee
- employee 1 : 0..1 user_account
- asset 1 : N assignment, employee 1 : N assignment
- software_license 1 : N license_allocation
- asset 0..1 : N license_allocation (PER_DEVICE), employee 0..1 : N license_allocation (PER_USER)

### 핵심 규칙

- 자산 하나당 returned_at이 NULL인 assignment는 최대 1건. MySQL엔 조건부 UNIQUE 인덱스가 없으므로 서비스에서 검사하고, asset에 `@Version`(낙관적 락)을 둬서 동시 지급을 막는다(2단계에서 추가).
- 지급 시 asset.status → IN_USE, 회수 시 → IN_STOCK.
- 이력이 있는 자산은 삭제 불가(409). 폐기는 상태를 DISPOSED로.
- 사용 중 자산·라이선스가 있는 직원은 퇴사 처리 불가(409).
- 라이선스 사용 좌석 = revoked_at NULL인 배정 수. total_seats를 넘으면 배정 거부(409). 유형과 대상이 안 맞으면 400.

## 6. API 목록

| 단계 | 메서드 · 경로 | 권한 | 설명 |
| --- | --- | --- | --- |
| 1 | GET /assets | ADMIN | 목록. 필터 status, category, keyword(자산번호·모델명·시리얼), 페이징 |
| 1 | GET /assets/{id} | ADMIN | 상세 (2단계부터 현재 사용자 포함) |
| 1 | POST /assets | ADMIN | 등록. asset_tag·serial_no 중복 409 |
| 1 | PUT /assets/{id} | ADMIN | 수정 (status 제외) |
| 1 | PATCH /assets/{id}/status | ADMIN | REPAIR, DISPOSED, LOST, IN_STOCK으로 변경 |
| 1 | DELETE /assets/{id} | ADMIN | 이력 없는 IN_STOCK만 삭제, 아니면 409 |
| 2 | GET·POST /departments, PUT·DELETE /departments/{id} | ADMIN | 소속 직원 있으면 삭제 409 |
| 2 | GET·POST /employees, GET·PUT /employees/{id} | ADMIN | 목록 필터 departmentId, status, keyword |
| 2 | PATCH /employees/{id}/resign | ADMIN | 사용 중 자산·라이선스 있으면 409 |
| 2 | POST /assets/{id}/assignments | ADMIN | 지급. IN_STOCK만, 이미 사용 중이면 409 ASSET_ALREADY_ASSIGNED |
| 2 | PATCH /assignments/{id}/return | ADMIN | 회수 |
| 2 | GET /assets/{id}/assignments, GET /employees/{id}/assignments | ADMIN | 이력 조회 (`?active=true`) |
| 3 | POST /auth/login, POST /auth/logout, GET /auth/me | 누구나·로그인 | 세션 기반 인증 |
| 3 | GET /me/assets | 로그인 | 내게 지급된 자산 |
| 4 | GET /assets/import/template | ADMIN | 업로드 양식 xlsx |
| 4 | POST /assets/import | ADMIN | asset_tag 기준 upsert, `?dryRun=true`, 한 행이라도 실패하면 전체 롤백, 행별 실패 사유 반환 |
| 4 | GET /assets/export | ADMIN | 목록 필터 그대로 xlsx |
| 5 | GET·POST /licenses, GET·PUT /licenses/{id} | ADMIN | 좌석 사용 현황 포함, 사용 중보다 total_seats 축소 시 409 |
| 5 | GET /licenses/expiring?days=30 | ADMIN | 만료 임박 |
| 5 | POST /licenses/{id}/allocations, PATCH /license-allocations/{id}/revoke | ADMIN | 배정·회수 |
| 5 | GET /me/licenses | 로그인 | 내게 배정된 라이선스 |

인증은 세션 방식(Spring Security, JSESSIONID). JWT는 쓰지 않는다. 이유: 서버 한 대 + 같은 도메인 화면이고, 로그아웃·계정 잠금이 즉시 반영돼야 해서.

## 7. 로드맵

| 단계 | 범위 | 완료 기준 |
| --- | --- | --- |
| 1 | 자산 CRUD, 목록 검색·페이징, 공통 에러 처리 | asset API 전부 동작, `.http` 전 케이스 통과, 테스트 통과 |
| 2 | 부서·직원, 지급·회수 이력, asset `@Version` | 지급·회수·이력 조회 동작, 동시 지급 시 한쪽 409 |
| 3 | Spring Security 세션 로그인, ADMIN·USER 권한, 초기 관리자 계정 | 권한별 403 동작, 비밀번호 BCrypt 저장 확인 |
| 4 | 엑셀 양식·업로드·내보내기 (Apache POI) | dryRun·전체 롤백·행별 실패 사유 동작 |
| 5 | 소프트웨어 라이선스, 좌석 배정, 만료 임박 | 좌석 초과 409, 유형·대상 불일치 400 |
| 6 | 운영: 서버 배포(Docker Compose), 운영 프로필(`ddl-auto: validate`), DB 자동 백업과 복구 테스트, 로그 확인 방법, 계정 관리 절차 | README에 운영 문서, 실제로 백업에서 복구해 본 기록 |
| 화면 | 최소 CRUD 화면 (방식은 해당 시점에 사용자와 결정) | 관리자가 브라우저로 주요 기능 사용 가능 |

## 8. 문서

- `README.md`: 프로젝트 소개(왜 만들었나: 헬프데스크에서 겪은 자산 추적 문제), 기술 스택과 버전, 실행 방법(`.env` 작성 → `docker compose up -d` → 앱 실행), ERD(Mermaid `erDiagram`), API 요약, 진행 로드맵, 트러블슈팅 기록. 단계를 마칠 때마다 갱신한다.
- `docs/`: 단계별 가이드와 설계서 보관. `docs/step1-asset-create-get.md`에 1단계 첫 슬라이스(등록·단건 조회)의 참고 코드가 있다. 1단계는 이 구조와 규칙을 따른다.
- 트러블슈팅은 "문제 → 원인 → 해결" 세 줄로 README 하단에 누적한다.

## 9. 커밋

- 형식: `타입: 한국어 설명` (예: `feat: 자산 등록 API 추가`, `fix: 빈 시리얼 저장 시 중복 오류 수정`, `docs: README 실행 방법 갱신`)
- 타입: feat, fix, refactor, test, docs, chore
- 한 커밋엔 한 가지 목적만. `.env`, IDE 설정(`.idea`), 빌드 산출물은 커밋하지 않는다.
