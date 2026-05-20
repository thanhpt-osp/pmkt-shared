-- ============================================================================
-- PMKT — Service role-grant template (T2.6 / T3.10)
-- ============================================================================
--
-- Copy + replace `__SERVICE__` thành tên service (e.g. `core`, `platform`).
-- Đặt vào: src/main/resources/db/migration/V002__role_grant.sql của service.
--
-- Role-grant pattern theo CLAUDE.md "Surgical Changes": chỉ INSERT/UPDATE/DELETE
-- trên schema sở hữu. KHÔNG cross-schema. KHÔNG superuser.

-- Idempotent role creation
DO $$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'pmkt___SERVICE___app') THEN
    CREATE ROLE pmkt___SERVICE___app NOLOGIN;
  END IF;
END$$;

-- Grant trên schema
GRANT USAGE ON SCHEMA pmkt___SERVICE__ TO pmkt___SERVICE___app;

GRANT SELECT, INSERT, UPDATE, DELETE
  ON ALL TABLES IN SCHEMA pmkt___SERVICE__
  TO pmkt___SERVICE___app;

GRANT USAGE, SELECT
  ON ALL SEQUENCES IN SCHEMA pmkt___SERVICE__
  TO pmkt___SERVICE___app;

-- Default privilege cho bảng / sequence tạo sau migration này
ALTER DEFAULT PRIVILEGES IN SCHEMA pmkt___SERVICE__
  GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO pmkt___SERVICE___app;

ALTER DEFAULT PRIVILEGES IN SCHEMA pmkt___SERVICE__
  GRANT USAGE, SELECT ON SEQUENCES TO pmkt___SERVICE___app;
