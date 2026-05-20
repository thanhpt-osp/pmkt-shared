package com.dopai.pmkt.shared.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class PmktJwtAuthenticationConverterTest {

  private final PmktJwtAuthenticationConverter converter = new PmktJwtAuthenticationConverter();

  @Test
  void convert_extractsRolesAsAuthorities() {
    Jwt jwt =
        jwt(
            Map.of(
                "tenant_id",
                "tnt-1",
                "sub",
                "user-1",
                "realm_access",
                Map.of("roles", List.of("pmkt-ke-toan-vien", "pmkt-admin"))));

    AbstractAuthenticationToken auth = converter.convert(jwt);

    assertThat(auth).isInstanceOf(JwtAuthenticationToken.class);
    assertThat(auth.getName()).isEqualTo("user-1");
    assertThat(auth.getAuthorities())
        .extracting(Object::toString)
        .containsExactlyInAnyOrder("ROLE_pmkt-ke-toan-vien", "ROLE_pmkt-admin");
  }

  @Test
  void convert_returnsEmptyAuthorities_whenNoRealmAccess() {
    Jwt jwt = jwt(Map.of("tenant_id", "tnt-1", "sub", "user-1"));

    AbstractAuthenticationToken auth = converter.convert(jwt);

    assertThat(auth.getAuthorities()).isEmpty();
  }

  @Test
  void convert_returnsEmptyAuthorities_whenRolesNotList() {
    Jwt jwt =
        jwt(
            Map.of(
                "tenant_id",
                "tnt-1",
                "sub",
                "user-1",
                "realm_access",
                Map.of("roles", "not-a-list")));

    AbstractAuthenticationToken auth = converter.convert(jwt);

    assertThat(auth.getAuthorities()).isEmpty();
  }

  @Test
  void convert_throwsInvalidBearer_whenTenantMissing() {
    Jwt jwt = jwt(Map.of("sub", "user-1", "realm_access", Map.of("roles", List.of("pmkt-admin"))));

    assertThatThrownBy(() -> converter.convert(jwt))
        .isInstanceOf(InvalidBearerTokenException.class);
  }

  private static Jwt jwt(Map<String, Object> claims) {
    return Jwt.withTokenValue("token")
        .header("alg", "RS256")
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(300))
        .claims(c -> c.putAll(claims))
        .build();
  }
}
