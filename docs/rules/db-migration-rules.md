# PMKT DB Migration Rules

> **Rule §7 #9 (BDR)** — Quy ước Flyway migration. T3.10 draft + T2.1 plumbing (Batch 2).

## WHAT — Quy tắc

### 1. Tool: Flyway 11.x

Spring Boot 3.5.14 manage Flyway 11.x. Mỗi service include:

```xml
<dependency>
  <groupId>org.flywaydb</groupId>
  <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
  <groupId>org.flywaydb</groupId>
  <artifactId>flyway-database-postgresql</artifactId>
</dependency>
```

Spring Boot auto-run Flyway khi app start (config `spring.flyway.enabled=true` mặc định).

### 2. Vị trí migration

```
src/main/resources/db/migration/
├── V001__baseline.sql                 ← khởi tạo schema baseline
├── V002__role_grant.sql               ← role-grant (service-specific)
├── V003__add_table_chungtu.sql        ← business migration (B4+)
└── ...
```

KHÔNG mix `R__` (repeatable) + `V__` trừ khi cần view/function — em phải có lý do rõ trong commit message.

### 3. Naming convention

```
V<version>__<description_in_snake_case>.sql
```

- `<version>` = 3-digit zero-padded số nguyên (`001`, `002`, ..., `999`). MVP1 đủ.
- 2 underscore `__` ngăn version + description (Flyway convention).
- Description = snake_case English **HOẶC** tiếng Việt không dấu — em **chọn 1 convention / service**, không trộn.
- Ví dụ: `V001__baseline.sql`, `V015__add_chungtu_constraint.sql`, `V042__seed_tk_default.sql`.

### 4. Nội dung file migration

- **Idempotent**: `CREATE TABLE IF NOT EXISTS`, `CREATE INDEX IF NOT EXISTS` — Flyway track version nhưng best practice là re-runnable.
- **Transactional**: Flyway mặc định chạy mỗi file trong 1 transaction. KHÔNG mix DDL + DML không-transactional (e.g. `CREATE INDEX CONCURRENTLY`) — tách file riêng.
- **One concern per file**: 1 file = 1 thay đổi logic. KHÔNG dồn 10 thay đổi không liên quan.
- **KHÔNG sửa file đã merge main** — tạo migration mới `V<n+1>__` để correct. (Flyway sẽ FAIL checksum nếu phát hiện file cũ bị sửa.)

### 5. Rollback / undo

Flyway Community **KHÔNG support undo**. Chiến lược:
- Test migration ở dev env trước khi merge.
- Nếu phát hiện sai sau khi merge: tạo migration `V<n+1>__revert_<...>.sql`.
- Production cần rollback DB → restore từ backup PG (PITR — point-in-time recovery).

### 6. Baseline V001 — yêu cầu

V001 mỗi service tối thiểu phải có:
- `CREATE SCHEMA IF NOT EXISTS pmkt_<service>;`
- `SET search_path TO pmkt_<service>;`
- Extension đăng ký nếu cần (`CREATE EXTENSION IF NOT EXISTS "uuid-ossp";` cho UUIDv4 fallback)

Mỗi service tự thêm bảng business ở migration tiếp theo (V003+).

### 7. Role + grant (T2.6)

Role per service:
- `pmkt_<service>_app` — quyền `SELECT, INSERT, UPDATE, DELETE` trên schema service.
- `pmkt_audit_writer` — chỉ `INSERT` trên `pmkt_audit.*` (audit append-only — Inv-3).
- `pmkt_audit_reader` — chỉ `SELECT` trên `pmkt_audit.*`.

Role tạo qua Flyway migration → check-in code, KHÔNG manual.

### 8. Data seed

- Seed cố định (master data, COA TT200/TT99) = migration `V<n>__seed_<table>.sql`.
- Seed dev-only (sample tenant, fake user) = profile `dev` config qua Spring Boot data SQL hoặc test fixture — KHÔNG vào migration production.

## WHY — Lý do

| Quy tắc | Lý do |
|---|---|
| Flyway 11 | LTS stable, support PG 17. Spring Boot 3.5.14 managed. |
| `V001__baseline.sql` per service | DB-per-service (BDR §5 + ADR B-9) — không shared schema. |
| Naming snake_case | Linux filesystem case-sensitive; tránh space + special char. |
| Idempotent + transactional | Re-run safe khi rollback partial. Atomic per file. |
| Không sửa file merge | Checksum guard chống "lùi quay đầu" gây drift dev / prod. |
| Role-grant qua migration | Single source of truth — không phụ thuộc DBA tay. |
| Audit append-only role | Inv-3 (accounting-invariants.md) — DB-level guarantee. |

## HOW — Enforcement

| Rule | Gate | Trạng thái |
|---|---|---|
| Naming convention `V<NNN>__<snake>.sql` | Flyway built-in validate (FAIL nếu sai format) | Auto |
| Checksum không drift | Flyway `validate` chạy ở app start | Auto |
| Migration không sửa file merge | Flyway `validateOnMigrate=true` | Auto |
| Test migration ở CI | Reusable workflow gọi `mvn -Pmigration-test` (B4+) | ⏳ B4 |
| Baseline V001 tồn tại 6 service | Manual check Batch 2 Cổng 2 | ✅ Batch 2 (T2.2-T2.7) |

## Liên quan

- [accounting-invariants.md](accounting-invariants.md) — Inv-3 audit append-only
- [enforcement-map.md](enforcement-map.md) — Rule §7 #9
- BDR §5 — DB-per-service + schema convention
- Plan §4 Track 3 T3.10
- `pmkt-shared/sql/role-grant-template.sql` — template service role
- `pmkt-shared/sql/audit-role-grant.sql` — audit append-only role
