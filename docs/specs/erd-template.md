# ERD Template

> Copy file này thành `<feature>/erd.md` rồi điền vào. Mọi DDL phải tracebale tới Flyway version.

## 1. Bối cảnh

<!-- Service: pmkt-<svc>-service
     Schema: pmkt_<svc>
     Flyway version: V<NNN>__<feature>.sql -->

## 2. Diagram

```
┌──────────────────────────────────┐
│  <table_root>                    │
│ ─────────────────────────────────│
│ id            UUID PK            │
│ tenant_id     UUID NOT NULL      │
│ row_version   BIGINT NOT NULL    │
│ ...                              │
│ <field>       <type>             │
└─────────────┬────────────────────┘
              │ 1:N
              ▼
┌──────────────────────────────────┐
│  <table_sub>                     │
│ ─────────────────────────────────│
│ id            UUID PK            │
│ <root>_id     UUID FK NOT NULL   │
│ ...                              │
└──────────────────────────────────┘
```

## 3. DDL

```sql
-- Flyway V<NNN>__<feature>.sql
SET search_path TO pmkt_<svc>;

CREATE TABLE <table_root> (
    id              UUID            NOT NULL,
    tenant_id       UUID            NOT NULL,
    row_version     BIGINT          NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    created_by      UUID            NOT NULL,
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_by      UUID            NOT NULL,

    -- Domain columns
    <col1>          <type>          <NOT NULL / DEFAULT>,
    <col2>          <type>          <NOT NULL>,

    CONSTRAINT pk_<table_root>          PRIMARY KEY (id),
    CONSTRAINT chk_<table_root>_<rule>  CHECK (<expr>)
);

CREATE INDEX idx_<table_root>_tenant     ON <table_root>(tenant_id);
CREATE INDEX idx_<table_root>_<query>    ON <table_root>(<col>) WHERE <condition>;

-- Inv-5 partial unique (vd số chứng từ)
CREATE UNIQUE INDEX uk_<table_root>_<unique>
  ON <table_root>(tenant_id, <col1>, <col2>)
  WHERE <col3> IS NOT NULL;

-- Audit role grant (Inv-3 + ADR-C)
GRANT SELECT, INSERT, UPDATE, DELETE ON <table_root> TO pmkt_<svc>_app;
-- Audit table riêng (NẾU table này lưu PII):
-- GRANT INSERT ON pmkt_audit.audit_log TO pmkt_audit_writer;
```

## 4. Constraint nghiệp vụ

| Constraint | Loại | Định nghĩa |
|---|---|---|
| `chk_<rule>` | CHECK | Mô tả nghiệp vụ + SQL expression |
| `uk_<unique>` | UNIQUE INDEX | Inv-5 nếu áp dụng |
| `fk_<rel>` | FOREIGN KEY | Reference table khác trong CÙNG schema (Inv-2) |

**KHÔNG cho phép**:
- ❌ Cross-schema FK (vi phạm Inv-2 DB-per-service boundary)
- ❌ `ON DELETE CASCADE` nếu table được audit (audit row mất khỏi audit_log)
- ❌ Trigger ngầm (logic ẩn — đặt ở app layer Inv-3 explicit)

## 5. Index strategy

| Index | Columns | Lý do | EXPLAIN gợi ý |
|---|---|---|---|
| `idx_<tbl>_tenant` | `(tenant_id)` | Inv-4 mọi query filter tenant first | Seq scan tenant_id → bitmap heap |
| `idx_<tbl>_<col>` | `(<col>) WHERE <cond>` | Hot query path X (BR-Y) | Index scan |

## 6. Migration plan

- **Init**: V<N>__<feature>.sql (B5+)
- **Backfill**: Nếu thay đổi từ Flyway version trước → V<N+1>__alter_<table>.sql
- **Rollback**: Spring đề xuất KHÔNG rollback migration ở prod; dùng forward migration (DROP COLUMN → DROP TABLE riêng).

## 7. Performance + scaling

- **PVC size estimate**: <N> row/day × <avg row size> = X MB/year. B5 dev-grade 8Gi đủ test 1 năm.
- **Partition**: Defer Phase 2 nếu > 10M row (range partition by `tenant_id` hoặc `created_at`).
- **Replication**: postgres-primary (write) + postgres-read (Inv-1 CQRS). Read query KHÔNG dùng `postgres-primary`.

## 8. Test plan

- `@DataJpaTest` Testcontainers Postgres 16.6 verify schema apply OK.
- Concurrency: 2 thread update same row → optimistic lock fail expected.
- Constraint violation test: vi phạm `chk_<rule>` → expect `DataIntegrityViolationException`.

## Liên quan

- BA UC: `<link>`
- Entity spec: `entity-spec.md` (same folder)
- API spec: `api-spec.md` (same folder)
- Rule: [../../rules/db-migration-rules.md](../../rules/db-migration-rules.md)
- Inv: [../../rules/accounting-invariants.md](../../rules/accounting-invariants.md)
