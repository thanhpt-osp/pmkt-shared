# PMKT Tech-Debt Ledger

> **Khoản nợ kỹ thuật** ghi nhận trong quá trình dựng base MVP1. Mỗi item phải có:
> ID • mô tả • nguồn gốc • mức rủi ro • batch giải quyết • trạng thái.
>
> Quy tắc: KHÔNG xoá item khi resolve — đổi trạng thái thành ✅ Resolved + commit hash. Audit trail bắt buộc.

## Trạng thái tổng (cập nhật 2026-05-20 — sau Cổng 4)

| ID | Mô tả | Nguồn | Rủi ro | Resolve-at | Trạng thái |
|---|---|---|---|---|---|
| **TD-01** | ApplicationTest pmkt-core-service đã chuyển Testcontainers; 5 service khác vẫn disable `DataSourceAutoConfiguration` ở profile `test`. Bất đối xứng pattern. **B4 trigger check 2026-05-20**: Phase 4.9 thêm `SecurityConfig` với `JwtDecoder` lazy (chỉ load JWK khi request đầu tiên, không OIDC discovery tại bean creation) → DataSource autoconfig KHÔNG bị activate ở 5 service chưa có JPA bean → ApplicationTest exclude vẫn pass. **Trigger không kích hoạt B4 — defer B5 khi service đầu tiên thêm `@Entity`+`JpaRepository` (dự kiến B5 sprint sau Entity/ERD spec) là CORRECT**, không phải kicked-can-down-road. | Phase 2.5a Batch 2 | Medium — dev local không Docker sẽ fail ApplicationTest của pmkt-core; CI ok. | Trigger hoãn: **B5** khi service thêm JPA bean (B4 Phase 4.9 chỉ thêm SecurityConfig, lazy JwtDecoder, không trigger DataSource autoconfig — asymmetry chưa fail). | 🔴 Open |
| **TD-02** | `maven-failsafe-plugin` chỉ có ở `pmkt-audit-notification-service`. Cần move binding lên `pmkt-shared` parent pom. | Phase 2.5b Batch 2 | Low — fix 1 lần, downstream tự inherit. | Batch 3 Phase 3.1. | ✅ **Resolved 2026-05-20 commit `1c9d69a`** — failsafe ở parent pluginManagement; service replicate qua copy-snippet (poly-repo limitation). |
| **TD-03** | Port assignment thực tế: `audit-notification = 8084`, `integration = 8085` (swap so với footnote plan §6). Documentation chưa cập nhật. | Phase 2.4 Batch 2 | Low — chỉ doc, code đã đúng. | **Cổng cuối** (B5) khi viết deployment doc. | ✅ **Resolved 2026-05-20 (B5 Phase 5.0)** — `pmkt-platform-infra/docs/port-matrix.md` published: 6 service + 8 cluster component + host-port-forward + DB credential mapping. |
| **TD-04** | JaCoCo coverage threshold chưa active — Rule §7 #5 chỉ có placeholder `T3.5`. | Cổng 2 review | High — không có gate coverage = quality drift. | Batch 3 Phase 3.1. | ✅ **Resolved 2026-05-20 commit `1c9d69a`** — JaCoCo 70/60/80 active, gate xanh pmkt-shared-libs (76% line / 88% class). Service copy-snippet B4. |
| **TD-05** | springdoc-openapi mới có dependency, chưa enable `@OpenAPIDefinition` + `@Tag` + schema gen. Rule §7 #8 chỉ ProblemDetail partial. | Cổng 1 + 2 review | Medium — API contract chưa machine-validated. | **B5** khi service có Controller business. | 🔴 Open |
| **TD-06** | `CleanArchTest` ArchUnit chưa viết — Rule §7 #1 entry chỉ có scope-guard + Modulith verify. Layer dependency rule (api→app→domain) chưa enforce. | Cổng 1 review | Medium — code có thể vi phạm Clean Arch không bị catch. | **B5** khi template module mở rộng (≥2 module). | 🔴 Open |
| **TD-07** | Keycloak + Spring Security + JWT decode test chưa setup. Rule §7 #10 chỉ doc. | Cổng 1 list | High (security) — nhưng MVP1 deploy nội bộ → defer chấp nhận được. | **B4 Phase 4.9**. | ✅ **Resolved 2026-05-20 commit pmkt-shared `4870bd1` + 6 service consume** — pmkt-shared kernel `JwtTenantClaimConverter` + `PmktJwtAuthenticationConverter` + `PmktSecurityChainSupport.baseline` (denyAll + permit actuator + JWT decode + Inv-4 tenant validation). 6 service mỗi service `SecurityConfig` delegate baseline. Test 7/7 pass (JwtTenantClaimConverterTest 3 + PmktJwtAuthenticationConverterTest 4). |
| **TD-08** | Schema registry + EventEnvelope compatibility test chưa có. Rule §7 #13 chỉ doc + envelope class. | Cổng 1 list | Medium — chỉ ảnh hưởng khi 2 service publish/consume event production. | **B4 Phase 4.6**. | ✅ **Resolved 2026-05-20 commit pmkt-platform-infra `8b8c8dd`** — confluentinc/cp-schema-registry 7.9.0 deploy qua kafka chart, compatibilityLevel=BACKWARD env, EventEnvelope contract test (B1 T1.2) + topic naming convention `pmkt.<service>.<aggregate>.<event>.v<major>`. Schema upload automation defer B5 (bootstrap script). |
| **TD-09** | Secret manager integration test chưa có. Rule §7 #7 sẽ ra ở T3.8 — chỉ doc, impl defer. | Rule §7 #7 | Medium — Spring `application.yml` chứa placeholder, prod cần secret manager. | **B5 (T4.11)**. | ✅ **Resolved 2026-05-21 (B5 Phase 5.1 commit `17dc3bc`)** — Sealed-Secrets controller v0.36.6 deploy ArgoCD sync-wave -15 + 6 PG app user externalize qua envFrom Secret + psql `-v` flag + 7 SealedSecret placeholder template re-seal workflow. K8s Secret plaintext còn ở `clusters/k3d/dev-secrets/` chỉ là DEV k3d ephemeral fallback (PRIVATE repo + cluster down xoá sealing key). Phase 2 migration = re-seal toàn bộ + xoá plaintext. Chart `pmkt_dev_app_2026` grep count = 0. |
| **TD-10** | Branch protection rules ở 8 repo chưa set (GH free tier limit nếu private). Hiện 7 repo PUBLIC → protection cần GH Pro/org account. | Batch 1 list | Medium — merge bypass review có thể xảy ra. | When org account hoặc khi switch private. | 🔴 Open |
| **TD-11** | `pmkt-shared` repo PUBLIC tactical (workaround GH Packages SNAPSHOT bug với private repo). **B4 scope expansion 2026-05-20**: B4 Phase 4.0 tạo `pmkt-platform-infra` PRIVATE trên `thanhpt-osp` (8th repo bù B1 miss). Toàn bộ 8 repo MVP1 cùng visibility=PRIVATE personal account. Khi migrate sang org account, áp dụng cùng pattern cho cả 8 repo (không có ngoại lệ infra repo). | Batch 1 list | Low — không có business secret trong shared kernel. | When org account → switch private + GH Packages org. | 🔴 Open |
| **TD-12** | B-6 SME kế toán ratify `(LoaiCT, SoCT)` predicate Inv-5 (theo VAS/TT200/TT99). | Batch 1 list | **Blocker go-live** — sai predicate = ghi sổ sai. | Cần SME bên ngoài — **trước B4 go-live**. | 🔴 Open |
| **TD-13** | pmkt-core-service `ModulithStructureTest` hardcode expected module count = 13 sau khi thêm `config` package. `@Modulithic` không có API exclude package; bump cứng. Khi service thêm package non-business mới sẽ tiếp tục bump. | B4 Phase 4.9b | Low — chỉ ảnh hưởng test maintenance. | **B5** khi refactor template module + xem xét `@ApplicationModule` annotation explicit. | ✅ **Resolved 2026-05-20 (B5 Phase 5.0)** — refactor sang `EXPECTED_MODULES` Set<String> + diff-based missing/unexpected. Thêm module = update Set 1 chỗ + tự nhiên ra commit diff, không phải bump số cứng. Test pass 3/3. |

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
