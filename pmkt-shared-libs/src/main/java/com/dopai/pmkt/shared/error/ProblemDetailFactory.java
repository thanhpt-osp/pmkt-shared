package com.dopai.pmkt.shared.error;

import java.net.URI;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/**
 * Factory cho RFC 9457 (Problem Details — obsoletes RFC 7807) response.
 *
 * <p>Policy bắt buộc (BDR §7, plan T1.2):
 *
 * <ul>
 *   <li>KHÔNG leak stack trace
 *   <li>KHÔNG leak SQL / DB error message thô
 *   <li>KHÔNG leak business-sensitive data (số dư, mật khẩu, token, PII)
 *   <li>{@code type} URI phải resolvable (trỏ tới docs/errors page)
 *   <li>{@code code} machine-readable (e.g. PMKT-ERR-0001)
 * </ul>
 */
public final class ProblemDetailFactory {

  private static final URI GENERIC_SERVER_ERROR_TYPE =
      URI.create("https://docs.pmkt.dopai.com/errors/PMKT-ERR-9999");

  private ProblemDetailFactory() {
    throw new UnsupportedOperationException("Utility class");
  }

  /** Tạo ProblemDetail cho business error (4xx). */
  public static ProblemDetail businessError(ErrorCode errorCode, String detail) {
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(errorCode.status(), detail);
    pd.setType(errorCode.type());
    pd.setTitle(errorCode.title());
    pd.setProperty("code", errorCode.code());
    pd.setProperty("timestamp", Instant.now());
    return pd;
  }

  /** Tạo ProblemDetail tự do (advanced — caller tự lo URI/title/code). */
  public static ProblemDetail of(
      HttpStatus status, URI type, String title, String code, String detail) {
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
    pd.setType(type);
    pd.setTitle(title);
    pd.setProperty("code", code);
    pd.setProperty("timestamp", Instant.now());
    return pd;
  }

  /**
   * Tạo ProblemDetail cho 5xx — sanitized, chỉ cung cấp correlationId để tra log nội bộ.
   *
   * <p>Không leak technical detail ra client.
   */
  public static ProblemDetail serverError(String correlationId) {
    ProblemDetail pd =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Lỗi hệ thống. Vui lòng liên hệ hỗ trợ với mã tham chiếu.");
    pd.setType(GENERIC_SERVER_ERROR_TYPE);
    pd.setTitle("Internal Server Error");
    pd.setProperty("code", "PMKT-ERR-9999");
    pd.setProperty("correlationId", correlationId);
    pd.setProperty("timestamp", Instant.now());
    return pd;
  }
}
