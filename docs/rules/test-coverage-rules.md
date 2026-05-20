# PMKT Test Coverage Rules

> **Rule §7 #5 (BDR)** — Test layering + coverage threshold enforce via JaCoCo + Failsafe. T3.5 doc + T3.7 plugin.

## WHAT — Quy tắc

### 1. Test layering

| Layer | Tool | Naming | Phase Maven | Bắt buộc |
|---|---|---|---|---|
| **Unit test** | JUnit 5 + AssertJ + Mockito | `*Test.java` | `test` (surefire) | Mọi class business logic |
| **Integration test** | Spring Boot + Testcontainers | `*IT.java` | `integration-test` (failsafe) | Repository + Controller + ApplicationTest |
| **Architecture test** | ArchUnit | `*ArchTest.java` | `test` (surefire) | Mỗi service tối thiểu 1 (CleanArchTest pattern) |

### 2. Coverage threshold (JaCoCo)

| Metric | Threshold | Lý do |
|---|---|---|
| **Line coverage** | ≥ **70%** | Industry baseline; phù hợp greenfield không có legacy debt. |
| **Branch coverage** | ≥ **60%** | Branch yếu hơn line vì if/else nhánh hiếm khó test full. |
| **Class coverage** | ≥ **80%** | Class hoàn toàn 0 test = code chết cần xoá. |

**Gate enforce ở phase `verify`** (sau `integration-test`) — `mvn clean verify` sẽ FAIL nếu coverage dưới threshold.

### 3. Exclusion từ coverage

KHÔNG đếm coverage cho:

- `**/*Application.class` — Spring Boot bootstrap class (chỉ có `main()`).
- `**/config/**` — `@Configuration` class chỉ chứa bean wiring.
- `**/dto/**`, `**/api/*Request.class`, `**/api/*Response.class` — record DTO (auto-gen accessor).
- `**/generated/**` — code sinh tự động (springdoc, JPA metamodel, …).
- `**/package-info.class` — Modulith annotation holder.
- `**/testkit/**` — dev-tooling helper class (e.g. Testcontainers support). Coverage % không có ý nghĩa khi yêu cầu Docker daemon để hit branch.

Service có thể **mở rộng** danh sách exclude qua `<configuration><excludes>` của plugin trong pom service, nhưng KHÔNG được giảm threshold dưới mức trên trừ khi có ADR.

### 4. Test naming convention

- `MethodNameUnderTest_shouldBehavior_whenCondition()` — preferred (BDD style).
- Hoặc `should_behavior_when_condition()` — chấp nhận nếu nhất quán toàn service.
- KHÔNG dùng `test1`, `testMethodA`, `tt`, `kiemTra`. Test fail không đọc được tên = không debug được.

### 5. Test isolation

- Mỗi `@Test` phải **độc lập** — chạy 1 mình hoặc cùng nhau đều pass.
- KHÔNG share mutable state qua static field giữa các test.
- IT với Testcontainers: dùng `@DynamicPropertySource` + reset DB state ở `@BeforeEach` (xem `AuditRoleGrantIT` pattern).
- Connection pool HikariCP **reuse connection** → role/session state có thể persist; dùng `RESET ROLE`, `TRUNCATE`, hoặc `@DirtiesContext` khi cần.

### 6. Test data

- KHÔNG hard-code UUID literals — dùng `PmktIds.newUuidV7()` hoặc fixture builder.
- KHÔNG depend vào `LocalDate.now()` trực tiếp — inject `Clock`.
- Tiếng Việt OK cho test description, KHÔNG cho class/method name (Spotless format).

## HOW — Cấu hình

### 7. JaCoCo plugin canonical (pmkt-shared parent pluginManagement)

```xml
<plugin>
  <groupId>org.jacoco</groupId>
  <artifactId>jacoco-maven-plugin</artifactId>
  <!-- version managed by spring-boot-starter-parent -->
  <executions>
    <execution>
      <id>jacoco-prepare-agent</id>
      <goals><goal>prepare-agent</goal></goals>
    </execution>
    <execution>
      <id>jacoco-report</id>
      <phase>verify</phase>
      <goals><goal>report</goal></goals>
    </execution>
    <execution>
      <id>jacoco-check</id>
      <phase>verify</phase>
      <goals><goal>check</goal></goals>
      <configuration>
        <rules>
          <rule>
            <element>BUNDLE</element>
            <limits>
              <limit><counter>LINE</counter>   <minimum>0.70</minimum></limit>
              <limit><counter>BRANCH</counter> <minimum>0.60</minimum></limit>
              <limit><counter>CLASS</counter>  <minimum>0.80</minimum></limit>
            </limits>
          </rule>
        </rules>
        <excludes>
          <exclude>**/*Application.class</exclude>
          <exclude>**/config/**</exclude>
          <exclude>**/dto/**</exclude>
          <exclude>**/api/*Request.class</exclude>
          <exclude>**/api/*Response.class</exclude>
          <exclude>**/generated/**</exclude>
          <exclude>**/package-info.class</exclude>
        </excludes>
      </configuration>
    </execution>
  </executions>
</plugin>
```

### 8. Failsafe plugin canonical (pmkt-shared parent pluginManagement) — resolves TD-02

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-failsafe-plugin</artifactId>
  <!-- version managed by spring-boot-starter-parent -->
  <executions>
    <execution>
      <id>failsafe-integration-test</id>
      <goals>
        <goal>integration-test</goal>
        <goal>verify</goal>
      </goals>
    </execution>
  </executions>
</plugin>
```

Failsafe convention auto-pick file `*IT.java` / `IT*.java` / `*ITCase.java`. KHÔNG cần `<configuration><includes>` override.

### 9. Service repo activate (poly-repo pattern)

Vì service repo **không inherit** từ `pmkt-shared` (parent là `spring-boot-starter-parent`), em phải copy 2 block trên vào `<build><plugins>` của service pom:

```xml
<build>
  <plugins>
    <plugin>
      <groupId>org.jacoco</groupId>
      <artifactId>jacoco-maven-plugin</artifactId>
      <!-- copy executions từ §7 ở trên -->
    </plugin>
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-failsafe-plugin</artifactId>
      <!-- copy executions từ §8 ở trên -->
    </plugin>
  </plugins>
</build>
```

Khi sync version JaCoCo / Failsafe ở cross-repo: cập nhật `pmkt-shared/docs/rules/test-coverage-rules.md` này là source-of-truth, dán snippet vào service.

### 10. CI gate

GitHub Actions reusable workflow `reusable-build.yml` chạy `mvn -B clean verify` → JaCoCo `check` bind ở phase `verify` → fail-fast nếu coverage dưới threshold. KHÔNG cần thêm step riêng.

Báo cáo coverage HTML sinh ở `target/site/jacoco/index.html` — có thể publish làm GitHub Actions artifact (B4 task khi setup observability).

## Manual review areas

- **Test giá trị thực tế vs coverage số liệu**: 70% line không đảm bảo test có ý nghĩa. PR reviewer phải xem có test edge case không, có assert đúng không. JaCoCo chỉ là gate sàn.
- **Slow IT**: nếu suite IT > 5 phút, tag chậm bằng JUnit `@Tag("slow")` + chạy CI nhánh riêng. (Defer B4.)

## Liên quan

- [coding-style.md](coding-style.md) — naming convention (UPPER_SNAKE_CASE etc.)
- [architecture-rules.md](architecture-rules.md) — CleanArchTest pattern (Rule §7 #1)
- [enforcement-map.md](enforcement-map.md) — Rule #5 status
- [../tech-debt-ledger.md](../tech-debt-ledger.md) — TD-02 failsafe propagation, TD-04 JaCoCo threshold
- BDR §7 #5 — Test coverage threshold (TBD owner Batch 2) — em chốt 70/60/80 theo industry baseline
