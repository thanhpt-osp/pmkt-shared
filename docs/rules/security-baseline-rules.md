# PMKT Security Baseline Rules

> **Rule §7 #10 (BDR)** — Authentication (Keycloak JWT) + Authorization (RBAC + tenant scope) + audit + secret. T3.12.
>
> **Trạng thái**: ✅ **Active** sau B4 Phase 4.9 (TD-07 resolved). Convention publish B3 + impl gate B4.
> Kernel: `pmkt-shared/.../security/{JwtTenantClaimConverter, PmktJwtAuthenticationConverter, PmktSecurityChainSupport}`. 6 service consume qua `SecurityConfig` delegate.

## WHAT — Quy tắc

### 1. Authentication — Keycloak JWT

- IDP duy nhất: **Keycloak 26.x** (B4 T4.5 ✅ realm test setup).
- Realm: `pmkt-mvp1`.
- Frontend: **Authorization Code + PKCE flow** (SPA, no client secret).
- Backend ↔ backend: **Client Credentials flow** với confidential client per-service.
- Token format: JWT RS256 (Keycloak default), không HS256.
- Expiry: access token 15 phút, refresh token 8 giờ.

### 2. JWT validation (mọi service)

Spring Security OAuth2 Resource Server config (B4 impl):

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${PMKT_KEYCLOAK_ISSUER:http://localhost:8180/realms/pmkt-mvp1}
```

Validate:
- ✅ `iss` (issuer) matches Keycloak realm URL.
- ✅ `exp` (expiry) chưa qua.
- ✅ `aud` (audience) chứa service name (`pmkt-core`, `pmkt-platform`, …).
- ✅ Signature verify với JWK Set fetch từ `<issuer>/protocol/openid-connect/certs`.

JWT claim bắt buộc:

| Claim | Mô tả | Bắt buộc |
|---|---|---|
| `sub` | User ID Keycloak | ✅ |
| `preferred_username` | Username login | ✅ |
| `realm_access.roles` | RBAC role array | ✅ |
| `tenant_id` | Custom claim — tenant scope (Inv-4) | ✅ business endpoint |
| `email` | Email user | Optional |

### 3. RBAC — role naming

Convention `pmkt-<service>-<action>` hoặc cross-service `pmkt-<role>`:

| Role | Phạm vi | Mô tả |
|---|---|---|
| `pmkt-admin` | Cross-service | Quản trị tenant, không can thiệp data |
| `pmkt-ke-toan-truong` | Cross-service | Phê duyệt ghi sổ, đóng kỳ, xem báo cáo full |
| `pmkt-ke-toan-vien` | Cross-service | Tạo / sửa chứng từ, ghi sổ |
| `pmkt-core-reader` | core service | Read-only danh mục + chứng từ |
| `pmkt-reporting-reader` | reporting service | Xem báo cáo |
| `pmkt-audit-reader` | audit service | Xem audit log (compliance team) |

Spring Security `@PreAuthorize("hasRole('pmkt-ke-toan-truong')")` mapping. KHÔNG dùng `hasAuthority` (Keycloak nest role trong `realm_access.roles` cần custom converter).

### 4. Tenant scope (Inv-4)

JWT chứa `tenant_id` claim. Filter ở `pmkt-shared` (B4) tự:

1. Extract `tenant_id` từ JWT.
2. Set vào `TenantContext` (ThreadLocal hoặc Spring Request scope bean).
3. JPA repository tự inject filter `WHERE tenant_id = :currentTenant` qua Hibernate Filter.
4. Clear context sau request.

KHÔNG endpoint nào không có tenant scope (trừ login + actuator health). Vi phạm = leak data cross-tenant.

### 5. Endpoint security baseline

```java
http
  .authorizeHttpRequests(auth -> auth
      .requestMatchers("/actuator/health", "/actuator/info").permitAll()
      .requestMatchers("/api/**").authenticated()
      .anyRequest().denyAll())
  .oauth2ResourceServer(o -> o.jwt(jwt -> jwt.jwtAuthenticationConverter(...)));
```

- `permitAll` chỉ cho `/actuator/health` + `/actuator/info`. KHÔNG cho `/actuator/env`, `/actuator/configprops`, `/actuator/metrics`.
- Default `denyAll` cho path không match — fail-closed.
- CSRF: disable cho REST API (stateless JWT). Bật cho form login (nếu có).
- Session: stateless (`SessionCreationPolicy.STATELESS`).

### 6. Audit trail (Inv-3)

Mọi action thay đổi data quan trọng phải log vào `pmkt_audit.audit_log`:

| Event | Sample |
|---|---|
| Login success / fail | `LOGIN_SUCCESS userId=... ip=...` |
| Ghi sổ / bỏ ghi sổ chứng từ | `CHUNG_TU_POSTED id=... userId=... tenantId=...` |
| Đóng / mở kỳ | `KY_CLOSED kyId=... userId=...` |
| Đổi cấu hình quan trọng | `CONFIG_CHANGED key=pmkt.core.audit.retention-days oldVal=30 newVal=90` |
| Truy cập PII bulk | `EXPORT_DOI_TAC count=1234 userId=...` |

Audit table append-only, role `pmkt_audit_writer` INSERT-only (Inv-3 ✅ B2 enforce).

### 7. Secret + credential

Xem [configuration-management-rules.md](configuration-management-rules.md) §5. Quy tắc:

- KHÔNG hardcode credential.
- KHÔNG commit secret git.
- Sử dụng Vault (B4 TD-09).
- DB credential rotate qua Vault dynamic secret (B5+).

### 8. Password + sensitive endpoint

- Login endpoint (Keycloak host, không phải pmkt service) — không log body request.
- Reset password flow: time-limited token (15 phút), one-time use.
- 2FA: Keycloak TOTP — bắt buộc cho role `pmkt-admin` + `pmkt-ke-toan-truong` (B5+).

### 9. CORS

```yaml
pmkt:
  security:
    cors:
      allowed-origins:
        - https://app.pmkt.dopai.com
        - https://staging.pmkt.dopai.com
      allowed-methods: [GET, POST, PUT, PATCH, DELETE]
      allowed-headers: [Authorization, Content-Type, Idempotency-Key, X-Request-Id]
      allow-credentials: true
```

KHÔNG `allowed-origins: *` ở prod. Dev có thể loose nhưng phải log warning ở startup.

### 10. Rate limiting

Gateway Kong (B4) handle rate limit:
- Public endpoint: 60 req/min/IP.
- Authenticated endpoint: 600 req/min/user.
- Login endpoint: 5 req/min/IP (chống brute-force).

Service KHÔNG tự rate limit — duplicate concern.

## HOW — Enforce

### 11. CI gate (B4)

- ArchUnit detector: KHÔNG endpoint nào có `@PreAuthorize("permitAll()")` ngoài actuator allowlist.
- Integration test: stub JWT (mock decoder) test mọi endpoint reject `401` khi không có token.
- Test cross-tenant: user tenant A query data tenant B → expect `403` hoặc empty list (tuỳ design).

### 12. Manual review

- PR thêm endpoint → check `@PreAuthorize` annotation đúng role.
- PR thêm RBAC role → cập nhật bảng §3 doc này.
- PR thêm JWT claim → cập nhật §2 + Keycloak realm config.

## Defer cho B4 (TD-07)

- Spring Security `SecurityFilterChain` config ở `pmkt-shared`.
- JWT authentication converter (extract `realm_access.roles`).
- `TenantContext` filter + JPA Hibernate filter `WHERE tenant_id = ?`.
- Integration test với Keycloak Testcontainers (image `quay.io/keycloak/keycloak:26.x`).
- ArchUnit no-permitAll detector.
- Audit logger ngoài @ControllerAdvice (interceptor or AOP aspect).

## Liên quan

- [configuration-management-rules.md](configuration-management-rules.md) §5 — secret
- [logging-monitoring-rules.md](logging-monitoring-rules.md) §5 — PII không log
- [accounting-invariants.md](accounting-invariants.md) — Inv-3 audit immutability, Inv-4 tenant scope
- [enforcement-map.md](enforcement-map.md) — Rule #10 status
- [../tech-debt-ledger.md](../tech-debt-ledger.md) — TD-07 security impl
- T4.5 Keycloak realm test setup ✅ B1
- BDR §7 #10 — security baseline
