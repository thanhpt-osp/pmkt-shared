package com.dopai.pmkt.shared.error;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Advice chia sẻ — map {@link BusinessException} → 4xx ProblemDetail (RFC 9457) và generic {@code
 * Exception} → 5xx sanitized ProblemDetail.
 *
 * <p>Service chọn import qua {@code @Import(SharedProblemDetailAdvice.class)} ở class
 * {@code @Configuration} hoặc {@code Application}. Service có thể override 1 entry bằng cách
 * declare {@code @ExceptionHandler} riêng với {@code @Order(Ordered.HIGHEST_PRECEDENCE)}.
 *
 * <p>Policy: tham chiếu {@link ProblemDetailFactory} — KHÔNG leak stack trace / SQL / secret.
 */
@ControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class SharedProblemDetailAdvice {

  private static final Logger LOG = LoggerFactory.getLogger(SharedProblemDetailAdvice.class);

  @ExceptionHandler(BusinessException.class)
  public ProblemDetail handleBusiness(BusinessException ex) {
    LOG.warn("Business error {}: {}", ex.errorCode().code(), ex.getMessage());
    return ProblemDetailFactory.businessError(ex.errorCode(), ex.getMessage());
  }

  @ExceptionHandler(Exception.class)
  public ProblemDetail handleGeneric(Exception ex) {
    String correlationId = UUID.randomUUID().toString();
    LOG.error("Unexpected error correlationId={}", correlationId, ex);
    return ProblemDetailFactory.serverError(correlationId);
  }
}
