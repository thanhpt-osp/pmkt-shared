# Entity Spec Template

> Copy file này thành `<feature>/entity-spec.md` rồi điền vào. Xoá comment hướng dẫn `<!-- ... -->`.

## 1. Bối cảnh

<!-- Liên kết BA UC: pmkt-docs/ba/E-XXXXX_<feature>/UC_<feature>.md
     Liên kết Epic: PMKT-E-XXXXX
     Service owner: pmkt-<svc>-service / module <module> -->

## 2. Domain Aggregate

**Aggregate Root**: `<Tên>`

**Sub-Entity** (component của aggregate, lifecycle theo root):

| Class | Loại | Lý do nằm trong aggregate |
|---|---|---|
| `<TenSubEntity>` | Entity / Value Object | <vì invariant X cần tx-atomic> |

**Reference (KHÔNG nằm trong aggregate)**:

| Class | Aggregate gốc | Lý do reference, không component |
|---|---|---|
| `<TenRef>` | `<Service.Aggregate>` | <quản lý lifecycle độc lập, cross-module> |

## 3. Field schema

```
<TenAggregate> {
  id:           UUIDv7    (ADR-E, application-generated)
  tenantId:     UUID      (Inv-4 — required mọi aggregate trừ kernel)
  rowVersion:   long      (optimistic lock, BIGINT)
  createdAt:    Instant   (audit)
  createdBy:    UUID      (audit)
  updatedAt:    Instant   (audit)
  updatedBy:    UUID      (audit)

  -- Domain fields --
  <field1>:     <type>    <comment business meaning>
  <field2>:     <type>    <constraint nếu có>
}
```

## 4. State machine (nếu có)

```
   ┌─────────────┐
   │  <STATE_1>  │ (initial)
   └──────┬──────┘
          │ <event/action>
   ┌──────▼──────┐
   │  <STATE_2>  │
   └──────┬──────┘
          │ <event/action>
   ┌──────▼──────┐
   │  <STATE_3>  │ (terminal)
   └─────────────┘
```

Enum: `<TenEnum>` — values `{ V1, V2, V3 }`.

**Transition rules**:

| From | To | Trigger | Pre-condition | Side effect |
|---|---|---|---|---|
| V1 | V2 | `<action>` | <điều kiện business> | <event sinh ra hoặc table thay đổi> |

## 5. Invariant áp dụng

| Inv | Tên | Áp dụng cụ thể ở entity này |
|---|---|---|
| Inv-1 | CQRS read replica | Read query → `postgres-read` replica; write → `postgres-primary` |
| Inv-2 | DB boundary per-service | Schema `pmkt_<svc>`; KHÔNG cross-schema FK |
| Inv-3 | Audit trail tx + outbox | `INSERT INTO pmkt_audit.audit_log` cùng transaction; outbox event `<Event>` |
| Inv-4 | Tenant scope | `WHERE tenant_id = :currentTenant` qua Hibernate Filter |
| Inv-5 | Số chứng từ unique | `(tenant_id, loai_chung_tu, so_chung_tu)` partial unique |
| Inv-6 | Bút toán cân | `SUM(no) = SUM(co)` per `but_toan_id` |
| Inv-7 | Idempotent ghi nghiệp vụ | `idempotency_key` UUID trên `<bảng>` |
| Inv-8 | Append-only audit | INSERT-only via role `pmkt_audit_writer` |

## 6. Business rule

- **BR1**: <mô tả rule + reference BA UC step>
- **BR2**: <mô tả rule>

## 7. Domain event

| Event | Khi nào | Payload schema version | Consumer service |
|---|---|---|---|
| `<TenEvent>` v1 | Sau khi `<action>` thành công, cùng transaction | `pmkt-shared:.../event/<TenEvent>V1.java` | pmkt-reporting (B-A), pmkt-audit-notification |

Event versioning theo Rule §7 #13: BACKWARD compatible; thay đổi schema = `<TenEvent>V2` parallel publish.

## 8. Hibernate mapping note

- Map enum bằng `@Enumerated(EnumType.STRING)` (KHÔNG ORDINAL).
- `UUIDv7` qua `@GeneratedValue` + factory trong aggregate constructor (ADR-E).
- `BigDecimal` MoneyAmount: `precision=18 scale=2`; TyGia/UnitPrice: `precision=24 scale=8`.
- Soft-delete: KHÔNG dùng (ADR-F soft-posting đã handle). Hard delete via tx + audit.
- `@Where(clause = "tenant_id = ...")` — KHÔNG dùng. Hibernate Filter explicit thay vì.

## 9. Repository contract

Spring Data JPA `<TenAggregate>Repository extends JpaRepository<TenAggregate, UUID>`.

Custom query methods:

```java
Optional<TenAggregate> findByTenantIdAndId(UUID tenantId, UUID id);
Page<TenAggregate> findAllByTenantId(UUID tenantId, Pageable pageable);
boolean existsByTenantIdAndUniqueKey(UUID tenantId, String uniqueKey);
```

Read query với projection (DTO) cho hot path → `<TenAggregate>Projection` interface.

## 10. Test plan

- Unit test: 1 file per domain rule (Inv check) + factory method.
- Integration test: `@DataJpaTest` (Testcontainers Postgres) — happy path + edge case + concurrent update (optimistic lock).
- ArchUnit: layer dependency rule (Rule §7 #1) verify CleanArchTest.
- Coverage: per Rule §7 #5 (70% line / 60% branch / 80% class).

## 11. Migration impact

- Flyway `V<version>__<table>.sql` create table + index + role grant.
- Audit role grant pattern (Inv-3): `pmkt_audit_writer` INSERT-only on `pmkt_audit.audit_log`.

## Liên quan

- BA UC: `<link>`
- ADR: ADR-A..H BDR §2
- ERD: `erd.md` (same folder)
- API: `api-spec.md` (same folder)
- UI: `ui-spec.md` (same folder)
- Rule: [../../rules/accounting-invariants.md](../../rules/accounting-invariants.md)
