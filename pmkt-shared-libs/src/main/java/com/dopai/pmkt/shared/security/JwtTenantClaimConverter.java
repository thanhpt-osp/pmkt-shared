package com.dopai.pmkt.shared.security;

import org.springframework.lang.NonNull;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

/**
 * Extract + validate {@code tenant_id} claim từ JWT (Inv-4 tenant scope).
 *
 * <p>Inv-4 (BDR §3, accounting-invariants Inv-4): mọi request phải có tenant scope rõ ràng — không
 * có claim {@code tenant_id} = JWT vô hiệu cho PMKT (KHÔNG fallback "default tenant").
 *
 * <p>Pattern: gọi {@link #extract(Jwt)} ở edge filter (Spring Security {@code
 * JwtAuthenticationConverter}). Nếu missing/blank → ném {@link InvalidBearerTokenException} →
 * Spring Security trả 401 với RFC 9457 ProblemDetail (Rule §7 #8).
 *
 * <p>KHÔNG cache tenant_id ở chỗ khác — claim luôn đọc từ JWT của request hiện tại để đảm bảo
 * forward security (token revocation, scope downgrade).
 */
public final class JwtTenantClaimConverter {

  /** Tên claim chuẩn của PMKT Keycloak realm. */
  public static final String TENANT_CLAIM = "tenant_id";

  private JwtTenantClaimConverter() {
    throw new UnsupportedOperationException("Utility class — không instantiate.");
  }

  /**
   * Trả về tenant_id sau khi validate.
   *
   * <p>Message ASCII-only theo RFC 6750 §3 (BearerTokenError reject Unicode).
   *
   * @throws InvalidBearerTokenException nếu claim missing, null, hoặc blank.
   */
  @NonNull
  public static String extract(@NonNull Jwt jwt) {
    String tenantId = jwt.getClaimAsString(TENANT_CLAIM);
    if (tenantId == null || tenantId.isBlank()) {
      throw new InvalidBearerTokenException(
          "Inv-4 violation: JWT missing or blank claim '"
              + TENANT_CLAIM
              + "'. "
              + "All PMKT requests require explicit tenant scope.");
    }
    return tenantId;
  }
}
