# PMKT Logging & Monitoring Rules

> **Rule §7 #6 (BDR)** — Logging level + format + correlation + no-PII enforce qua Logback template + ArchUnit detector. T3.6.

## WHAT — Quy tắc

### 1. Stack

- **SLF4J 2.x** API (Spring Boot 3.5 managed).
- **Logback 1.5.x** implementation (Spring Boot default).
- **Micrometer Tracing 1.5.x** (OTEL bridge) cho correlation — không dùng Spring Cloud Sleuth (deprecated).
- **JSON output** ở profile `prod` để log shipper (Loki / OpenSearch) ingest được trực tiếp.

### 2. Logger declaration

**Bắt buộc** trong mọi class business:

```java
private static final Logger LOG = LoggerFactory.getLogger(MyClass.class);
```

- Field name UPPER_SNAKE_CASE (`LOG`) — đồng nhất với Rule §7 #3 coding-style ConstantName.
- KHÔNG dùng `@Slf4j` Lombok — toàn bộ pmkt không phụ thuộc Lombok (ADR-A).
- KHÔNG inject `Logger` qua constructor — static final đủ.

### 3. Log level guidance

| Level | Khi nào dùng | Sample |
|---|---|---|
| `TRACE` | Bước nội bộ rất chi tiết, chỉ enable debugging. | `LOG.trace("Calculating debit for line {}", lineId);` |
| `DEBUG` | Diagnostic info hữu ích cho dev — KHÔNG bật prod thường xuyên. | `LOG.debug("Posting chứng từ {}", soCT);` |
| `INFO` | Sự kiện business quan trọng (đăng nhập, ghi sổ, đóng kỳ). | `LOG.info("Posted chungtu id={} tenant={}", id, tenantId);` |
| `WARN` | Tình huống bất thường nhưng app tự khắc phục. | `LOG.warn("Retry attempt {} for kafka publish", attempt);` |
| `ERROR` | Lỗi cần can thiệp. ALWAYS kèm correlationId / context. | `LOG.error("Failed posting cid={} reason={}", correlationId, ex.getMessage(), ex);` |

### 4. Định dạng log

**KHÔNG dùng string concatenation** — luôn placeholder `{}`:

```java
// ✅
LOG.info("Processed {} records in {}ms", count, durationMs);

// ❌ — concatenation evaluate ngay cả khi level disable
LOG.debug("Processed " + count + " records");
```

Exception luôn ở **last argument**, không nằm trong format:

```java
LOG.error("Failed to commit transaction id={}", txId, ex);  // ✅
LOG.error("Failed: " + ex.getMessage());                     // ❌ leak, mất stack
```

### 5. PII protection (BLOCKER)

KHÔNG được log:

- Mật khẩu, password hash, salt, secret, token, JWT, refresh-token.
- Số tài khoản ngân hàng đầy đủ, OTP, credit card, CVV.
- Số CMND/CCCD, hộ chiếu, thông tin định danh khách hàng cá nhân đầy đủ.
- Body request authentication / authorization endpoint (`/auth/**`).

Pattern an toàn:

- Mask: `user***@example.com`, `**** **** **** 1234`.
- ID-only: log `userId` không log `email`/`phone`.
- Domain event: chỉ ghi `eventType` + `aggregateId`, không full payload.

ArchUnit detector kiểm tra:
- KHÔNG có `System.out.println` / `System.err.println` / `printStackTrace()` trong main source.
- Best-effort grep `password|token|secret|cvv|otp` trong log statement — manual review CI artifact.

### 6. MDC + correlation

Spring Boot 3.5 + Micrometer Tracing tự inject `traceId` + `spanId` vào MDC. PMKT thêm:

| MDC key | Source | Bắt buộc |
|---|---|---|
| `tenantId` | Session tenant context (Inv-4) | ✅ |
| `userId` | JWT subject claim | ✅ khi authenticated |
| `requestId` | HTTP header `X-Request-Id` (gateway gen nếu thiếu) | ✅ |
| `loaiCT` | Khi xử lý chứng từ | Optional |

Filter ở `pmkt-shared` (B4 task) tự setMDC + clear sau request — service không cần code thủ công.

### 7. Output format

**Profile `dev` / `test`** — human-readable pattern (Spring Boot default OK):
```
%clr(%d{HH:mm:ss.SSS}) %clr(%-5level) %clr([%X{traceId:-},%X{spanId:-}]){yellow} %clr(%-40.40logger{39}){cyan} : %m%n
```

**Profile `prod`** — JSON layout (single-line):
```json
{"@timestamp":"2026-05-20T08:19:56.123Z","level":"INFO","logger":"com.dopai.pmkt.core.danhmuc.application.DanhMucDoiTacService","thread":"http-nio-8080-exec-3","message":"Created DM doi tac id=01938abc...","traceId":"abc123","spanId":"def456","tenantId":"01938xyz...","service":"pmkt-core"}
```

Dùng `logstash-logback-encoder` (B4 dep) hoặc Logback built-in `JsonEncoder` (1.5.7+). Template ở §10.

### 8. Log rotation

Service KHÔNG tự rotate log file — log ra `stdout` để container runtime (Docker / K8s) collect. Loki / Fluentbit handle shipping. KHÔNG cần `RollingFileAppender` ở prod.

Dev local có thể `tail -f` từ Spring Boot console.

### 9. Metric + tracing

Out of scope rule này — chi tiết ở **observability rule** (B4 setup):
- Micrometer `MeterRegistry` (Prometheus exporter).
- OTEL exporter cho trace → Tempo.
- Health check `/actuator/health` (Spring Boot Actuator).

### 10. Logback template (canonical)

Lưu ở `pmkt-shared-libs/src/main/resources/logback-template.xml`. Service copy vào `src/main/resources/logback-spring.xml` + thay `${SERVICE_NAME}`.

```xml
<configuration>
  <include resource="org/springframework/boot/logging/logback/defaults.xml"/>
  <include resource="org/springframework/boot/logging/logback/console-appender.xml"/>

  <springProfile name="dev,test">
    <root level="INFO">
      <appender-ref ref="CONSOLE"/>
    </root>
    <logger name="com.dopai.pmkt" level="DEBUG"/>
  </springProfile>

  <springProfile name="prod">
    <appender name="JSON_CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
      <encoder class="ch.qos.logback.classic.encoder.JsonEncoder"/>
    </appender>
    <root level="INFO">
      <appender-ref ref="JSON_CONSOLE"/>
    </root>
  </springProfile>
</configuration>
```

## HOW — Enforce

### 11. ArchUnit detector `NoSystemOutArchTest`

Test ở `pmkt-shared-libs/src/test/java/com/dopai/pmkt/shared/archunit/NoSystemOutArchTest.java`:
- Quét toàn classpath `com.dopai.pmkt..` ở pmkt-shared.
- FAIL nếu có call tới `System.out`, `System.err`, hoặc `Throwable.printStackTrace`.
- Service replicate test (snippet docs).

### 12. Manual review checklist

- PR diff có `System.out.println` / `e.printStackTrace()` → reject.
- PR diff log statement chứa từ khoá nhạy cảm (`password`, `token`, `secret`, `cvv`, `otp`) → manual reviewer xác nhận có mask không.
- PR thêm log ở endpoint `/auth/**` → đặc biệt cẩn trọng PII.

## Liên quan

- [coding-style.md](coding-style.md) — UPPER_SNAKE_CASE constant naming
- [test-coverage-rules.md](test-coverage-rules.md) — coverage threshold
- [architecture-rules.md](architecture-rules.md) — ArchUnit foundation
- [enforcement-map.md](enforcement-map.md) — Rule #6 status
- BDR §7 #6 — logging + monitoring
