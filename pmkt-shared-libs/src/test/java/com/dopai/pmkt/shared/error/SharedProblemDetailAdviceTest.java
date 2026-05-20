package com.dopai.pmkt.shared.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

class SharedProblemDetailAdviceTest {

  private static final ErrorCode SAMPLE =
      new ErrorCode() {
        @Override
        public String code() {
          return "PMKT-ERR-0042";
        }

        @Override
        public HttpStatus status() {
          return HttpStatus.CONFLICT;
        }

        @Override
        public URI type() {
          return URI.create("https://docs.pmkt.dopai.com/errors/PMKT-ERR-0042");
        }

        @Override
        public String title() {
          return "Conflict";
        }
      };

  private final SharedProblemDetailAdvice advice = new SharedProblemDetailAdvice();

  @Test
  void handleBusiness_should_emit_4xx_problemDetail_with_code() {
    ProblemDetail pd = advice.handleBusiness(new BusinessException(SAMPLE, "Số CT trùng"));

    assertThat(pd.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(pd.getTitle()).isEqualTo("Conflict");
    assertThat(pd.getDetail()).isEqualTo("Số CT trùng");
    assertThat(pd.getProperties()).containsEntry("code", "PMKT-ERR-0042");
    assertThat(pd.getProperties()).containsKey("timestamp");
  }

  @Test
  void handleGeneric_should_emit_5xx_with_correlationId_and_no_leak() {
    ProblemDetail pd = advice.handleGeneric(new IllegalStateException("INSERT INTO secret_table"));

    assertThat(pd.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
    assertThat(pd.getProperties()).containsEntry("code", "PMKT-ERR-9999");
    assertThat(pd.getProperties()).containsKey("correlationId");
    assertThat(pd.getDetail()).doesNotContain("INSERT");
    assertThat(pd.getDetail()).doesNotContain("secret_table");
  }
}
