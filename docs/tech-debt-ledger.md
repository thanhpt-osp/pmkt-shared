# PMKT Tech-Debt Ledger

> **Khoản nợ kỹ thuật** ghi nhận trong quá trình dựng base MVP1. Mỗi item phải có:
> ID • mô tả • nguồn gốc • mức rủi ro • batch giải quyết • trạng thái.
>
> Quy tắc: KHÔNG xoá item khi resolve — đổi trạng thái thành ✅ Resolved + commit hash. Audit trail bắt buộc.

## Trạng thái tổng (cập nhật 2026-05-20 — sau Cổng 2)

| ID | Mô tả | Nguồn | Rủi ro | Resolve-at | Trạng thái |
|---|---|---|---|---|---|
| **TD-01** | ApplicationTest pmkt-core-service đã chuyển Testcontainers (cần Docker); 5 service khác vẫn disable `DataSourceAutoConfiguration` ở profile `test`. Bất đối xứng pattern. | Phase 2.5a Batch 2 | Medium — dev local không Docker sẽ fail ApplicationTest của pmkt-core; CI ok. | **B4** khi 5 service khác thêm JPA bean → align toàn bộ về Testcontainers. | 🔴 Open |
| **TD-02** | `maven-failsafe-plugin` chỉ có ở `pmkt-audit-notification-service`. Cần move binding lên `pmkt-shared` parent pom để mọi service tự kích hoạt IT khi có file `*IT.java`. | Phase 2.5b Batch 2 | Low — fix 1 lần, downstream tự inherit. | **Batch 3 Phase 3.1** (gom với JaCoCo). | 🟡 Scheduled |
| **TD-03** | Port assignment thực tế: `audit-notification = 8084`, `integration = 8085` (swap so với footnote plan §6). Documentation chưa cập nhật. | Phase 2.4 Batch 2 | Low — chỉ doc, code đã đúng. | **Cổng cuối** (B5) khi viết deployment doc. | 🔴 Open |
| **TD-04** | JaCoCo coverage threshold chưa active — Rule §7 #5 chỉ có placeholder `T3.5`. | Cổng 2 review | High — không có gate coverage = quality drift. | **Batch 3 Phase 3.1**. | 🟡 Scheduled |
| **TD-05** | springdoc-openapi mới có dependency, chưa enable `@OpenAPIDefinition` + `@Tag` + schema gen. Rule §7 #8 chỉ ProblemDetail partial. | Cổng 1 + 2 review | Medium — API contract chưa machine-validated. | **B4** khi service có Controller business. | 🔴 Open |
| **TD-06** | `CleanArchTest` ArchUnit chưa viết — Rule §7 #1 entry chỉ có scope-guard + Modulith verify. Layer dependency rule (api→app→domain) chưa enforce. | Cổng 1 review | Medium — code có thể vi phạm Clean Arch không bị catch. | **B4** khi template module mở rộng (≥2 module). | 🔴 Open |
| **TD-07** | Keycloak + Spring Security + JWT decode test chưa setup. Rule §7 #10 chỉ doc. | Cổng 1 list | High (security) — nhưng MVP1 deploy nội bộ → defer chấp nhận được. | **B4 (T4.5)**. | 🔴 Open |
| **TD-08** | Schema registry + EventEnvelope compatibility test chưa có. Rule §7 #13 chỉ doc + envelope class. | Cổng 1 list | Medium — chỉ ảnh hưởng khi 2 service publish/consume event production. | **B4** (cùng Kafka setup). | 🔴 Open |
| **TD-09** | Secret manager integration test chưa có. Rule §7 #7 sẽ ra ở T3.8 — chỉ doc, impl defer. | Rule §7 #7 | Medium — Spring `application.yml` chứa placeholder, prod cần secret manager. | **B4 (T4.11)**. | 🔴 Open |
| **TD-10** | Branch protection rules ở 8 repo chưa set (GH free tier limit nếu private). Hiện 7 repo PUBLIC → protection cần GH Pro/org account. | Batch 1 list | Medium — merge bypass review có thể xảy ra. | When org account hoặc khi switch private. | 🔴 Open |
| **TD-11** | `pmkt-shared` repo PUBLIC tactical (workaround GH Packages SNAPSHOT bug với private repo). | Batch 1 list | Low — không có business secret trong shared kernel. | When org account → switch private + GH Packages org. | 🔴 Open |
| **TD-12** | B-6 SME kế toán ratify `(LoaiCT, SoCT)` predicate Inv-5 (theo VAS/TT200/TT99). | Batch 1 list | **Blocker go-live** — sai predicate = ghi sổ sai. | Cần SME bên ngoài — **trước B4 go-live**. | 🔴 Open |

## Quy ước

### Mức rủi ro

- **High**: Có thể gây sai sót nghiệp vụ, mất dữ liệu, vi phạm bảo mật, hoặc block go-live. Xử lý trong batch gần nhất.
- **Medium**: Ảnh hưởng dev experience hoặc quality gate; chấp nhận tạm thời nếu có ETA rõ.
- **Low**: Chỉ ảnh hưởng documentation, dev convenience, hoặc edge case rất hiếm.

### Vòng đời item

```
🔴 Open ──> 🟡 Scheduled ──> 🟢 In-Progress ──> ✅ Resolved (commit-hash)
                                  │
                                  └─> ❌ Cancelled (lý do)
```

### Khi thêm item

1. ID kế tiếp `TD-XX` (zero-padded 2 chữ số).
2. Update bảng + commit cùng PR sinh ra khoản nợ.
3. Tham chiếu từ commit message (`See TD-XX in tech-debt-ledger.md`).

### Khi resolve

1. Đổi trạng thái `✅ Resolved`.
2. Thêm cột "Resolved-commit" với hash.
3. KHÔNG xoá row — audit trail.

## Cross-reference

- [enforcement-map.md](rules/enforcement-map.md) — TD-04, TD-05, TD-06, TD-07, TD-08, TD-09 đều liên quan rule gate
- BDR §7 — Bộ rule nền tảng
- Plan §6 — Cổng review (B-6 SME ratify task)
