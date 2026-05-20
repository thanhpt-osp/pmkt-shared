# PMKT Architecture Rules

> **Rule §7 #1 (BDR)** — Kiến trúc Clean Architecture + Spring Modulith module boundary, enforce qua ArchUnit code-gate.
>
> Source of truth: [BDR §7](https://github.com/thanhpt-osp/pmkt-docs) (cụ thể rule list trong base-decision-record).

## WHAT — Quy tắc

### 1. Clean Architecture 4 layer

Mỗi module / service được tổ chức theo 4 lớp đồng tâm:

```
┌─────────────────────────────────────────────┐
│  api/        ← REST controller, GraphQL     │  (driver adapters)
│              ← DTO request/response          │
├─────────────────────────────────────────────┤
│  application/ ← Use case, application       │  (orchestration)
│              service, transaction boundary   │
├─────────────────────────────────────────────┤
│  domain/     ← Entity, aggregate, value     │  (business core)
│              object, domain service          │  KHÔNG Spring annotation
├─────────────────────────────────────────────┤
│  infrastructure/ ← Repository impl, JPA,    │  (driven adapters)
│              Kafka producer/consumer,        │
│              external API client             │
└─────────────────────────────────────────────┘
```

**Quy tắc phụ thuộc** (Dependency Rule): chiều mũi tên chỉ đi vào trong:

- `api` → `application` → `domain`
- `infrastructure` → `application` → `domain`
- `domain` KHÔNG biết về `application`, `api`, `infrastructure`
- `application` KHÔNG biết về `api`, `infrastructure` (chỉ qua port interface)

### 2. Spring Modulith Module Boundary

Mỗi nghiệp vụ = 1 Spring Modulith module = 1 Java root package = 1 Maven submodule (trong `pmkt-core-service`).

Module công khai API qua:

- `@NamedInterface("public-api")` ở sub-package → các module khác chỉ được import qua named interface
- Mọi class ngoài named interface = internal, **không được import từ module khác**

Ví dụ:

```java
@org.springframework.modulith.NamedInterface("public-api")
package com.dopai.pmkt.core.danhmuc.api.shared;
```

Module có thể declare allowed dependencies:

```java
@ApplicationModule(
    displayName = "Chứng từ",
    allowedDependencies = {"kernel", "danhmuc", "audit"})
package com.dopai.pmkt.core.chungtu;
```

**Cyclic dependency** giữa module = CẤM. `ApplicationModules.verify()` fail-fast.

### 3. `pmkt-shared` scope guard

`pmkt-shared` (shared kernel cross-service) KHÔNG được import bất kỳ class nào từ:

- `com.dopai.pmkt.core..`
- `com.dopai.pmkt.platform..`
- `com.dopai.pmkt.reporting..`
- `com.dopai.pmkt.document..`
- `com.dopai.pmkt.auditnotification..`
- `com.dopai.pmkt.integration..`

→ **Broadly** (mọi sub-package, không chỉ `.domain.*`).

Allowlist `pmkt-shared` content: BOM + primitives (`PmktIds`, `EventEnvelope`, RFC 9457 `ProblemDetail`) + error contract base + test base + event schema artifacts. **CẤM** business logic / domain class / service-specific code.

LOC warning (non-blocking): `> 2000 LOC` end Batch 3 → cảnh báo refactor split.

### 4. Cross-service communication

- **Sync** giữa service: REST qua Kong (API gateway) — DỪNG ở boundary; module bên trong service dùng method call thẳng (Modulith).
- **Async** giữa service: Kafka event với `EventEnvelope` chuẩn (BDR §4.4 guardrail 3).
- **KHÔNG shared DB** giữa service (BDR §5 + ADR B-9 DB-per-service).
- **KHÔNG sync RPC trong transaction** xuyên service.

## WHY — Lý do

| Quy tắc | Lý do |
|---|---|
| Clean Architecture | Domain core không phụ thuộc framework — đổi Spring → Quarkus / JPA → jOOQ chỉ touch `infrastructure/`. Test domain pure không cần Spring context. |
| Modulith boundary | 1 build artifact (core service) nhưng module isolation logic — refactor sang microservice mai sau dễ hơn. `@NamedInterface` chống "trượt vào internal" giữa team. |
| `pmkt-shared` scope guard broadly | Codex CONSENSUS 2026-05-20 cảnh báo god-module risk: nếu cho phép `pmkt-shared` import service code, dần dần shared phình to chứa business logic → khó upgrade independent. Broadly guard chặn từ gốc. |
| Cross-service async qua envelope | Backward compat additive — consumer pin minor không pin major; deprecation window ≥ 1 release (BDR §4.4 guardrail 3). |
| Không shared DB | Service ownership = DB ownership (ADR B-9). Chia tách runtime trong tương lai không kéo theo migration database lớn. |

## HOW — Enforcement (CI-blocking)

| Quy tắc | Gate | File / Class |
|---|---|---|
| Clean Architecture layer dep | ArchUnit | `pmkt-shared-libs/src/test/.../archunit/CleanArchTest.java` (sẽ viết B4) |
| Module boundary cyclic | Spring Modulith `verify()` | `pmkt-core-app/src/test/.../ModulithStructureTest.java` ✅ T1.4 |
| `@NamedInterface` access | Spring Modulith `verify()` | (cùng test trên) |
| `pmkt-shared` scope guard | ArchUnit CI-blocking | `pmkt-shared-libs/src/test/.../archunit/PmktSharedScopeGuardTest.java` ✅ T1.2 |
| No-cycle giữa Maven module | Maven multi-module reactor | `pmkt-core-service/pom.xml` (declared `<modules>`) ✅ T1.4 |
| Cross-service event envelope | Manual review + code review | (B4+ khi có producer/consumer thực) |

CI workflow `reusable-build.yml` chạy `mvn -B clean verify` → kích hoạt mọi ArchUnit test + Modulith verify trong phase `test`. PR fail nếu vi phạm.

## Liên quan

- [accounting-invariants.md](accounting-invariants.md) — Rule §7 #2
- [coding-style.md](coding-style.md) — Rule §7 #3
- [adr-template.md](adr-template.md) — Rule §7 #4
- [enforcement-map.md](enforcement-map.md) — Rule §7 #11 (mapping rules → gates)
- BDR §4.1 — Service canonical list
- BDR §4.2 — Module bên trong core
- BDR §4.3 — Chuẩn code base 1 service
- BDR §4.4 Revised 2026-05-20 — Poly-repo + 4 guardrails Codex CONSENSUS
- Plan §2 Track 1 T1.2 (scope guard), T1.4 (Modulith verify)
