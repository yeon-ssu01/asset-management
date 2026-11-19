# 1단계 첫 슬라이스: 자산 등록 · 단건 조회

목표: `POST /api/v1/assets`로 자산을 등록하고 `GET /api/v1/assets/{id}`로 조회한다.
이 두 개가 1~8번을 전부 관통하므로, 이걸 이해하면 나머지 CRUD는 같은 패턴의 반복이다.

## 패키지 구조

`src/main/java/com/yeonseong/assetmanagement/` 아래에 이렇게 만든다.
IntelliJ에서 패키지 우클릭 → New → Package / Java Class.

```
com.yeonseong.assetmanagement
├── AssetManagementApplication.java   (이미 있음)
├── global
│   ├── config
│   │   └── JpaAuditingConfig.java
│   ├── entity
│   │   └── BaseTimeEntity.java
│   └── error
│       ├── ErrorCode.java
│       ├── BusinessException.java
│       ├── ErrorResponse.java
│       └── GlobalExceptionHandler.java
└── asset
    ├── domain
    │   ├── Asset.java
    │   ├── AssetCategory.java
    │   └── AssetStatus.java
    ├── repository
    │   └── AssetRepository.java
    ├── dto
    │   ├── AssetCreateRequest.java
    │   └── AssetResponse.java
    ├── service
    │   └── AssetService.java
    └── controller
        └── AssetController.java
```

`global`은 모든 기능이 같이 쓰는 것, `asset`은 자산 기능 전용이다.
2단계에서 `employee`, `assignment` 패키지가 `asset`과 나란히 추가된다.

---

## 1. 공통 시간 필드

모든 테이블에 들어가는 `created_at`, `updated_at`을 한 번만 정의하고 상속해서 쓴다.
값은 JPA Auditing이 저장·수정 시점에 자동으로 채운다.

### global/config/JpaAuditingConfig.java

```java
package com.yeonseong.assetmanagement.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
```

### global/entity/BaseTimeEntity.java

```java
package com.yeonseong.assetmanagement.global.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseTimeEntity {

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
```

- `@MappedSuperclass`: 이 클래스 자체는 테이블이 아니고, 상속받는 엔티티에 컬럼만 물려준다.
- `@EnableJpaAuditing`을 메인 클래스가 아닌 별도 설정 클래스에 둔 이유: 나중에 테스트를 작게 돌릴 때 메인 클래스의 설정이 딸려와서 에러 나는 걸 피하려고.

---

## 2. Enum

### asset/domain/AssetCategory.java

```java
package com.yeonseong.assetmanagement.asset.domain;

public enum AssetCategory {
    DESKTOP, LAPTOP, MONITOR, PERIPHERAL, ETC
}
```

### asset/domain/AssetStatus.java

```java
package com.yeonseong.assetmanagement.asset.domain;

public enum AssetStatus {
    IN_STOCK, IN_USE, REPAIR, DISPOSED, LOST
}
```

---

## 3. Entity

### asset/domain/Asset.java

```java
package com.yeonseong.assetmanagement.asset.domain;

import com.yeonseong.assetmanagement.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "asset")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Asset extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String assetTag;

    @Column(unique = true, length = 100)
    private String serialNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AssetCategory category;

    @Column(length = 50)
    private String manufacturer;

    @Column(nullable = false, length = 100)
    private String modelName;

    private LocalDate purchaseDate;

    @Column(precision = 12, scale = 0)
    private BigDecimal purchasePrice;

    private LocalDate warrantyEndDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AssetStatus status;

    @Column(length = 100)
    private String location;

    @Column(length = 500)
    private String memo;

    @Builder
    private Asset(String assetTag, String serialNo, AssetCategory category,
                  String manufacturer, String modelName, LocalDate purchaseDate,
                  BigDecimal purchasePrice, LocalDate warrantyEndDate,
                  String location, String memo) {
        this.assetTag = assetTag;
        this.serialNo = serialNo;
        this.category = category;
        this.manufacturer = manufacturer;
        this.modelName = modelName;
        this.purchaseDate = purchaseDate;
        this.purchasePrice = purchasePrice;
        this.warrantyEndDate = warrantyEndDate;
        this.location = location;
        this.memo = memo;
        this.status = AssetStatus.IN_STOCK;
    }
}
```

- `@GeneratedValue(IDENTITY)`: MySQL의 AUTO_INCREMENT로 id를 만든다. 설계서의 "대리키"가 이것.
- `@Enumerated(EnumType.STRING)`: DB에 `LAPTOP`처럼 이름으로 저장한다. 기본값(ORDINAL)은 0, 1, 2 숫자로 저장돼서, enum 순서를 바꾸는 순간 기존 데이터 의미가 뒤틀린다. 면접 단골 질문.
- `@NoArgsConstructor(PROTECTED)`: JPA는 빈 생성자가 필요하지만, 아무나 `new Asset()`으로 빈 자산을 만들지 못하게 막는다.
- `@Setter`가 없는 이유: 아무 곳에서나 값을 바꾸지 못하게 하려고. 수정은 나중에 `update(...)`, `changeStatus(...)` 같은 의미 있는 메서드로만 한다.
- 생성자에서 `status = IN_STOCK`: 새 자산은 무조건 재고 상태로 시작한다는 규칙을 엔티티가 직접 보장한다.

**여기까지 만들고 앱을 실행한 뒤 DBeaver에서 `asset_db`를 새로고침하면 `asset` 테이블이 생겨 있어야 한다.** 컬럼과 타입이 설계서와 맞는지 확인해 볼 것.

---

## 4. Repository

### asset/repository/AssetRepository.java

```java
package com.yeonseong.assetmanagement.asset.repository;

import com.yeonseong.assetmanagement.asset.domain.Asset;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssetRepository extends JpaRepository<Asset, Long> {

    boolean existsByAssetTag(String assetTag);

    boolean existsBySerialNo(String serialNo);
}
```

- 인터페이스만 만들면 Spring Data JPA가 구현체를 자동으로 만든다. `save`, `findById` 등은 `JpaRepository`에 이미 있다.
- `existsByAssetTag`는 메서드 이름을 해석해서 `SELECT ... WHERE asset_tag = ?` 쿼리를 만들어 준다. 콘솔에 찍히는 SQL로 확인해 볼 것 (`show-sql: true`).

---

## 5. DTO

엔티티를 API에 그대로 주고받지 않고, 요청용·응답용 클래스를 따로 둔다.
엔티티를 그대로 쓰면 클라이언트가 `id`나 `status`까지 마음대로 보낼 수 있고, 테이블 구조가 API 모양에 묶여버린다.

### asset/dto/AssetCreateRequest.java

```java
package com.yeonseong.assetmanagement.asset.dto;

import com.yeonseong.assetmanagement.asset.domain.Asset;
import com.yeonseong.assetmanagement.asset.domain.AssetCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AssetCreateRequest(
        @NotBlank(message = "자산번호는 필수입니다.")
        @Size(max = 30, message = "자산번호는 30자 이하입니다.")
        String assetTag,

        @Size(max = 100, message = "시리얼 번호는 100자 이하입니다.")
        String serialNo,

        @NotNull(message = "분류는 필수입니다.")
        AssetCategory category,

        @Size(max = 50)
        String manufacturer,

        @NotBlank(message = "모델명은 필수입니다.")
        @Size(max = 100)
        String modelName,

        LocalDate purchaseDate,

        @PositiveOrZero(message = "구매가는 0 이상입니다.")
        BigDecimal purchasePrice,

        LocalDate warrantyEndDate,

        @Size(max = 100)
        String location,

        @Size(max = 500)
        String memo
) {
    public Asset toEntity() {
        return Asset.builder()
                .assetTag(assetTag.trim())
                .serialNo(blankToNull(serialNo))
                .category(category)
                .manufacturer(manufacturer)
                .modelName(modelName)
                .purchaseDate(purchaseDate)
                .purchasePrice(purchasePrice)
                .warrantyEndDate(warrantyEndDate)
                .location(location)
                .memo(memo)
                .build();
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
```

- `record`: 필드, 생성자, getter를 자동으로 만들어 주는 자바 문법. 값을 담아 나르기만 하는 DTO에 딱 맞다. getter 이름이 `getAssetTag()`가 아니라 `assetTag()`인 점 주의.
- `blankToNull`: 시리얼을 빈 문자열 `""`로 보내면 두 번째 자산부터 UNIQUE 위반이 난다. NULL은 중복이 허용되니 빈 값은 NULL로 바꿔 저장한다.

### asset/dto/AssetResponse.java

```java
package com.yeonseong.assetmanagement.asset.dto;

import com.yeonseong.assetmanagement.asset.domain.Asset;
import com.yeonseong.assetmanagement.asset.domain.AssetCategory;
import com.yeonseong.assetmanagement.asset.domain.AssetStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record AssetResponse(
        Long id,
        String assetTag,
        String serialNo,
        AssetCategory category,
        String manufacturer,
        String modelName,
        LocalDate purchaseDate,
        BigDecimal purchasePrice,
        LocalDate warrantyEndDate,
        AssetStatus status,
        String location,
        String memo,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AssetResponse from(Asset asset) {
        return new AssetResponse(
                asset.getId(),
                asset.getAssetTag(),
                asset.getSerialNo(),
                asset.getCategory(),
                asset.getManufacturer(),
                asset.getModelName(),
                asset.getPurchaseDate(),
                asset.getPurchasePrice(),
                asset.getWarrantyEndDate(),
                asset.getStatus(),
                asset.getLocation(),
                asset.getMemo(),
                asset.getCreatedAt(),
                asset.getUpdatedAt()
        );
    }
}
```

---

## 6. 에러 처리 + Service

서비스에서 "자산번호 중복", "자산 없음" 같은 규칙 위반을 알리려면 예외 클래스가 먼저 있어야 한다.
설계서의 에러 응답 형식(`code`, `message`, `errors`)을 여기서 만든다.

### global/error/ErrorCode.java

```java
package com.yeonseong.assetmanagement.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    ASSET_NOT_FOUND(HttpStatus.NOT_FOUND, "자산을 찾을 수 없습니다."),
    DUPLICATE_ASSET_TAG(HttpStatus.CONFLICT, "이미 등록된 자산번호입니다."),
    DUPLICATE_SERIAL_NO(HttpStatus.CONFLICT, "이미 등록된 시리얼 번호입니다.");

    private final HttpStatus status;
    private final String message;
}
```

### global/error/BusinessException.java

```java
package com.yeonseong.assetmanagement.global.error;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
```

### global/error/ErrorResponse.java

```java
package com.yeonseong.assetmanagement.global.error;

import java.util.List;

public record ErrorResponse(
        String code,
        String message,
        List<FieldError> errors
) {
    public record FieldError(String field, String reason) {
    }

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.name(), errorCode.getMessage(), List.of());
    }

    public static ErrorResponse of(ErrorCode errorCode, List<FieldError> errors) {
        return new ErrorResponse(errorCode.name(), errorCode.getMessage(), errors);
    }
}
```

### global/error/GlobalExceptionHandler.java

```java
package com.yeonseong.assetmanagement.global.error;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 서비스에서 던진 규칙 위반 (중복, 없음 등)
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException e) {
        ErrorCode code = e.getErrorCode();
        return ResponseEntity.status(code.getStatus()).body(ErrorResponse.of(code));
    }

    // @Valid 검증 실패 (@NotBlank 등)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        List<ErrorResponse.FieldError> errors = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();
        return ResponseEntity.badRequest().body(ErrorResponse.of(ErrorCode.INVALID_INPUT, errors));
    }

    // JSON 형식 오류, enum에 없는 값 (category: "NOTEBOOK" 등)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(ErrorResponse.of(ErrorCode.INVALID_INPUT));
    }
}
```

- `@RestControllerAdvice`: 모든 컨트롤러에서 터진 예외를 여기서 한 번에 받아 같은 형식으로 응답한다. 이게 없으면 에러마다 스프링 기본 응답이 제각각 나간다.

### asset/service/AssetService.java

```java
package com.yeonseong.assetmanagement.asset.service;

import com.yeonseong.assetmanagement.asset.domain.Asset;
import com.yeonseong.assetmanagement.asset.dto.AssetCreateRequest;
import com.yeonseong.assetmanagement.asset.dto.AssetResponse;
import com.yeonseong.assetmanagement.asset.repository.AssetRepository;
import com.yeonseong.assetmanagement.global.error.BusinessException;
import com.yeonseong.assetmanagement.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssetService {

    private final AssetRepository assetRepository;

    @Transactional
    public AssetResponse create(AssetCreateRequest request) {
        Asset asset = request.toEntity();

        if (assetRepository.existsByAssetTag(asset.getAssetTag())) {
            throw new BusinessException(ErrorCode.DUPLICATE_ASSET_TAG);
        }
        if (asset.getSerialNo() != null && assetRepository.existsBySerialNo(asset.getSerialNo())) {
            throw new BusinessException(ErrorCode.DUPLICATE_SERIAL_NO);
        }

        Asset saved = assetRepository.save(asset);
        return AssetResponse.from(saved);
    }

    public AssetResponse get(Long id) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ASSET_NOT_FOUND));
        return AssetResponse.from(asset);
    }
}
```

- `@RequiredArgsConstructor` + `private final`: 생성자 주입. 스프링이 `AssetRepository` 구현체를 생성자로 넣어준다. `@Autowired` 필드 주입보다 권장되는 방식이고, 이유(불변성, 테스트 용이성)도 면접 단골.
- 클래스에 `readOnly = true`, 쓰기 메서드에만 `@Transactional`: 조회는 읽기 전용으로 가볍게, 저장은 트랜잭션으로 묶는다.
- 중복 체크를 `toEntity()` 뒤에 하는 이유: 앞뒤 공백을 제거하고 빈 시리얼을 NULL로 바꾼 **정리된 값**으로 비교해야 정확하다.

---

## 7. Controller

### asset/controller/AssetController.java

```java
package com.yeonseong.assetmanagement.asset.controller;

import com.yeonseong.assetmanagement.asset.dto.AssetCreateRequest;
import com.yeonseong.assetmanagement.asset.dto.AssetResponse;
import com.yeonseong.assetmanagement.asset.service.AssetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/assets")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;

    @PostMapping
    public ResponseEntity<AssetResponse> create(@Valid @RequestBody AssetCreateRequest request) {
        AssetResponse response = assetService.create(request);
        return ResponseEntity
                .created(URI.create("/api/v1/assets/" + response.id()))
                .body(response);
    }

    @GetMapping("/{id}")
    public AssetResponse get(@PathVariable Long id) {
        return assetService.get(id);
    }
}
```

- `@Valid`: 이게 있어야 DTO의 `@NotBlank` 같은 검증이 실제로 동작한다. 빼먹으면 검증이 조용히 무시된다.
- `created(...)`: 등록 성공 시 201 상태 코드 + 새 자원 주소를 `Location` 헤더로 알려준다. 설계서의 공통 규칙 그대로.
- 컨트롤러는 요청을 받아 서비스에 넘기고 응답만 만든다. 비즈니스 규칙은 여기 두지 않는다.

---

## 8. 테스트 호출

프로젝트 최상위에 `http` 폴더를 만들고 `asset.http` 파일을 만든다.
IntelliJ에서 각 요청 왼쪽의 ▶ 버튼으로 실행한다. (.http 실행 버튼이 안 보이면 아래 curl을 터미널에서 쓰면 된다.)

### http/asset.http

```http
### 1. 자산 등록 → 201
POST http://localhost:8080/api/v1/assets
Content-Type: application/json

{
  "assetTag": "HW-2026-0001",
  "serialNo": "5CG1234ABC",
  "category": "LAPTOP",
  "manufacturer": "LG",
  "modelName": "gram 16",
  "purchaseDate": "2026-03-02",
  "purchasePrice": 1890000
}

### 2. 단건 조회 → 200
GET http://localhost:8080/api/v1/assets/1

### 3. 같은 자산번호로 다시 등록 → 409 DUPLICATE_ASSET_TAG
POST http://localhost:8080/api/v1/assets
Content-Type: application/json

{
  "assetTag": "HW-2026-0001",
  "category": "MONITOR",
  "modelName": "27UP850"
}

### 4. 필수값 누락 → 400 INVALID_INPUT, errors에 필드별 사유
POST http://localhost:8080/api/v1/assets
Content-Type: application/json

{
  "assetTag": "",
  "category": "LAPTOP"
}

### 5. 없는 분류값 → 400
POST http://localhost:8080/api/v1/assets
Content-Type: application/json

{
  "assetTag": "HW-2026-0002",
  "category": "NOTEBOOK",
  "modelName": "test"
}

### 6. 없는 자산 조회 → 404 ASSET_NOT_FOUND
GET http://localhost:8080/api/v1/assets/9999
```

curl로 할 경우 (1번 예시):

```bash
curl -i -X POST http://localhost:8080/api/v1/assets \
  -H "Content-Type: application/json" \
  -d '{"assetTag":"HW-2026-0001","category":"LAPTOP","modelName":"gram 16"}'
```

매 요청 후 두 곳을 같이 볼 것.
- IntelliJ 콘솔: 어떤 SQL이 나갔는지 (`select ... where asset_tag=?` → `insert into asset ...`)
- DBeaver: `asset` 테이블에 행이 실제로 들어갔는지, `created_at`이 자동으로 채워졌는지

---

## 이해 확인 (다음으로 넘어가기 전에)

1. 1번 요청 하나가 Controller → Service → Repository → DB를 거쳐 응답으로 돌아오기까지, 어떤 파일의 어떤 메서드를 순서대로 지나는지 종이에 화살표로 그려본다.
2. 일부러 망가뜨려 본다. 먼저 결과를 예측하고 실행할 것.
   - 컨트롤러에서 `@Valid`를 지우고 4번 요청을 보내면?
   - `Asset`의 `@Enumerated(EnumType.STRING)`을 지우고 테이블을 새로 만들면 DB에 뭐가 저장될까?
   - 서비스의 `@Transactional`을 지우면 달라지는 게 있을까?
3. 설명할 수 있어야 하는 질문
   - 엔티티를 응답으로 바로 돌려주지 않고 DTO를 쓰는 이유는?
   - 중복 체크(`existsBy`)와 `save` 사이에 다른 요청이 같은 자산번호로 먼저 저장하면 어떻게 될까? (힌트: DB의 UNIQUE 제약이 막는데, 그때 어떤 예외가 나고 지금 핸들러가 그걸 처리하고 있는지)
4. 마지막으로 `AssetService`와 `AssetController`를 지우고, 이 문서를 보지 않고 다시 작성한다. 막히는 곳만 다시 보고 또 지운다.
