# PMKT Rules — Enforcement Map

> **Rule §7 #11 (BDR)** — Mapping từng rule §7 #1-#10 + T3.13 sang gate CI cụ thể. KHÔNG rule nào được "manual review only" mà không có automated gate.

## Bảng map rule → gate

| # | Rule | Doc | Gate | File / Class | Trạng thái |
|---|---|---|---|---|---|
| **#1** | Clean Architecture layer + Modulith boundary + scope guard | [architecture-rules.md](architecture-rules.md) | ArchUnit + Spring Modulith verify | `PmktSharedScopeGuardTest.java` ✅ T1.2 / `ModulithStructureTest.java` ✅ T1.4 / `CleanArchTest.java` ⏳ B4 | Partial (T1.2 ✅, T1.4 ✅, CleanArchTest B4) |
| **#2** | Accounting invariants (Debit=Credit, sổ cái projection, audit immutable, tenant scope, soft-posting, đa tệ, kỳ closed) | [accounting-invariants.md](accounting-invariants.md) | DB constraint + role-grant + JPA filter + state-machine test | Flyway V001 ⏳ T2.x B2 / engine code ⏳ B4 | B2 + B4 |
| **#3** | Coding style (Google Java Format, naming, package layout) | [coding-style.md](coding-style.md) | Spotless + Checkstyle | `pmkt-shared/pom.xml` (Spotless) + `pmkt-checkstyle.xml` ✅ T1.1, T1.2 | Active (CI-blocking on every PR) |
| **#4** | ADR lifecycle + numbering | [adr-template.md](adr-template.md) | Manual review + checklist (no auto-gate; convention only) | `docs/adr/NNNN-title.md` numbering | Active (convention only, no CI) |
| **#5** | Test coverage threshold (TBD owner Batch 2) | (chưa viết) ⏳ T3.5 | JaCoCo `coverage-threshold` enforce | `pmkt-shared/pom.xml` jacoco plugin ⏳ T3.7 | Pending B2 (T3.5, T3.7) |
| **#6** | Logging + monitoring (level + format + no-PII) | (chưa viết) ⏳ T3.6 | Logback config + SLF4J detector + ArchUnit no-System.out | Logback XML + custom ArchUnit ⏳ B2 | Pending B2 |
| **#7** | Configuration management (profile + secret manager) | (chưa viết) ⏳ T3.8 | Spring profile validation + secret manager integration test | `application.yml` profile + integration test ⏳ B4 | Pending B2 (T3.8) + B4 |
| **#8** | API contract (RFC 9457 ProblemDetail, OpenAPI YAML, versioning) | (chưa viết) ⏳ T3.9 | springdoc-openapi schema generation + ProblemDetail policy test | `ProblemDetailFactory.java` ✅ T1.2 / OpenAPI gen ⏳ B4 | Partial (T1.2 ✅, springdoc B4) |
| **#9** | Database migration (Flyway, idempotent, versioned) | (chưa viết) ⏳ T3.10 | Flyway `validate` + naming convention checker | `pmkt-shared/db-migration-rules.md` ⏳ T3.10 + Flyway plugin | Pending B2 (T2.x, T3.10) |
| **#10** | Security baseline (Keycloak JWT, RBAC, audit) | (chưa viết) ⏳ T3.12 | Spring Security config + integration test JWT decode + ArchUnit no-anonymous-endpoint | T4.5 Keycloak setup ⏳ + Security ArchUnit test ⏳ B4 | Pending B1 (T4.5) + B4 |
| **#13** | Domain event versioning (envelope + additive v1 + deprecation) | (chưa viết) ⏳ T3.13 | Schema registry compatibility test + EventEnvelope contract test | `EventEnvelope.java` ✅ T1.2 / schema registry ⏳ B4 | Partial (T1.2 ✅, registry B4) |

## Phủ rule (coverage)

| Rule # | Có doc | Có CI gate | Trạng thái |
|---|---|---|---|
| 1 | ✅ | ✅ (T1.2+T1.4, CleanArchTest B4) | Active partial, hoàn thiện B4 |
| 2 | ✅ | ⏳ (B2 Flyway + B4 engine) | Active partial |
| 3 | ✅ | ✅ T1.1+T1.2 | **Active** |
| 4 | ✅ | (convention only — no CI) | **Active** |
| 5 | ⏳ T3.5 | ⏳ T3.7 JaCoCo | Pending B2 |
| 6 | ⏳ T3.6 | ⏳ B2 | Pending B2 |
| 7 | ⏳ T3.8 | ⏳ B4 | Pending B2 + B4 |
| 8 | ⏳ T3.9 | ✅ partial (T1.2 ProblemDetail), full springdoc B4 | Pending B2 + B4 |
| 9 | ⏳ T3.10 | ⏳ B2 | Pending B2 |
| 10 | ⏳ T3.12 | ⏳ B1 (T4.5) + B4 | Pending B1 + B4 |
| 13 | ⏳ T3.13 | ✅ partial (T1.2 envelope), registry B4 | Pending B3 + B4 |

**Tổng**: 11 rule.
- **5 rule có doc** (1, 2, 3, 4 + 11/this) ✅ — Batch 1 hoàn thiện
- **6 rule chưa có doc** (5, 6, 7, 8, 9, 10, 13) ⏳ — Batch 3 (T3.5-T3.10, T3.12, T3.13)
- **CI gate active**: Rule 3 (Spotless + Checkstyle), Rule 1 partial (T1.2 scope guard + T1.4 Modulith)
- **CI gate pending**: rule còn lại — kế hoạch ở plan §4 Track 3

## Manual review areas (không phải mọi rule có auto-gate)

- **Rule #4 ADR** — convention only; PR phải có ADR khi quyết định ảnh hưởng kiến trúc. Manual check.
- **Rule #6 No-PII log** — auto detector best-effort (`grep` log statements với `password|token|account`), manual catch edge case.
- **Rule #8 API versioning** — schema diff manual review trong PR `openapi.yaml`.

Manual area phải có **checklist trong PR template** (sẽ tạo `.github/pull_request_template.md` ở T3.x B2 hoặc B4).

## Cập nhật enforcement-map

Khi thêm rule mới hoặc thay đổi gate:
1. Cập nhật bảng ở section "Bảng map rule → gate"
2. Cập nhật coverage statistics
3. Verify CI workflow `reusable-build.yml` có step tương ứng
4. PR liên kết tới ADR nếu thay đổi gate quan trọng

## Liên quan

- [architecture-rules.md](architecture-rules.md) — Rule §7 #1
- [accounting-invariants.md](accounting-invariants.md) — Rule §7 #2
- [coding-style.md](coding-style.md) — Rule §7 #3
- [adr-template.md](adr-template.md) — Rule §7 #4
- BDR §7 — Bộ Rule nền tảng (tổng)
- Plan §4 Track 3 — T3.1-T3.13 (toàn bộ rule task list)
- Plan §6 Cổng 3 — Rules publish criteria
