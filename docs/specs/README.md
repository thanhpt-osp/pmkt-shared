# PMKT Feature Spec Templates

> **B5 Phase 5.4** — Template chuẩn cho mỗi domain feature trong PMKT MVP1. Bốn template song hành tạo "**Feature Spec Bundle**" — không thay thế BA UC (Use Case từ pmkt-docs/ba/), mà dịch UC nghiệp vụ sang artifact kỹ thuật mà DEV + FE + QA dùng song hành.

## Bốn template trong bundle

| Template | File | Audience | Mục đích |
|---|---|---|---|
| **1. Entity Spec** | `entity-spec-template.md` | BE dev, DBA | Domain object + state machine + invariant (Inv-1..7) áp dụng |
| **2. ERD** | `erd-template.md` | BE dev, DBA, BA review | DB schema chi tiết: bảng, FK, index, constraint, partial unique, Flyway version |
| **3. API Contract** | `api-spec-template.md` | BE + FE dev, QA | OpenAPI 3.1 spec endpoint, request/response schema, RFC 9457 error code, idempotency-key |
| **4. UI Spec** | `ui-spec-template.md` | FE dev, UX, BA review | Wireframe + state mapping + form validation + i18n key + accessibility |

## Quy trình spec workflow

```
BA UC ──┐
        ├─► Feature Spec Bundle (4 file) ──┬─► BE impl (entity + repo + service + controller)
        │   │                              ├─► DBA impl (Flyway V<version>__<table>.sql)
        │   ├── entity-spec                ├─► FE impl (React component + form validation + i18n)
        │   ├── erd                        └─► QA test plan + acceptance criteria
        │   ├── api-spec
        │   └── ui-spec
        │
        ├─► Review meeting: SA + BA + Tech Lead BE/FE/QA — sign-off trước impl
        │
        └─► Cập nhật BA UC nếu có gap (vd thiếu state, thiếu input validation)
```

## Naming convention

```
docs/specs/<service>/<feature>/{entity,erd,api,ui}-spec.md
```

Ví dụ thư mục pattern:
- `docs/specs/pmkt-core/danh-muc-doi-tac/entity-spec.md`
- `docs/specs/pmkt-core/danh-muc-doi-tac/erd.md`
- `docs/specs/pmkt-core/danh-muc-doi-tac/api-spec.md`
- `docs/specs/pmkt-core/danh-muc-doi-tac/ui-spec.md`

## Cross-reference

Mỗi spec phải có section "Liên quan" cross-link:

- BA UC: `pmkt-docs/ba/<epic>/UC_<feature>.md`
- BA messages: `pmkt-docs/ba/.../6.messages.md`
- ADR/BDR: `pmkt-docs/mvp1/sa/2026-05-18-base-decision-record.md` ADR-A..H
- Rule: `pmkt-shared/docs/rules/<rule>.md`
- Flyway: `pmkt-<service>-service/src/main/resources/db/migration/`
- Test: `pmkt-<service>-service/.../src/test/java/.../<feature>Test.java`

## Reference implementation hiện có (template module B2)

`pmkt-core-service/pmkt-core-danhmuc/` đã có `DanhMucDoiTac` aggregate (live code, B2 commit) cho thấy pattern đầy đủ — KHÔNG có spec doc track ngược, nhưng đối chiếu được:

| Spec template | Code location |
|---|---|
| Entity spec | `pmkt-core-danhmuc/src/main/java/com/dopai/pmkt/core/danhmuc/domain/{DanhMucDoiTac,LoaiDoiTac}.java` |
| ERD | `pmkt-core-app/src/main/resources/db/migration/V001__*.sql` + `V002__role_grant.sql` |
| API spec | `pmkt-core-danhmuc/src/main/java/com/dopai/pmkt/core/danhmuc/api/{DanhMucDoiTacController,CreateDanhMucDoiTacRequest,DanhMucDoiTacResponse}.java` |
| UI spec | (FE-1 base sẽ tạo `pmkt-web/src/features/danh-muc-doi-tac/`) |

**B5 Phase 5.4** publish template chứ chưa back-fill spec doc cho code hiện có (defer Phase 2 hoặc khi cần modify DanhMucDoiTac).

## Liên quan

- [../rules/architecture-rules.md](../rules/architecture-rules.md) §7 #1 module layering
- [../rules/accounting-invariants.md](../rules/accounting-invariants.md) Inv-1..7
- [../rules/api-contract-rules.md](../rules/api-contract-rules.md) §7 #8 RFC 9457
- [../rules/test-coverage-rules.md](../rules/test-coverage-rules.md) §7 #5 JaCoCo
- [../tech-debt-ledger.md](../tech-debt-ledger.md) TD-05 springdoc full
