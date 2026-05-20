# API Spec Template

> Copy file này thành `<feature>/api-spec.md`. Tuân theo Rule §7 #8 RFC 9457 + OpenAPI 3.1 + URI versioning + camelCase Vietnamese.

## 1. Bối cảnh

<!-- Service: pmkt-<svc>-service  | Module: <module>
     Base URI: /api/v1/<resource> (qua Kong route)
     Authentication: Bearer JWT (Keycloak realm pmkt-mvp1) -->

## 2. Endpoint matrix

| Method | Path | Mục đích | Idempotent | Auth |
|---|---|---|---|---|
| `GET` | `/api/v1/<resource>` | List với pagination | ✅ | RBAC `pmkt-<svc>-reader` |
| `GET` | `/api/v1/<resource>/{id}` | Get by ID | ✅ | RBAC `pmkt-<svc>-reader` |
| `POST` | `/api/v1/<resource>` | Tạo mới | ❌ + `Idempotency-Key` header (Inv-7) | RBAC `pmkt-<svc>-writer` |
| `PUT` | `/api/v1/<resource>/{id}` | Cập nhật full | ✅ + `If-Match: <rowVersion>` (optimistic lock) | RBAC `pmkt-<svc>-writer` |
| `PATCH` | `/api/v1/<resource>/{id}` | Cập nhật partial | ✅ + `If-Match` | RBAC `pmkt-<svc>-writer` |
| `DELETE` | `/api/v1/<resource>/{id}` | Xoá | ✅ + `If-Match` | RBAC `pmkt-<svc>-admin` |
| `POST` | `/api/v1/<resource>/{id}/<action>` | Action verb (vd `ghi-so`) | ❌ + `Idempotency-Key` | RBAC role nghiệp vụ |

## 3. Schema DTO

### Request DTO

```yaml
<TenResource>CreateRequest:
  type: object
  required: [<field1>, <field2>]
  properties:
    <field1>:
      type: string
      maxLength: 50
      pattern: "^[A-Z0-9]+$"
      description: "Mô tả nghiệp vụ"
    <field2>:
      type: integer
      format: int64
      minimum: 1
    <field3>:
      $ref: '#/components/schemas/<NestedType>'
```

### Response DTO

```yaml
<TenResource>Response:
  type: object
  required: [id, tenantId, rowVersion, createdAt]
  properties:
    id:           { type: string, format: uuid }
    tenantId:     { type: string, format: uuid }
    rowVersion:   { type: integer, format: int64 }
    createdAt:    { type: string, format: date-time }
    <field1>:     { type: string }
    # ... tất cả field từ entity-spec ngoại trừ field nội bộ (audit timestamps OK,
    # nhưng KHÔNG expose hibernate-managed columns trừ rowVersion)

<TenResource>Page:
  type: object
  required: [content, page, totalElements]
  properties:
    content:       { type: array, items: { $ref: '#/components/schemas/<TenResource>Response' } }
    page:          { type: integer }
    size:          { type: integer }
    totalElements: { type: integer, format: int64 }
    totalPages:    { type: integer }
```

## 4. RFC 9457 error code

Mọi error trả về `application/problem+json`:

```json
{
  "type": "https://api.pmkt.dopai.com/problems/<error-slug>",
  "title": "<Tóm tắt error tiếng Việt>",
  "status": 400,
  "detail": "<Detail cụ thể, KHÔNG leak SQL/stack/PII>",
  "instance": "/api/v1/<resource>",
  "errorCode": "<MODULE>.<ERROR_NAME>",
  "fields": {
    "<field>": "<lỗi field-specific>"
  },
  "traceId": "<correlation-id>"
}
```

| HTTP | errorCode | Khi nào |
|---|---|---|
| 400 | `<MODULE>.VALIDATION_FAILED` | Bean validation fail |
| 400 | `<MODULE>.<BR_NAME>_FAIL` | Business rule fail (vd `<MODULE>.SO_CHUNG_TU_DUPLICATE`) |
| 401 | `AUTH.TOKEN_INVALID` | JWT decode fail / expired (auto từ Spring Security) |
| 403 | `AUTH.FORBIDDEN` | Thiếu role; Inv-4 tenant mismatch |
| 404 | `<MODULE>.NOT_FOUND` | Entity không tồn tại (cùng tenant) |
| 409 | `<MODULE>.OPTIMISTIC_LOCK_FAIL` | If-Match rowVersion mismatch |
| 409 | `<MODULE>.IDEMPOTENCY_MISMATCH` | Idempotency-Key trùng nhưng payload khác |
| 412 | `<MODULE>.PRECONDITION_FAILED` | Pre-condition (vd kỳ đã đóng — ADR-A) |
| 422 | `<MODULE>.BUSINESS_RULE_VIOLATION` | Inv violation (vd Inv-6 bút toán không cân) |
| 429 | `RATE_LIMIT.EXCEEDED` | Kong rate-limit (auto) |
| 500 | `INTERNAL.ERROR` | Lỗi không xử lý — KHÔNG leak stack |

## 5. Standard header

### Request
- `Authorization: Bearer <JWT>` (mọi /api/** endpoint)
- `Idempotency-Key: <UUIDv4>` (POST + action verb Inv-7)
- `If-Match: <rowVersion>` (PUT/PATCH/DELETE optimistic lock)
- `X-Request-ID: <UUIDv4>` (correlation; Kong tự thêm nếu thiếu)
- `Accept-Language: vi-VN` (i18n; default vi-VN)

### Response
- `X-Request-ID` echo back
- `ETag: <rowVersion>` (sau write thành công)
- `X-Total-Count` (list endpoint)

## 6. OpenAPI 3.1 file

Vị trí: `src/main/resources/openapi/<feature>.yaml`. Build qua springdoc-openapi (TD-05 B5).

## 7. Test plan

- **Contract test**: Pact / springdoc snapshot — verify schema không drift.
- **Integration test**: MockMvc + Testcontainers Postgres — 1 test per error code (10+ test).
- **Security test**: 1 endpoint × 4 scenario (no token, expired token, wrong tenant, wrong role) → expect 401/403.
- **Idempotency test**: POST với same Idempotency-Key + same payload → return same 201; same key + diff payload → 409.
- **Optimistic lock test**: 2 concurrent PUT → 1 success 200, 1 fail 409.

## Liên quan

- BA UC: `<link>`
- Entity spec: `entity-spec.md`
- ERD: `erd.md`
- UI spec: `ui-spec.md`
- Rule: [../../rules/api-contract-rules.md](../../rules/api-contract-rules.md)
- TD-05: springdoc-openapi (defer B5 — Phase 5.6)
