package com.dopai.pmkt.shared.archunit;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Rule §7 #6 — Logging discipline detector.
 *
 * <p>Main source KHÔNG được dùng {@code System.out}, {@code System.err}, hoặc {@code
 * Throwable.printStackTrace}. Dùng SLF4J logger (xem logging-monitoring-rules.md §2).
 *
 * <p>Test source được phép (test có thể println debug nhanh). Vì vậy {@code DoNotIncludeTests}.
 *
 * <p>Service replicate snippet này ở chính package test riêng để cover code base service.
 */
@AnalyzeClasses(
    packages = "com.dopai.pmkt.shared",
    importOptions = {ImportOption.DoNotIncludeTests.class})
public class NoSystemOutArchTest {

  @ArchTest
  static final ArchRule NO_SYSTEM_OUT =
      noClasses()
          .should()
          .accessField(System.class, "out")
          .orShould()
          .accessField(System.class, "err")
          .because(
              "Rule §7 #6 — Production code phải dùng SLF4J logger thay vì System.out/err. "
                  + "Xem docs/rules/logging-monitoring-rules.md.");

  @ArchTest
  static final ArchRule NO_PRINT_STACK_TRACE =
      noClasses()
          .should()
          .callMethod(Throwable.class, "printStackTrace")
          .because(
              "Rule §7 #6 — printStackTrace() leak stack ra stdout không qua logger framework. "
                  + "Dùng LOG.error(\"...\", ex) thay thế.");
}
