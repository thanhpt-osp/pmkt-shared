# pmkt-shared

PMKT MVP1 shared kernel — parent POM + BOM + reusable utilities cho 6 service.

## Vai trò

- **Parent POM** (`<parent>com.dopai.pmkt:pmkt-shared:0.1.0-SNAPSHOT</parent>` cho 6 service repo).
- **Version alignment gate** — lock Spring Boot / Modulith / springdoc / Testcontainers / JUG / ArchUnit cho toàn bộ MVP1.
- **Reusable libs** (T1.2 sắp tới): `PmktIds.newUuidV7()`, RFC 9457 `ProblemDetail`, error contract base, event schema DTO base versioned, ArchUnit/Spotless/Checkstyle config artifact.
- **Reusable CI workflow** (T1.5) cho 8 repo.

## Scope guard (mandatory — T1.2 sẽ enforce qua ArchUnit CI-blocking)

`pmkt-shared` **không được import** bất kỳ class nào trong `com.dopai.pmkt.{core,platform,reporting,document,auditnotification,integration}..` **broadly** (không chỉ `.domain.*`).

**Cấm**: business logic, domain class, service-specific code.

**Allowlist** (BDR §4.4): BOM + primitives + RFC 9457 `ProblemDetail` + error contract + test base + event schema artifacts.

**LOC warning** (non-blocking): `> 2000 LOC` end Batch 3 → cảnh báo refactor split.

## Version alignment matrix (T1.1 chốt 2026-05-20)

| Component | Version | Note |
|---|---|---|
| Java | 21 LTS | Temurin / openjdk@21 |
| Spring Boot | 3.5.14 | latest 3.5.x patch |
| Spring Modulith | 1.4.11 | compat Boot 3.5 |
| springdoc-openapi | 2.8.17 | webmvc-ui starter |
| Testcontainers | 1.21.4 | integration test |
| JUG (java-uuid-generator) | 5.2.0 | UUIDv7 (ADR-E) |
| ArchUnit | 1.4.2 | scope guard + architecture rules |

Đổi version ở đây = patch BDR §3 + plan §2 T1.1 + `architecture-infographic.html` đồng thời.

## Tài liệu liên quan

- [BDR §4.4 Revised 2026-05-20](https://github.com/thanhpt-osp/pmkt-docs) — Poly-repo + 4 guardrails
- ADR-E — UUIDv7 JUG app-side (`Generators.timeBasedEpochGenerator()`)
- ADR-F — Soft-posting (sổ cái = projection)
- Plan §2 Track 1 — Codebase
- Plan §6 — 5 cổng review

## Build

```bash
mvn -B clean install
```

Local install đủ chạy đến khi T4.0 publish lên GitHub Packages.
