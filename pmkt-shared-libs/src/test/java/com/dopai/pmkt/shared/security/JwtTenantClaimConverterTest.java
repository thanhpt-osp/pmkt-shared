package com.dopai.pmkt.shared.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

class JwtTenantClaimConverterTest {

  @Test
  void extract_returnsTenantId_whenClaimPresent() {
    Jwt jwt = jwtWithClaims(Map.of("tenant_id", "tnt-abc", "sub", "user-1"));

    String tenantId = JwtTenantClaimConverter.extract(jwt);

    assertThat(tenantId).isEqualTo("tnt-abc");
  }

  @Test
  void extract_throwsInvalidBearer_whenClaimMissing() {
    Jwt jwt = jwtWithClaims(Map.of("sub", "user-1"));

    // InvalidBearerTokenException.getMessage() returns "Invalid token" by Spring Security
    // convention; chi tiết lý do nằm trong getError().getDescription().
    assertThatThrownBy(() -> JwtTenantClaimConverter.extract(jwt))
        .isInstanceOf(InvalidBearerTokenException.class)
        .satisfies(
            e -> {
              String description = ((OAuth2AuthenticationException) e).getError().getDescription();
              assertThat(description).contains("Inv-4 violation").contains("tenant_id");
            });
  }

  @Test
  void extract_throwsInvalidBearer_whenClaimBlank() {
    Jwt jwt = jwtWithClaims(Map.of("tenant_id", "   ", "sub", "user-1"));

    assertThatThrownBy(() -> JwtTenantClaimConverter.extract(jwt))
        .isInstanceOf(InvalidBearerTokenException.class)
        .satisfies(
            e -> {
              String description = ((OAuth2AuthenticationException) e).getError().getDescription();
              assertThat(description).contains("Inv-4 violation");
            });
  }

  private static Jwt jwtWithClaims(Map<String, Object> claims) {
    return Jwt.withTokenValue("token")
        .header("alg", "RS256")
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(300))
        .claims(c -> c.putAll(claims))
        .build();
  }
}
