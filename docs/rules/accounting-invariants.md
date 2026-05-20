# PMKT Accounting Invariants

> **Rule §7 #2 (BDR)** — Bất biến (invariant) kế toán cốt lõi. Vi phạm bất kỳ invariant nào = bug nghiệp vụ nghiêm trọng.

## WHAT — Quy tắc

### Inv-1 — Cân đối kép (Debit = Credit)

Tổng số tiền **Nợ (Debit)** của một bút toán LUÔN bằng tổng số tiền **Có (Credit)**.

```
Σ Debit (bút toán X) = Σ Credit (bút toán X)
```

Áp dụng cho:
- Mọi bút toán đơn (single posting)
- Mọi bút toán phức (compound posting)
- Mọi loại tiền (single-currency); với đa tệ → kiểm tra theo từng tệ sau quy đổi (xem Inv-7)

### Inv-2 — Sổ cái = projection (ADR-F)

Sổ cái (general ledger) KHÔNG phải bảng độc lập. Sổ cái = **projection** từ tập hợp chứng từ + bút toán theo thời gian.

```
Sổ cái(period) = Σ posting(chứng từ.status = POSTED, kỳ ∈ period)
```

Hệ quả:
- Bỏ ghi sổ một chứng từ = **xoá vật lý** bút toán liên quan (soft-posting, ADR-F)
- KHÔNG append-only (đã bác Phương án B 2026-05-19)
- Sổ cái lúc nào cũng có thể rebuild lại từ chứng từ + posting state hiện tại

### Inv-3 — Audit immutability (ADR-C)

Bản ghi audit trail (`pmkt-core-audit` module) là **append-only tại DB level**:
- Role `pmkt_audit_writer` chỉ có quyền `INSERT`
- Role `pmkt_audit_reader` chỉ có quyền `SELECT`
- `UPDATE` / `DELETE` trên audit table = **role-grant reject** (PostgreSQL refuses query)

Không có code path nào trong application được phép UPDATE/DELETE audit row. Trigger DB block để chống code bypass.

### Inv-4 — Tenant scope cô lập (ADR-D)

Mọi entity có `TenantId` column. Mọi query mặc định scoped theo `TenantId` của session hiện tại:

```sql
WHERE TenantId = :sessionTenantId AND IsDeleted = false
```

Tenant = Đơn vị kế toán (ĐVKT), 1:1 (ADR-D bàn giao BA). Cross-tenant access = **CẤM**, kể cả user nội bộ chung ĐVKT khác (ADR-D Addendum B-3 2026-05-20).

### Inv-5 — Số chứng từ unique trong DB

Số chứng từ (`SoCT`) duy nhất theo predicate:

```sql
UNIQUE (TenantId, LoaiCT, SoCT) WHERE IsDeleted = false
```

(B-6 SA-provisional 2026-05-20, **cần SME kế toán ratify trước go-live B4**.)

Không dùng Redis cache cho check unique. DB constraint = single source of truth.

### Inv-6 — Soft-posting lifecycle (ADR-F)

Chứng từ có 3 trạng thái cụ thể:

```
DRAFT → POSTED → (UNPOSTED → DRAFT)
                       ↓
                   (DELETED — soft)
```

| State | Bút toán DB | Có thể edit nội dung CT |
|---|---|---|
| DRAFT | Không có | ✓ |
| POSTED | Có (do engine ghi vào DB) | ✗ (unpost trước) |
| UNPOSTED | Xoá vật lý (rollback engine) | ✓ |
| DELETED | Không có | ✗ |

Trạng thái UNPOSTED là trạng thái logic intermediate — sau khi unpost xong chứng từ về DRAFT.

### Inv-7 — Đa tệ DECIMAL(24,8) + dual-currency ledger (ADR-G)

Mọi cột tiền:
- `Amount` (đa tệ gốc) = DECIMAL(24, 8)
- `AmountVnd` (quy đổi VND base) = DECIMAL(24, 8)
- `CurrencyCode` ISO 4217 (e.g., `USD`, `EUR`, `VND`)
- `ExchangeRate` = DECIMAL(24, 8) (tại thời điểm phát sinh)

Invariant:
```
AmountVnd = Amount × ExchangeRate
```

Ghi sổ cái lưu CẢ 2 giá trị (dual-currency). Đánh giá lại tỷ giá cuối kỳ (revaluation) = phép tính downstream trên ledger, KHÔNG sửa source posting.

### Inv-8 — Kỳ kế toán không vào quá khứ closed

Chứng từ trong kỳ kế toán đã `CLOSED` → KHÔNG cho phép insert / update / unpost. Phải mở kỳ (`REOPEN` — quyền hạn cao) trước.

```
posting(ChungTu).kỳ ∈ (kỳ_open hoặc kỳ_locked-but-reopen-authorized)
```

(ADR-A — sở hữu kỳ kế toán nằm trong core service.)

## WHY — Lý do

| Invariant | Lý do |
|---|---|
| Inv-1 Debit = Credit | Chuẩn kế toán kép quốc tế (IFRS, VAS). Vi phạm = sai báo cáo tài chính. |
| Inv-2 Sổ cái projection | Tránh duplicate state. Sổ cái và chứng từ đồng nhất tự động không phải sync. Codex CONSENSUS 2026-05-19 (ADR-F). |
| Inv-3 Audit immutability | Yêu cầu audit pháp lý — bản gốc audit phải bất biến. Vi phạm = không tin cậy được điều tra. |
| Inv-4 Tenant scope | Multi-tenant SaaS. Cross-tenant leak = vi phạm bảo mật khách hàng. ADR-D Addendum 2026-05-20 chốt nghiêm. |
| Inv-5 Số CT unique | Số chứng từ là khoá nghiệp vụ. Duplicate = nghi vấn gian lận. |
| Inv-6 Soft-posting | Cho phép sửa chứng từ đã ghi sổ (qua unpost) — UX truyền thống VN. Không cản ý đồ append-only quá khắt khe. (ADR-F bác Phương án B) |
| Inv-7 Đa tệ + dual-currency | Khách hàng XNK, VN cần USD/EUR/JPY song song với VND. DECIMAL(24,8) đủ chính xác 0.00000001 ≈ 1 satoshi. |
| Inv-8 Kỳ closed | Đảm bảo BCTC đã chốt không bị thay đổi ngầm — yêu cầu kiểm toán. |

## HOW — Enforcement

| Invariant | Gate | Vị trí |
|---|---|---|
| Inv-1 Debit = Credit | Engine ghi sổ — validate trước commit transaction; throw `DebitCreditMismatchException` | `pmkt-core-ketoantonghop` (B4) |
| Inv-2 Sổ cái projection | Architecture: KHÔNG có bảng sổ cái độc lập trong schema | DB migration baseline (T2.x B2) |
| Inv-3 Audit immutable | PostgreSQL role grant `pmkt_audit_writer = INSERT only` | Flyway V001 (T2.6 B2) + ArchUnit no-update test |
| Inv-4 Tenant scope | Hibernate `@Filter` hoặc JPA `@Where` mặc định trên mọi entity | `pmkt-core-kernel` (B4) |
| Inv-5 Số CT unique | DB constraint `UNIQUE (TenantId, LoaiCT, SoCT) WHERE IsDeleted=false` (partial index) | Flyway V001 (T2.5 B2) |
| Inv-6 Soft-posting | State machine trong `pmkt-core-chungtu`; engine reject UPDATE khi POSTED | `pmkt-core-chungtu` (B4) |
| Inv-7 Đa tệ DECIMAL(24,8) | DB column type `NUMERIC(24,8)`; JPA `BigDecimal` precision lock | Flyway V001 + JPA mapping (T2.x B2, B4) |
| Inv-8 Kỳ closed | Application check trên insert/update chứng từ → throw `PeriodClosedException` | `pmkt-core-ketoantonghop` (B4) |

Test gate:
- **Unit test** invariant logic — `pmkt-core-*/src/test/`
- **Integration test** với Testcontainers PostgreSQL — verify DB constraint thực sự reject
- **ArchUnit** verify audit code không gọi `update()` / `delete()` trên audit entity

## Liên quan

- [architecture-rules.md](architecture-rules.md) — Rule §7 #1
- ADR-A — sở hữu kỳ kế toán (Inv-8)
- ADR-C — audit transactional + outbox (Inv-3)
- ADR-D — Tenant = ĐVKT 1:1 (Inv-4)
- ADR-F — soft-posting (Inv-2, Inv-6)
- ADR-G — đa tệ + COA versioned (Inv-7)
- BDR §2 (toàn bộ ADR), §5 (DB)
- B-6 SME kế toán ratify task (Inv-5 predicate)
