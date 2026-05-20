# PMKT API Contract Rules

> **Rule §7 #8 (BDR)** — REST API contract: URI versioning + RFC 9457 ProblemDetail + OpenAPI YAML + deprecation policy. T3.9.
>
> **Trạng thái**: Convention publish B3. ProblemDetail ✅ T1.7. Full springdoc OpenAPI gen defer B4 (TD-05).

## WHAT — Quy tắc

### 1. URI versioning

```
/api/v<major>/<resource>
```

- `v1`, `v2`, ... — KHÔNG dùng `v1.0`, không có minor version trong URI.
- Bump version chỉ khi **breaking change** (xoá field, đổi type, đổi semantic).
- Additive change (thêm field optional, thêm endpoint) → giữ version, không bump.

```
GET  /api/v1/danh-muc/doi-tac
POST /api/v1/danh-muc/doi-tac
GET  /api/v1/danh-muc/doi-tac/{id}
```

Resource path **kebab-case tiếng Việt không dấu** (Inv-9 tiếng Việt nghiệp vụ): `doi-tac`, `chung-tu`, `tai-khoan`, không `partners`, `vouchers`, `accounts`.

### 2. HTTP method semantics

| Method | Idempotent | Mục đích | PMKT use |
|---|---|---|---|
| `GET` | ✅ | Đọc | list / detail / search |
| `POST` | ❌ | Tạo, hoặc action không-CRUD | create resource, post chứng từ |
| `PUT` | ✅ | Replace toàn bộ | update full resource |
| `PATCH` | ❌ (theo body) | Partial update | rename, soft-delete |
| `DELETE` | ✅ | Xoá / soft-delete | delete |

POST cho action: `/api/v1/chung-tu/{id}/ghi-so` (post chứng từ), `/api/v1/chung-tu/{id}/bo-ghi-so` (unpost). KHÔNG dùng GET cho action có side-effect.

### 3. Idempotency key

Endpoint `POST` tạo resource (chứng từ, journal entry) **bắt buộc** support `Idempotency-Key` header:

```
POST /api/v1/chung-tu
Idempotency-Key: 01938abc-7c8d-7e8f-9012-3456789abcde
```

Service lưu key + response 24h. Request lặp với cùng key trả response cached (HTTP 200). Key mismatch payload → HTTP 422.

(Impl ở pmkt-shared filter — B4 task.)

### 4. Request/Response naming

- Request DTO: record có suffix `Request` — `CreateDanhMucDoiTacRequest`.
- Response DTO: record có suffix `Response` — `DanhMucDoiTacResponse`.
- Naming method: `from(domain)` factory để convert domain → response.
- Validation: jakarta.validation (`@NotBlank`, `@NotNull`, `@Min`, `@Pattern`).

```java
public record CreateDanhMucDoiTacRequest(
    @NotBlank @Size(max = 32) String maDoiTac,
    @NotBlank @Size(max = 256) String tenDoiTac,
    @NotNull LoaiDoiTac loaiDoiTac) {}
```

### 5. Error response (RFC 9457 ProblemDetail)

**Bắt buộc** mọi error 4xx/5xx response theo schema RFC 9457:

```json
{
  "type": "https://docs.pmkt.dopai.com/errors/PMKT-CORE-1002",
  "title": "Duplicate MaDoiTac",
  "status": 409,
  "detail": "Mã đối tác 'KH001' đã tồn tại trong tenant.",
  "instance": "/api/v1/danh-muc/doi-tac",
  "code": "PMKT-CORE-1002",
  "timestamp": "2026-05-20T08:19:56Z"
}
```

Implementation: `BusinessException(ErrorCode, message)` → `SharedProblemDetailAdvice` (T1.7 ✅) tự generate.

Error code convention: `PMKT-<SERVICE>-<NNNN>` — 4-digit zero-padded:
- `PMKT-CORE-1001` → CORE service, error 1001.
- `PMKT-PLATFORM-2042` → PLATFORM service, error 2042.
- `PMKT-ERR-9999` → generic 5xx fallback.

Đăng ký error code mới: thêm enum value implement `ErrorCode` + ghi vào `docs/errors/<code>.md` (B4).

### 6. Pagination

GET list endpoint dùng query param chuẩn Spring Data:

```
GET /api/v1/danh-muc/doi-tac?page=0&size=20&sort=tenDoiTac,asc
```

Response wrap:

```json
{
  "content": [ ... ],
  "page": 0,
  "size": 20,
  "totalElements": 142,
  "totalPages": 8,
  "sort": "tenDoiTac,asc"
}
```

Default size = 20, max size = 100 (service throw 400 nếu request size > 100).

### 7. Date/time + decimal format

- Date: ISO 8601 `YYYY-MM-DD` (kế toán Việt Nam dùng theo lịch dương).
- DateTime: ISO 8601 + UTC offset — `2026-05-20T08:19:56Z`. Storage UTC, display VN local timezone là việc của FE.
- Decimal: JSON number, không string. `DECIMAL(24,8)` ở DB → JSON `123456.78`.
- Money: kèm currency code ISO 4217 trong response — `{"amount": 1500000.00, "currency": "VND"}`.

### 8. Deprecation policy

Khi đổi/xoá field hoặc endpoint:

1. **Mark deprecated** tối thiểu **2 minor release** trước khi xoá. Annotation `@Deprecated(since = "v1.3")` + OpenAPI `deprecated: true`.
2. Header response `Deprecation: true` + `Sunset: <date>` cho consumer cảnh báo.
3. Log warn mỗi request hit deprecated endpoint.
4. Xoá ở major bump (v1 → v2). Trong v1 KHÔNG xoá.

### 9. OpenAPI YAML

PMKT dùng **springdoc-openapi 2.x** (annotation-driven) sinh OpenAPI 3.1 YAML từ Controller + DTO record.

Endpoint UI: `/swagger-ui.html` (chỉ ở profile dev/staging).
Endpoint YAML: `/v3/api-docs.yaml`.

Convention annotation:
- Controller class: `@Tag(name = "Danh mục đối tác", description = "...")`.
- Method: `@Operation(summary = "...", description = "...")`.
- Response: `@ApiResponse(responseCode = "201", description = "Tạo thành công")`.

(Defer full annotation set + CI gate compare YAML diff B4 — TD-05.)

### 10. CORS + security header (B4)

Defer B4 cùng Keycloak setup (Rule §7 #10). Convention:

- Production CORS: chỉ allow origin Kong gateway, không `*`.
- Header bắt buộc: `Strict-Transport-Security`, `X-Content-Type-Options`, `Content-Security-Policy`.

## HOW — Enforce

### 11. CI gate

- Spotless + Checkstyle đảm bảo record naming convention (`*Request` / `*Response`).
- ProblemDetail policy test ở pmkt-shared ✅ T1.7.
- Defer B4:
  - springdoc YAML diff gate (so sánh với baseline đã review).
  - ArchUnit detector force `@Operation` + `@Tag` trên Controller.
  - OpenAPI lint (Spectral) trong CI.

### 12. Manual review

- PR thêm endpoint mới → reviewer check: URI versioning đúng, `*Request`/`*Response` naming đúng, validation annotation đủ.
- PR đổi response schema → flag deprecation policy, không xoá field cũ ngay.

## Liên quan

- [logging-monitoring-rules.md](logging-monitoring-rules.md) §5 — PII không leak trong response error
- [accounting-invariants.md](accounting-invariants.md) — Inv-7 multi-currency response format
- [enforcement-map.md](enforcement-map.md) — Rule #8 status
- [../tech-debt-ledger.md](../tech-debt-ledger.md) — TD-05 springdoc full gen
- T1.7 RFC 9457 — `ProblemDetailFactory`, `BusinessException`, `SharedProblemDetailAdvice`
- BDR §7 #8 — API contract
