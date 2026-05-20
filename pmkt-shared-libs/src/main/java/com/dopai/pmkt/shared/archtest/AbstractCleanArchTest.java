package com.dopai.pmkt.shared.archtest;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.Architectures;
import org.junit.jupiter.api.Test;

/**
 * Clean Architecture layer dependency rule (Rule §7 #1 — TD-06).
 *
 * <p>Mỗi service test class extends class này và override {@link #basePackage()} + tuỳ chọn {@link
 * #importClasses()} nếu muốn scope khác mặc định (toàn bộ base package).
 *
 * <p>Layer convention package suffix:
 *
 * <ul>
 *   <li>{@code .api..} — controller + DTO (REST surface)
 *   <li>{@code .application..} — service + orchestration
 *   <li>{@code .domain..} — aggregate + repository interface + domain event
 *   <li>{@code .infrastructure..} — JPA entity + repository impl + external adapter
 * </ul>
 *
 * <p>Allowed dependency direction:
 *
 * <pre>
 *   api -&gt; application -&gt; domain &lt;- infrastructure
 * </pre>
 *
 * <p>Service chưa có code business (B4 skeleton) → import 0 class layer = test pass empty. Khi
 * service thêm code Phase 2, rule auto-enforce.
 *
 * <p>Yêu cầu service consumer: thêm {@code com.tngtech.archunit:archunit-junit5} test scope (pmkt
 * -shared-libs khai báo optional + compile để class này biên dịch được — KHÔNG leak ra runtime
 * service).
 */
public abstract class AbstractCleanArchTest {

  /**
   * Base package để import class scan. Mặc định trả về toàn bộ package gốc service, ví dụ {@code
   * "com.dopai.pmkt.platform"}.
   */
  protected abstract String basePackage();

  /**
   * Import classes mặc định: toàn bộ {@link #basePackage()}, loại trừ test class. Service override
   * nếu muốn scope hẹp hơn (ví dụ loại trừ generated code).
   */
  protected JavaClasses importClasses() {
    return new ClassFileImporter()
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
        .importPackages(basePackage());
  }

  @Test
  void layerDependenciesShouldBeRespected() {
    // `.optionalLayer()` cho phép layer rỗng (service skeleton B4 chưa có code) — KHÔNG fail.
    Architectures.LayeredArchitecture rule =
        Architectures.layeredArchitecture()
            .consideringAllDependencies()
            .optionalLayer("API")
            .definedBy("..api..")
            .optionalLayer("Application")
            .definedBy("..application..")
            .optionalLayer("Domain")
            .definedBy("..domain..")
            .optionalLayer("Infrastructure")
            .definedBy("..infrastructure..")
            .whereLayer("API")
            .mayNotBeAccessedByAnyLayer()
            .whereLayer("Application")
            .mayOnlyBeAccessedByLayers("API")
            .whereLayer("Domain")
            .mayOnlyBeAccessedByLayers("API", "Application", "Infrastructure")
            .whereLayer("Infrastructure")
            .mayNotBeAccessedByAnyLayer();
    rule.check(importClasses());
  }

  @Test
  void domainMustNotDependOnApplicationLayer() {
    // `.allowEmptyShould(true)` — không fail khi service skeleton chưa có class trong .domain..
    ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..application..")
            .allowEmptyShould(true);
    rule.check(importClasses());
  }

  @Test
  void domainMustNotDependOnApiLayer() {
    ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..api..")
            .allowEmptyShould(true);
    rule.check(importClasses());
  }

  @Test
  void domainMustNotDependOnInfrastructureLayer() {
    ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..infrastructure..")
            .allowEmptyShould(true);
    rule.check(importClasses());
  }
}
