# PMKT Rules — Enforcement Map

> **Rule §7 #11 (BDR)** — Mapping từng rule §7 #1-#10 + T3.13 sang gate CI cụ thể. KHÔNG rule nào được "manual review only" mà không có automated gate.

## Bảng map rule → gate

| # | Rule | Doc | Gate | File / Class | Trạng thái |
|---|---|---|---|---|---|
| **#1** | Clean Architecture layer + Modulith boundary + scope guard | [architecture-rules.md](architecture-rules.md) | ArchUnit + Spring Modulith verify | `PmktSharedScopeGuardTest.java` ✅ T1.2 / `ModulithStructureTest.java` ✅ T1.4 / `CleanArchTest.java` ⏳ B4 | Partial (T1.2 ✅, T1.4 ✅, CleanArchTest B4) |
| **#2** | Accounting invariants (Debit=Credit, sổ cái projection, audit immutable, tenant scope, soft-posting, đa tệ, kỳ closed) | [accounting-invariants.md](accounting-invariants.md) | DB constraint + role-grant + JPA filter + state-machine test | Flyway V001 ⏳ T2.x B2 / engine code ⏳ B4 | B2 + B4 |
| **#3** | Coding style (Google Java Format, naming, package layout) | [coding-style.md](coding-style.md) | Spotless + Checkstyle | `pmkt-shared/pom.xml` (Spotless) + `pmkt-checkstyle.xml` ✅ T1.1, T1.2 | Active (CI-blocking on every PR) |
| **#4** | ADR lifecycle + numbering | [adr-template.md](adr-template.md) | Manual review + checklist (no auto-gate; convention only) | `docs/adr/NNNN-title.md` numbering | Active (convention only, no CI) |
| **#5** | Test coverage threshold (70% line / 60% branch / 80% class) | [test-coverage-rules.md](test-coverage-rules.md) ✅ T3.5 | JaCoCo `check` bind verify phase + Failsafe IT | `pmkt-shared/pom.xml` pluginManagement ✅ T3.7 / activate `pmkt-shared-libs/pom.xml` ✅ | **Active** (B3 Phase 3.1) |
| **#6** | Logging + monitoring (level + format + no-PII + correlation MDC) | [logging-monitoring-rules.md](logging-monitoring-rules.md) ✅ T3.6 | ArchUnit `NoSystemOutArchTest` + Logback template + manual no-PII review | `NoSystemOutArchTest.java` ✅ + `logback-template.xml` ✅ | **Active** (B3 Phase 3.2) |
| **#7** | Configuration management (profile + externalized + secret manager) | [configuration-management-rules.md](configuration-management-rules.md) ✅ T3.8 | `@ConfigurationProperties` + `@Validated` fail-fast; secret IT defer B4 (TD-09) | `application*.yml` + ConfigurationProperties record | Active partial (doc ✅, secret IT ⏳ B4) |
| **#8** | API contract (URI versioning + RFC 9457 ProblemDetail + OpenAPI + deprecation) | [api-contract-rules.md](api-contract-rules.md) ✅ T3.9 | ProblemDetail policy test ✅ + springdoc gen ⏳ B4 (TD-05) | `ProblemDetailFactory.java` ✅ T1.2 / `*Request`-`*Response` record pattern | Partial (doc ✅, springdoc B4) |
| **#9** | Database migration (Flyway, idempotent, versioned) | [db-migration-rules.md](db-migration-rules.md) ✅ T3.10 | Flyway `validate` + naming convention checker | `pmkt-shared/docs/rules/db-migration-rules.md` ✅ T3.10 / Flyway plugin pluginManagement ✅ T2.1 / V001 per-service ⏳ T2.2-T2.7 | Partial (doc ✅, V001 in-progress) |
| **#10** | Security baseline (Keycloak JWT + RBAC + tenant scope + audit) | [security-baseline-rules.md](security-baseline-rules.md) ✅ T3.12 | Spring Security config ✅ B4 Phase 4.9 + JWT decode test ✅ (TD-07 resolved) | T4.5 Keycloak realm ✅ + `PmktSecurityChainSupport` ✅ B4 + 6 service `SecurityConfig` consume ✅ | **Active** (B4 Phase 4.9 — TD-07 resolved; cross-ref **ADR-H (BDR §2.8)** — Kong scope: gateway routing only, JWT decode downstream Spring Security) |
| **#13** | Domain event versioning (envelope + additive + deprecation + outbox + idempotency) | [domain-event-versioning-rules.md](domain-event-versioning-rules.md) ✅ T3.13 | EventEnvelope contract test ✅ + schema registry BACKWARD compatibility ✅ B4 Phase 4.6 (TD-08 resolved) | `EventEnvelope.java` ✅ T1.2 / `EventEnvelopeTest.java` ✅ / `cp-schema-registry` 7.9.0 ✅ B4 | **Active** (B4 Phase 4.6 — TD-08 resolved; outbox publisher service-side defer B5) |

## Phủ rule (coverage)

| Rule # | Có doc | Có CI gate | Trạng thái |
|---|---|---|---|
| 1 | ✅ | ✅ (T1.2+T1.4, CleanArchTest B4) | Active partial, hoàn thiện B4 |
| 2 | ✅ | ⏳ (B2 Flyway + B4 engine) | Active partial |
| 3 | ✅ | ✅ T1.1+T1.2 | **Active** |
| 4 | ✅ | (convention only — no CI) | **Active** |
| 5 | ✅ T3.5 | ✅ T3.7 JaCoCo (70/60/80) | **Active** (B3) |
| 6 | ✅ T3.6 | ✅ NoSystemOutArchTest + Logback template | **Active** (B3) |
| 7 | ✅ T3.8 | ✅ ConfigurationProperties (secret IT B4) | Active partial |
| 8 | ✅ T3.9 | ✅ partial (T1.2 ProblemDetail), full springdoc B4 | Active partial |
| 9 | ✅ T3.10 | ✅ T2.1 plumbing, V001 in-progress | Active partial |
| 10 | ✅ T3.12 | ✅ B4 Phase 4.9 — Spring Security baseline + 6 service consume (TD-07 ✅) | **Active** (B4) |
| 13 | ✅ T3.13 | ✅ B4 Phase 4.6 — Schema Registry BACKWARD + envelope test (TD-08 ✅) | **Active** (B4) |

**Tổng**: 11 rule.
- **11/11 rule có doc** ✅ — sau Cổng 3 (B3).
- **CI gate Active đầy đủ** (B3 + B4): Rule 3 (Spotless+Checkstyle), 5 (JaCoCo 70/60/80), 6 (NoSystemOutArchTest+Logback), **10** (Spring Security baseline ✅ B4), **13** (Schema Registry ✅ B4).
- **Active partial** (gate có phần, full impl defer B5): Rule 1 (CleanArchTest B5 TD-06), 2 (engine B5), 7 (secret IT B5 TD-09), 8 (springdoc full B5 TD-05), 9 (per-service V001 ✅).
- **Convention-only** (manual review): Rule 4 (ADR).
- Toàn bộ defer item theo dõi ở [../tech-debt-ledger.md](../tech-debt-ledger.md).

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
- [test-coverage-rules.md](test-coverage-rules.md) — Rule §7 #5
- [logging-monitoring-rules.md](logging-monitoring-rules.md) — Rule §7 #6
- [configuration-management-rules.md](configuration-management-rules.md) — Rule §7 #7
- [api-contract-rules.md](api-contract-rules.md) — Rule §7 #8
- [db-migration-rules.md](db-migration-rules.md) — Rule §7 #9
- [security-baseline-rules.md](security-baseline-rules.md) — Rule §7 #10
- [domain-event-versioning-rules.md](domain-event-versioning-rules.md) — Rule §7 #13
- [../tech-debt-ledger.md](../tech-debt-ledger.md) — TD registry
- BDR §7 — Bộ Rule nền tảng (tổng)
- Plan §4 Track 3 — T3.1-T3.13 (toàn bộ rule task list)
- Plan §6 Cổng 3 — Rules publish criteria
