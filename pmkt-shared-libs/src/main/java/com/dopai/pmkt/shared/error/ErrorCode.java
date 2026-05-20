package com.dopai.pmkt.shared.error;

import java.net.URI;
import org.springframework.http.HttpStatus;

/**
 * Marker contract cho error code catalog (BDR §7).
 *
 * <p>Mỗi service implement enum riêng (e.g. {@code CoreErrorCode}, {@code PlatformErrorCode}) —
 * {@code pmkt-shared} cung cấp contract.
 *
 * <p>Convention: code = {@code PMKT-ERR-NNNN} (NNNN 4-digit, namespace theo service).
 */
public interface ErrorCode {

  /** Machine-readable code (e.g. {@code PMKT-ERR-0001}). */
  String code();

  /** HTTP status mapping. */
  HttpStatus status();

  /** Type URI — resolvable to docs page (e.g. https://docs.pmkt.dopai.com/errors/PMKT-ERR-0001). */
  URI type();

  /** Title ngắn (1 dòng), người dùng cuối đọc được. */
  String title();
}
