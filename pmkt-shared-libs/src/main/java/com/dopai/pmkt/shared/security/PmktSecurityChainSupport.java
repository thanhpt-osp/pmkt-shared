package com.dopai.pmkt.shared.security;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;

/**
 * Helper áp dụng security baseline chung cho 6 service PMKT (Rule §7 #10).
 *
 * <p>Baseline:
 *
 * <ul>
 *   <li>{@code csrf} disabled — stateless REST API.
 *   <li>{@code sessionManagement} STATELESS — JWT bearer mỗi request.
 *   <li>{@code authorizeHttpRequests}:
 *       <ul>
 *         <li>permit {@code /actuator/health/**}, {@code /actuator/info}, {@code
 *             /actuator/prometheus} — health probe + Prom scrape.
 *         <li>{@code anyRequest().authenticated()} — denyAll mặc định (Rule §7 #10).
 *       </ul>
 *   <li>{@code oauth2ResourceServer.jwt} dùng {@link PmktJwtAuthenticationConverter} — Inv-4 tenant
 *       validation + Keycloak realm_access.roles → ROLE_*.
 * </ul>
 *
 * <p>Service mở rộng pattern qua {@link #baseline(HttpSecurity, JwtDecoder)}:
 *
 * <pre>{@code
 * @Configuration
 * @EnableWebSecurity
 * public class SecurityConfig {
 *   @Bean
 *   SecurityFilterChain pmktChain(HttpSecurity http, JwtDecoder decoder) throws Exception {
 *     return PmktSecurityChainSupport.baseline(http, decoder).build();
 *   }
 * }
 * }</pre>
 *
 * <p>Service cần permit thêm path → gọi {@code http.authorizeHttpRequests(...)} TRƯỚC khi gọi
 * baseline — Spring Security áp dụng theo thứ tự cấu hình chuỗi.
 */
public final class PmktSecurityChainSupport {

  /** Path permit mặc định baseline — health probe + Prom scrape. */
  public static final String[] DEFAULT_PERMIT_PATTERNS = {
    "/actuator/health/**", "/actuator/info", "/actuator/prometheus"
  };

  private PmktSecurityChainSupport() {
    throw new UnsupportedOperationException("Utility class — không instantiate.");
  }

  /**
   * Cấu hình HttpSecurity với baseline. Trả về cùng builder để service tiếp tục chain.
   *
   * @param http HttpSecurity builder của service
   * @param decoder JwtDecoder của service (auto-config từ
   *     spring.security.oauth2.resourceserver.jwt)
   * @return HttpSecurity đã apply baseline — service gọi {@code .build()} hoặc chain tiếp
   * @throws Exception nếu HttpSecurity config throw
   */
  public static HttpSecurity baseline(HttpSecurity http, JwtDecoder decoder) throws Exception {
    return http.csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(DEFAULT_PERMIT_PATTERNS)
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(
            oauth2 ->
                oauth2.jwt(
                    jwt ->
                        jwt.decoder(decoder)
                            .jwtAuthenticationConverter(new PmktJwtAuthenticationConverter())));
  }
}
