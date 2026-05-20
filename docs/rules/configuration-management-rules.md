# PMKT Configuration Management Rules

> **Rule §7 #7 (BDR)** — Spring profile + externalized config + secret manager. T3.8.
>
> **Trạng thái**: Doc + convention publish ở B3. **Integration test secret manager defer B4** (TD-09).

## WHAT — Quy tắc

### 1. Profile chuẩn

Mỗi service có **đúng 5 profile**:

| Profile | Mục đích | Khi nào active |
|---|---|---|
| `dev` | Local development (IDE) | Default ở dev machine, `application-dev.yml` |
| `test` | Unit test (surefire) | `@ActiveProfiles("test")`, disable DataSource autoconfig |
| `it` | Integration test (failsafe + Testcontainers) | `@ActiveProfiles("it")`, Flyway + JPA bật |
| `staging` | Pre-production cluster | K8s `SPRING_PROFILES_ACTIVE=staging` |
| `prod` | Production cluster | K8s `SPRING_PROFILES_ACTIVE=prod` |

KHÔNG tạo profile custom (`dev2`, `local`, `nick`) — nhánh cá nhân thì dùng override file `application-dev.yml` đặt `~/.pmkt/`.

### 2. Vị trí cấu hình

```
src/main/resources/
├── application.yml              ← base — properties chung (port, app name)
├── application-dev.yml          ← dev override
├── application-test.yml         ← test (disable DB/Flyway)
├── application-it.yml           ← IT (Testcontainers, Flyway)
└── application-prod.yml         ← prod (JSON log, actuator security)
```

`application.yml` là **source of truth**. Override file chỉ chứa thuộc tính **khác** base — không duplicate.

### 3. Property naming

- snake_case-with-dashes (Spring convention): `spring.datasource.url`, `pmkt.core.tenant.default`.
- Prefix custom property: **`pmkt.<service>.<group>.<key>`**, ví dụ `pmkt.core.audit.retention-days`.
- Bind qua `@ConfigurationProperties(prefix = "pmkt.core.audit")` + record + `@Validated`.

```java
@ConfigurationProperties(prefix = "pmkt.core.audit")
@Validated
public record AuditProperties(
    @NotNull @Min(30) Integer retentionDays,
    @NotBlank String storageBucket) {}
```

### 4. Externalized config (12-factor)

- KHÔNG hardcode hostname, port, URL, credential.
- Reference env var qua placeholder `${ENV_VAR:default}`:

```yaml
spring:
  datasource:
    url: ${PMKT_DB_URL:jdbc:postgresql://localhost:5432/pmkt_core}
    username: ${PMKT_DB_USER:pmkt_core_app}
    password: ${PMKT_DB_PASSWORD:}   # KHÔNG default cho password
```

- Password / secret KHÔNG có default — nếu thiếu env var thì fail-fast khi startup.

### 5. Secret management

#### 5.1 Quy tắc (BLOCKER)

- ❌ KHÔNG commit secret vào git (gồm `.env`, `application-prod.yml` có password, K8s Secret YAML có plain value).
- ❌ KHÔNG log secret (Rule §7 #6 — logging-monitoring §5).
- ❌ KHÔNG pass secret qua command line argument (process list visible).
- ✅ Inject qua env var hoặc mounted file từ secret manager.

#### 5.2 Secret manager (B4 setup — TD-09)

PMKT MVP1 dùng **HashiCorp Vault** (chốt ADR — defer ratification B4). Pattern:

- Vault Agent sidecar mount secret vào pod tại `/vault/secrets/<name>`.
- Spring đọc qua `spring.config.import: file:/vault/secrets/db-credentials`.
- Rotation: Vault renew lease tự động, app reconnect khi credential expire.

Dev environment: dùng `.env.local` (gitignored) + `dotenv` plugin IDE. KHÔNG dùng Vault local.

CI: GitHub Actions secrets (encrypted) inject qua env var khi run.

### 6. Config validation startup

Sử dụng `@ConfigurationProperties` + `@Validated` (jakarta.validation) — Spring fail-fast khi binding sai. KHÔNG dùng `@Value` cho property nhiều/quan trọng.

Test config binding bằng `@ConfigurationPropertiesScan` + `@SpringBootTest` profile `test`:

```java
@SpringBootTest(classes = AuditProperties.class)
@TestPropertySource(properties = "pmkt.core.audit.retention-days=90")
class AuditPropertiesTest { ... }
```

### 7. Feature flag

MVP1 KHÔNG dùng external feature flag (LaunchDarkly, Unleash). Toggle qua property `pmkt.<service>.feature.<name>.enabled = true/false` ở `application.yml`. Switch tốn restart pod — chấp nhận.

(Defer external flag B5+ khi có người dùng thực.)

### 8. Property precedence (Spring Boot 3.5)

Thứ tự override (cao nhất ăn):

1. Command line: `--spring.config.location=...`
2. `SPRING_APPLICATION_JSON` env var
3. `application-<profile>.yml`
4. `application.yml`
5. Default class-level `@PropertySource`

Hiểu thứ tự = debug nhanh khi prod giá trị "không như expected."

## HOW — Enforce

### 9. CI gate

- Pre-commit hook (T1.9, ✅ active): Spotless format.
- ArchUnit detector KHÔNG có cho config (gate yếu — manual review).
- Manual review: PR thay đổi `application*.yml` phải kèm note giải thích.
- Tương lai (B4): Scanner secret leak qua git-secrets hoặc trufflehog.

### 10. Service repo replication

Khi tạo service mới hoặc thêm property:
1. Đặt prefix `pmkt.<service>.<group>.<key>`.
2. Thêm `@ConfigurationProperties` record + validation.
3. Document property ở README service hoặc dedicated `docs/configuration.md`.
4. Default ở `application.yml`, env var override qua `${...:default}`.

### 11. Defer cho B4

- Vault setup + Vault Agent sidecar (TD-09).
- K8s Secret kustomize / sealed-secrets.
- Integration test `@SpringBootTest` đọc secret từ Vault stub container.
- ArchUnit detector `@Value` usage (force `@ConfigurationProperties`).

## Liên quan

- [logging-monitoring-rules.md](logging-monitoring-rules.md) §5 — secret KHÔNG log
- [test-coverage-rules.md](test-coverage-rules.md) — config test pattern
- [enforcement-map.md](enforcement-map.md) — Rule #7 status
- [../tech-debt-ledger.md](../tech-debt-ledger.md) — TD-09 secret manager
- BDR §7 #7 — configuration management
