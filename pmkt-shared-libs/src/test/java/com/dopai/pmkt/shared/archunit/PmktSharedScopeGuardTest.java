package com.dopai.pmkt.shared.archunit;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Scope guard cho `pmkt-shared` (BDR §4.4 guardrail 2 — Codex CONSENSUS 2026-05-20).
 *
 * <p><b>CI-blocking</b>: `pmkt-shared` KHÔNG được import từ các package service riêng.
 *
 * <p>Phạm vi: <b>broadly</b> — không chỉ {@code .domain.*} mà cả {@code .application.*}, {@code
 * .infrastructure.*}, {@code .api.*}, {@code ..} (mọi sub-package).
 *
 * <p>Allowlist (BDR §4.4): BOM + primitives + RFC 9457 ProblemDetail + error contract + test base +
 * event schema artifacts. Cấm business logic, domain class, service-specific code.
 */
@AnalyzeClasses(
    packages = "com.dopai.pmkt.shared",
    importOptions = {ImportOption.DoNotIncludeTests.class})
public class PmktSharedScopeGuardTest {

  @ArchTest
  static final ArchRule PMKT_SHARED_MUST_NOT_DEPEND_ON_SERVICE_PACKAGES =
      noClasses()
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "com.dopai.pmkt.core..",
              "com.dopai.pmkt.platform..",
              "com.dopai.pmkt.reporting..",
              "com.dopai.pmkt.document..",
              "com.dopai.pmkt.auditnotification..",
              "com.dopai.pmkt.integration..")
          .because(
              "pmkt-shared là shared kernel có scope guard broadly (BDR §4.4 guardrail 2 — "
                  + "Codex CONSENSUS 2026-05-20). Không được import service-specific code. "
                  + "Allowlist: BOM + primitives + RFC 9457 ProblemDetail + error contract + "
                  + "test base + event schema artifacts.");
}
