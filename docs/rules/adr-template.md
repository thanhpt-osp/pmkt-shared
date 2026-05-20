# ADR Template — PMKT MVP1

> **Rule §7 #4 (BDR)** — Mỗi quyết định kiến trúc lớn phải có 1 ADR (Architecture Decision Record). Chuẩn hoá format + numbering + lifecycle.

## ADR là gì

Architecture Decision Record = 1 trang văn bản ghi lại:
- **Context** (Bối cảnh): vấn đề thực tế đang cần giải
- **Decision** (Quyết định): em chọn cái nào
- **Consequences** (Hệ quả): trade-off đã chấp nhận

ADR giúp người mới (dev / SA / audit) đọc lại 1-2 năm sau hiểu **tại sao** một cấu hình trông kỳ quặc lại tồn tại. Code thường không tự kể câu chuyện này.

## Phạm vi

Tạo ADR khi quyết định:
- Ảnh hưởng kiến trúc cross-service (e.g., chọn Kafka vs RabbitMQ).
- Trade-off rõ ràng (e.g., soft-posting vs append-only — ADR-F).
- Cô lập ranh giới service hoặc data ownership.
- Chọn version major của library/framework (e.g., Spring Boot 3.x).

KHÔNG cần ADR cho:
- Refactor nội bộ 1 module.
- Sửa bug.
- Cập nhật version minor library.
- Quyết định coding-style chi tiết (đặt trong `coding-style.md`).

## Numbering

```
docs/adr/NNNN-title-kebab-case.md
```

- `NNNN` = 4 chữ số zero-padded, tăng dần (`0001`, `0002`, ...).
- `title` = kebab-case ngắn tả nội dung.
- Ví dụ: `0001-use-spring-modulith-modular-monolith.md`

ADR đã có (placeholder cho B0 — sẽ tạo file riêng ở B4 khi cần ref):

| Số | Tiêu đề | Trạng thái | Nguồn |
|---|---|---|---|
| ADR-A | Sở hữu trạng thái & quy trình chốt kỳ kế toán in-core | Accepted 2026-05-19 | BDR §2.1 |
| ADR-B | Reporting B-A read-model từ event | Accepted 2026-05-19 | BDR §2.2 |
| ADR-C | Bảo đảm audit bằng transaction + outbox | Accepted 2026-05-19 | BDR §2.3 |
| ADR-D | Tenant = ĐVKT 1:1 | Accepted 2026-05-19 + Addendum 2026-05-20 | BDR §2.4 |
| ADR-E | UUIDv7 app-side JUG | Accepted 2026-05-19 | BDR §2.5 |
| ADR-F | Soft-posting, sổ cái = projection (KHÔNG append-only) | Accepted 2026-05-19 | BDR §2.6 |
| ADR-G | Đa tệ DECIMAL(24,8) + COA versioned theo chế độ KT | Accepted 2026-05-19 | BDR §2.7 |

ADR mới từ B4+ sẽ đánh số `0001` trở đi (ADR-A..G là legacy reference từ BDR).

## Lifecycle

```
PROPOSED → ACCEPTED → (SUPERSEDED bởi ADR khác | DEPRECATED)
```

- **PROPOSED**: đang thảo luận. Status còn mở.
- **ACCEPTED**: đã chốt. Owner ký (anh Thành cho PMKT MVP1).
- **SUPERSEDED**: bị thay bởi ADR mới — link tới ADR thay thế.
- **DEPRECATED**: không dùng nữa nhưng không có replacement.

ADR **KHÔNG modify nội dung** sau khi ACCEPTED — chỉ thay status. Để correct nội dung: tạo ADR mới SUPERSEDE.

## Template

Copy file dưới đây thành `docs/adr/NNNN-title.md`:

```markdown
# ADR-NNNN: <Tiêu đề ngắn>

- **Status**: PROPOSED | ACCEPTED | SUPERSEDED bởi ADR-XXXX | DEPRECATED
- **Date**: YYYY-MM-DD
- **Author**: <Tên + role>
- **Reviewers**: <Tên + role> (nếu peer review)

## Context

[Mô tả vấn đề. Tại sao cần quyết định ngay bây giờ? Constraint nào?
Liên kết tới spec/bug/ticket nguồn nếu có.]

## Decision

[Em chọn phương án nào, một câu rõ ràng đặt đầu — sau đó giải thích chi tiết.]

## Alternatives considered

| Phương án | Pros | Cons | Lý do bác |
|---|---|---|---|
| A. [tên] | ... | ... | ... |
| B. [tên] | ... | ... | ... |
| C. [chosen] | ... | ... | (chosen — xem Decision) |

## Consequences

### Tích cực
- [hệ quả tốt 1]
- [hệ quả tốt 2]

### Tiêu cực (trade-off)
- [hệ quả xấu chấp nhận được]
- [debt / công nợ kỹ thuật]

### Trung tính
- [thay đổi process / convention]

## Implementation notes

[Chi tiết technical nếu cần dev tham khảo lúc code. Migration plan?]

## References

- BDR §X.Y — [link]
- Plan T-x.y — [link]
- Codex review session — [link if applicable]
- External docs (RFC, vendor docs) — [link]
```

## Peer review

ADR ảnh hưởng cross-service nên có **≥ 1 reviewer** ngoài author trước khi ACCEPTED.

Cho PMKT MVP1 hiện tại:
- Owner SA (anh Thành) = final approver.
- Codex (codex-think-about skill) = peer review for major architectural decisions (đã dùng cho ADR-A..G).

## Lưu ý

- ADR **KHÔNG phải user story / ticket**. ADR ghi *quyết định kiến trúc*, không phải feature backlog.
- ADR **KHÔNG phải spec API**. API contract = OpenAPI YAML (T1.7 / B4+).
- ADR ngắn — 1-2 trang. Nếu dài hơn → consider tách thành nhiều ADR.
- ADR tiếng Việt OK — cùng convention với BDR/SDD.

## Liên quan

- [architecture-rules.md](architecture-rules.md) — Rule §7 #1
- [enforcement-map.md](enforcement-map.md) — Rule §7 #11
- BDR §2 — ADR-A..G hiện có
- Plan §4 Track 3 T3.4
