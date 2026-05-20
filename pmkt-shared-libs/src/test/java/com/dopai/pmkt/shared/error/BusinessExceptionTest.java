package com.dopai.pmkt.shared.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class BusinessExceptionTest {

  private static final ErrorCode SAMPLE =
      new ErrorCode() {
        @Override
        public String code() {
          return "PMKT-ERR-0001";
        }

        @Override
        public HttpStatus status() {
          return HttpStatus.BAD_REQUEST;
        }

        @Override
        public URI type() {
          return URI.create("https://docs.pmkt.dopai.com/errors/PMKT-ERR-0001");
        }

        @Override
        public String title() {
          return "Sample error";
        }
      };

  @Test
  void constructor_should_keep_errorCode_and_message() {
    BusinessException ex = new BusinessException(SAMPLE, "Số chứng từ trùng");

    assertThat(ex.errorCode()).isSameAs(SAMPLE);
    assertThat(ex.getMessage()).isEqualTo("Số chứng từ trùng");
    assertThat(ex.getCause()).isNull();
  }

  @Test
  void constructor_should_keep_cause_when_provided() {
    Throwable cause = new IllegalStateException("DB down");
    BusinessException ex = new BusinessException(SAMPLE, "Lỗi nghiệp vụ", cause);

    assertThat(ex.errorCode()).isSameAs(SAMPLE);
    assertThat(ex.getCause()).isSameAs(cause);
  }
}
