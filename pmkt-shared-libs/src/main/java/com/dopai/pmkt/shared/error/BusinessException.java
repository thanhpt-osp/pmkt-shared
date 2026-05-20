package com.dopai.pmkt.shared.error;

/**
 * Base exception cho nghiệp vụ — service code throw để báo lỗi 4xx.
 *
 * <p>Convention: subclass đặt tại {@code com.dopai.pmkt.{service}.{module}.domain.exception}. Code
 * application/api KHÔNG catch generic {@code Exception} — chỉ {@code BusinessException} hoặc
 * subclass cụ thể (xem coding-style.md §6).
 *
 * <p>{@link SharedProblemDetailAdvice} sẽ chuyển thành RFC 9457 ProblemDetail tự động khi service
 * import advice (qua {@code @Import(SharedProblemDetailAdvice.class)}).
 */
public class BusinessException extends RuntimeException {

  private final ErrorCode errorCode;

  public BusinessException(ErrorCode errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }

  public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
    super(message, cause);
    this.errorCode = errorCode;
  }

  public ErrorCode errorCode() {
    return errorCode;
  }
}
