# PMKT Coding Style

> **Rule §7 #3 (BDR)** — Quy ước viết Java code. Enforce qua Spotless + Checkstyle CI-blocking.

## WHAT — Quy ước

### 1. Format

- **Google Java Format** (style: `GOOGLE`, version `1.22.0` lock trong `pmkt-shared/pom.xml`).
- Indent **2 spaces** (Google default), KHÔNG dùng tab.
- Max line length **120 ký tự** (config trong `checkstyle.xml`).
- End of file: newline.
- KHÔNG trailing whitespace.

### 2. Naming

| Element | Convention | Ví dụ |
|---|---|---|
| Class | `PascalCase` | `ChungTuService`, `PmktIds` |
| Interface | `PascalCase` (KHÔNG prefix `I`) | `ErrorCode`, `EventPublisher` |
| Method | `camelCase` | `findById`, `postChungTu` |
| Variable (local + field) | `camelCase` | `chungTuId`, `tenantId` |
| Constant | `UPPER_SNAKE_CASE` | `MAX_RETRY_COUNT`, `GENERIC_SERVER_ERROR_TYPE` |
| Package | `lower.case` (only) | `com.dopai.pmkt.core.chungtu` |
| Maven module dir | `kebab-case` | `pmkt-core-chungtu` |

ArchUnit static field naming convention exception: `@ArchTest static final ArchRule` dùng `UPPER_SNAKE_CASE` (per checkstyle ConstantName rule).

### 3. Package layout

```
com.dopai.pmkt.{service}.{module}/
├── api/             ← REST controller, DTO, OpenAPI generated
├── application/     ← Use case / application service
├── domain/          ← Entity, value object, domain service
└── infrastructure/  ← Repository impl, Kafka, external API client
```

(Theo Clean Architecture — chi tiết tại [architecture-rules.md](architecture-rules.md).)

### 4. Vietnamese trong code

- **Domain term tiếng Việt giữ nguyên** trong class/method name (không dịch sang English): `ChungTu`, `KyKeToan`, `DanhMucDoiTac`.
- **Class name không dấu** (Java identifier ASCII).
- **Javadoc tiếng Việt CÓ dấu** OK.
- Comment trong code: Vietnamese OR English, ưu tiên Vietnamese cho business logic.
- Test method name: `should_<expected>_when_<context>()` hoặc `<verb>_<object>_when_<context>()` — English action verb.

### 5. Annotation

- Spring annotation đặt **trước** class declaration (`@Service`, `@RestController`).
- JPA `@Entity`, `@Table` declared explicit.
- KHÔNG dùng Lombok ở `domain/` layer (giữ domain pure POJO).
- Lombok OK ở `infrastructure/`, `api/` cho boilerplate (`@Getter`, `@Setter`, `@Builder`).

### 6. Exception

- Domain exception extends `RuntimeException` (unchecked) — đặt tại `domain/exception/`.
- Application exception extends domain.
- API layer dùng `@ControllerAdvice` chuyển exception → `ProblemDetail` (RFC 9457) qua `ProblemDetailFactory.businessError(...)`.
- KHÔNG catch `Exception` rồi swallow — log + rethrow hoặc convert.

### 7. Logging

- SLF4J via `LoggerFactory.getLogger(class)` — declare `static final` ở class top.
- KHÔNG log password / token / số tài khoản / số dư (PII / sensitive).
- Level convention:
  - `ERROR` — exception bất ngờ, cần investigate.
  - `WARN` — vi phạm business rule retry-able.
  - `INFO` — sự kiện business quan trọng (POSTED, UNPOSTED).
  - `DEBUG` — chi tiết flow.
  - `TRACE` — bytecode-level (chỉ ngầm trong dev).

### 8. Test

- JUnit 5 (`@Test` từ `org.junit.jupiter.api`).
- AssertJ (`assertThat(...)`) — preferred over JUnit `assertEquals`.
- Test class: `<ClassUnderTest>Test` (e.g., `PmktIdsTest`).
- Test method: descriptive — `newUuidV7_should_have_version_7()`.
- Test annotation `@Test` ở method, KHÔNG class.
- Setup: `@BeforeEach` cho per-test setup; `@BeforeAll static` cho class-level fixture immutable.
- Integration test sử dụng Testcontainers (PostgreSQL, Kafka).

### 9. Import

- Static import OK cho `assertThat`, `mock`, `when`.
- KHÔNG star import (`import x.y.*;`) — Checkstyle `AvoidStarImport` reject.
- Import order: theo Google Style — static first, sau đó standard, third-party, project.
- Spotless `removeUnusedImports` tự dọn dead imports.

### 10. Comment

- KHÔNG comment tự động generated (TODO khi cần follow-up, mark `@todo`).
- KHÔNG comment "what code does" — code phải tự đọc được. Comment chỉ giải thích "why" hoặc business context.
- Javadoc cho public API class + method (có `@param`, `@return`, `@throws` khi cần).
- Vietnamese OK trong Javadoc.

## WHY — Lý do

| Quy ước | Lý do |
|---|---|
| Google Java Format | Convention phổ biến nhất, ít bikeshedding. Auto-fix qua Spotless. |
| 2 spaces indent | Chuẩn Google, đỡ ngốn chiều ngang khi nested. |
| Max 120 ký tự | Vietnamese verbose hơn English. 80 ký tự quá hẹp; 120 cân bằng. |
| Vietnamese domain term | Tránh dịch sai nghĩa kế toán (e.g., "chứng từ" ≠ "voucher" hoàn toàn). Dev Việt đọc dễ hơn. |
| ASCII class name (không dấu) | Java compile + IDE refactoring + grep tốt hơn. |
| Lombok không ở domain | Giữ domain layer pure, không phụ thuộc bytecode magic. |
| ProblemDetail RFC 9457 | Standard hiện đại, obsoletes RFC 7807. Spring 6+ built-in support. |
| KHÔNG log PII | Compliance + tránh leak data trong log aggregator. |
| Testcontainers thay H2/in-memory | Test với DB thực tế (PostgreSQL) — catch dialect-specific bug. |
| ArchUnit field UPPER_SNAKE | Convention `static final` — ConstantName Checkstyle rule. |

## HOW — Enforcement (CI-blocking)

| Quy ước | Gate | Tool |
|---|---|---|
| Format (indent, line length, trailing whitespace) | `mvn spotless:check` ở phase validate | Spotless plugin (config `pmkt-shared/pom.xml`) |
| Naming convention | Checkstyle ImportOrder, ConstantName, MethodName, etc. | `pmkt-checkstyle.xml` (bundled trong `pmkt-shared-libs` resource) |
| Star import | Checkstyle `AvoidStarImport` | `pmkt-checkstyle.xml` |
| Unused import | Checkstyle `UnusedImports` + Spotless `removeUnusedImports` | Cả 2 plugin |
| Line length 120 | Checkstyle `LineLength max=120` | `pmkt-checkstyle.xml` |
| Tab character | Checkstyle `FileTabCharacter` | `pmkt-checkstyle.xml` |
| Package name regex | Checkstyle `PackageName` | `pmkt-checkstyle.xml` |
| Newline EOF | Checkstyle `NewlineAtEndOfFile` | `pmkt-checkstyle.xml` |

CI workflow `reusable-build.yml` chạy phase `validate` → Spotless + Checkstyle. Vi phạm = build FAIL.

Local pre-commit hook (T1.9 ✅ Batch 2): `mvn spotless:apply` auto-fix trên file Java đã stage + re-stage. Setup: `./scripts/install-hooks.sh` sau khi clone — xem [dev-onboarding.md](../dev-onboarding.md).

## Liên quan

- [architecture-rules.md](architecture-rules.md) — Rule §7 #1 (Clean Arch layer naming)
- [accounting-invariants.md](accounting-invariants.md) — Rule §7 #2
- [enforcement-map.md](enforcement-map.md) — Rule §7 #11 (mapping)
- `pmkt-shared/checkstyle.xml` — Checkstyle baseline
- `pmkt-shared/pom.xml` — Spotless config (Google Java Format 1.22.0)
- Plan §4 Track 3 T3.3
