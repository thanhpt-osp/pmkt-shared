# PMKT Domain Event Versioning Rules

> **Rule §7 #13 (BDR)** — Event envelope chuẩn + additive evolution + deprecation lane + schema registry. T3.13.
>
> **Trạng thái**: Convention publish B3. `EventEnvelope<T>` ✅ T1.7. Schema registry IT defer B4 (TD-08).

## WHAT — Quy tắc

### 1. Envelope chuẩn

Mọi event publish ra Kafka **bắt buộc** wrap trong `com.dopai.pmkt.shared.event.EventEnvelope<T>`:

```java
public record EventEnvelope<T>(
    UUID eventId,         // UUIDv7 — primary key event (idempotency)
    String eventType,     // FQN, e.g. com.dopai.pmkt.core.chungtu.ChungTuPosted
    int eventVersion,     // 1, 2, 3... semantic version event
    Instant occurredAt,   // UTC, khi event xảy ra ở producer
    UUID tenantId,        // Inv-4 — tenant scope
    UUID aggregateId,     // Root entity event xảy ra trên (ChungTu id, …)
    int schemaVersion,    // Schema version của payload
    T payload) {}
```

Factory `EventEnvelope.newEnvelope(...)` tự gen `eventId` + `occurredAt`.

### 2. Event type naming

```
com.dopai.pmkt.<service>.<aggregate>.<Aggregate><Verb>ed
```

Ví dụ:
- `com.dopai.pmkt.core.chungtu.ChungTuPosted` (chứng từ ghi sổ).
- `com.dopai.pmkt.core.chungtu.ChungTuUnposted` (chứng từ bỏ ghi sổ).
- `com.dopai.pmkt.core.danhmuc.DanhMucDoiTacCreated`.
- `com.dopai.pmkt.platform.toChuc.ToChucActivated`.

Verb past tense (`...ed`, `...Posted`). KHÔNG dùng command (`PostChungTu`) — event = thực tế đã xảy ra.

### 3. Schema evolution — additive only

Trong **cùng `eventVersion`**:

✅ Cho phép:
- Thêm field **optional** vào payload (default null hoặc absent).
- Thêm enum value mới (consumer phải handle unknown gracefully).
- Bổ sung MDC header.

❌ Không cho phép:
- Xoá field.
- Đổi kiểu field (`Integer → Long`, `String → UUID`).
- Đổi semantic (cùng field, đổi ý nghĩa).
- Đổi enum value cũ.
- Đổi `eventType` string.

Vi phạm = bump `eventVersion`.

### 4. Bump eventVersion (breaking change)

Quy trình:

1. **Phase 1 — Dual publish**: Producer publish **cả v1 + v2** ra topic. Consumer cũ vẫn đọc v1, consumer mới migrate sang v2.
2. **Phase 2 — Deprecation announce**: Mark v1 `@Deprecated`, log warning khi consumer v1 nhận message, ETA xoá ≥ **2 release**.
3. **Phase 3 — Migration**: Tất cả consumer migrate. Verify qua schema registry stats (zero v1 consumer).
4. **Phase 4 — Sunset**: Producer ngưng publish v1, xoá code path v1.

KHÔNG nhảy thẳng từ Phase 1 sang Phase 4 — consumer chưa migrate sẽ bị mất event.

### 5. Topic naming

```
pmkt.<service>.<aggregate>.<event-or-stream>.v<major>
```

Ví dụ:
- `pmkt.core.chungtu.posted.v1`
- `pmkt.core.danhmuc.doi-tac.events.v1` (multiplex nhiều event cùng aggregate)
- `pmkt.platform.tochuc.events.v1`

Topic version `v1` không đổi khi `eventVersion` bump (vì dual publish trên cùng topic). Topic bump v1→v2 chỉ khi breaking topic-level (đổi key strategy, đổi partition count drastically).

### 6. Partition key

Partition key = `tenantId.toString()` — đảm bảo:
- Order in-tenant: event cùng tenant đến consumer theo thứ tự.
- Load distribution: tenant lớn vẫn fan-out qua nhiều partition (consumer concurrency theo aggregate inside tenant).

Trừ khi có lý do (ví dụ event toàn hệ thống không thuộc tenant) — dùng `aggregateId` làm fallback.

### 7. Consumer idempotency

Consumer **bắt buộc** idempotent với `eventId`. Pattern:

- Lưu `processed_event_ids` table (PK = eventId, expire 30 ngày).
- Trước khi handle: check tồn tại → skip.
- Sau khi handle: insert vào table.
- Insert + business logic trong **cùng 1 transaction** (outbox-inbox pattern).

Nếu Kafka redeliver event (consumer crash, retry), idempotency table catch.

### 8. Producer outbox

Producer publish event **trong cùng transaction** với DB write (transactional outbox):

1. Write business data + insert `event_outbox` row trong 1 DB transaction.
2. Background poller (Debezium hoặc Spring Modulith Externalization) đọc outbox → publish Kafka.
3. Đánh dấu outbox row sent sau ack thành công.

KHÔNG publish trực tiếp trong `@Transactional` method — risk: DB commit fail nhưng Kafka đã publish (ghost event).

### 9. Payload format

- **JSON** (Spring Boot default Jackson). KHÔNG Avro / Protobuf cho MVP1 (chấp nhận size lớn + schema registry sau).
- Field naming `camelCase` (Jackson default).
- Date/Instant: ISO 8601 string format.
- Decimal: JSON number.
- Enum: string (uppercase Java enum name).

### 10. Schema registry (B4 — TD-08)

PMKT MVP1 dùng **Confluent Schema Registry** (chốt ADR — defer ratification B4).

Compatibility mode: **BACKWARD** — consumer mới đọc được event producer cũ. Match với additive evolution rule §3.

Convention:
- Subject naming: `<topic>-value` (e.g. `pmkt.core.chungtu.posted.v1-value`).
- Schema register lúc CI build (`mvn schema-registry:register`).
- Test compatibility: CI fail nếu schema mới không BACKWARD-compatible với baseline.

## HOW — Enforce

### 11. CI gate

- ✅ `EventEnvelopeTest` (B1, T1.7) — assert envelope shape.
- ⏳ **B4** Schema registry compatibility test (TD-08).
- ⏳ **B4** ArchUnit: mọi class kết thúc `*Event` phải nằm trong `event` package + có Jackson `@JsonProperty`.
- ⏳ **B4** Outbox table convention test (PK + sent flag + retry count).

### 12. Manual review

- PR thêm event mới → reviewer check naming (`<Aggregate><Verb>ed`) + envelope wrapping.
- PR đổi payload field → check additive rule §3, không xoá / đổi type.
- PR bump `eventVersion` → kèm migration plan §4 (4 phase).

## Liên quan

- [api-contract-rules.md](api-contract-rules.md) §8 — deprecation policy (cùng nguyên lý)
- [accounting-invariants.md](accounting-invariants.md) — Inv-4 tenant scope, Inv-2 ghi sổ là event
- [enforcement-map.md](enforcement-map.md) — Rule #13 status
- [../tech-debt-ledger.md](../tech-debt-ledger.md) — TD-08 schema registry
- `EventEnvelope.java` ✅ T1.7
- BDR §7 #13 — domain event versioning
