package com.dopai.pmkt.shared.security;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Converter JWT → {@link AbstractAuthenticationToken} cho PMKT.
 *
 * <p>Trách nhiệm:
 *
 * <ul>
 *   <li>Validate tenant_id qua {@link JwtTenantClaimConverter} — fail-fast 401 nếu thiếu.
 *   <li>Extract authorities từ Keycloak claim {@code realm_access.roles} → {@code ROLE_<rolename>}.
 *   <li>Build {@link JwtAuthenticationToken} với name = JWT {@code sub}.
 * </ul>
 *
 * <p>Service đọc tenant_id qua {@code jwt.getClaimAsString("tenant_id")} hoặc helper {@code
 * JwtTenantClaimConverter.extract(jwt)} bất cứ khi nào cần (KHÔNG cache).
 */
public final class PmktJwtAuthenticationConverter
    implements Converter<Jwt, AbstractAuthenticationToken> {

  private static final String ROLE_PREFIX = "ROLE_";

  @Override
  public AbstractAuthenticationToken convert(Jwt jwt) {
    Objects.requireNonNull(jwt, "JWT must not be null");

    // Inv-4 gate — ném InvalidBearerTokenException nếu missing.
    JwtTenantClaimConverter.extract(jwt);

    Collection<GrantedAuthority> authorities = extractAuthorities(jwt);
    return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
  }

  @SuppressWarnings("unchecked")
  private static Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
    // Keycloak chuẩn: realm_access.roles = ["pmkt-ke-toan-vien", "pmkt-admin", ...]
    Map<String, Object> realmAccess = jwt.getClaim("realm_access");
    if (realmAccess == null) {
      return List.of();
    }
    Object rolesObj = realmAccess.get("roles");
    if (!(rolesObj instanceof Collection<?> roles)) {
      return List.of();
    }
    return roles.stream()
        .filter(String.class::isInstance)
        .map(String.class::cast)
        .map(role -> (GrantedAuthority) new SimpleGrantedAuthority(ROLE_PREFIX + role))
        .toList();
  }
}
