-- ============================================================================
-- PMKT — Audit append-only role-grant (T2.6)
-- Inv-3 accounting-invariants.md — audit immutability tại DB level
-- ============================================================================
--
-- Đặt vào: pmkt-audit-notification-service/src/main/resources/db/migration/V002__audit_role_grant.sql
--
-- Quy tắc:
--  - pmkt_audit_writer:  INSERT only (KHÔNG UPDATE, KHÔNG DELETE)
--  - pmkt_audit_reader:  SELECT only
-- DB sẽ reject UPDATE/DELETE trên audit table bất kể code có cố gắng gọi.

-- Idempotent role creation
DO $$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'pmkt_audit_writer') THEN
    CREATE ROLE pmkt_audit_writer NOLOGIN;
  END IF;
  IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'pmkt_audit_reader') THEN
    CREATE ROLE pmkt_audit_reader NOLOGIN;
  END IF;
END$$;

-- Schema usage
GRANT USAGE ON SCHEMA pmkt_audit TO pmkt_audit_writer;
GRANT USAGE ON SCHEMA pmkt_audit TO pmkt_audit_reader;

-- WRITER: chỉ INSERT — KHÔNG GRANT UPDATE, DELETE
GRANT INSERT ON ALL TABLES IN SCHEMA pmkt_audit TO pmkt_audit_writer;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA pmkt_audit TO pmkt_audit_writer;

-- READER: chỉ SELECT
GRANT SELECT ON ALL TABLES IN SCHEMA pmkt_audit TO pmkt_audit_reader;

-- Default privilege cho bảng audit tạo sau migration này
ALTER DEFAULT PRIVILEGES IN SCHEMA pmkt_audit
  GRANT INSERT ON TABLES TO pmkt_audit_writer;

ALTER DEFAULT PRIVILEGES IN SCHEMA pmkt_audit
  GRANT USAGE, SELECT ON SEQUENCES TO pmkt_audit_writer;

ALTER DEFAULT PRIVILEGES IN SCHEMA pmkt_audit
  GRANT SELECT ON TABLES TO pmkt_audit_reader;

-- KHÔNG có statement UPDATE/DELETE — không cấp = không có quyền.
-- Test PostgreSQL: `SET ROLE pmkt_audit_writer; UPDATE pmkt_audit.audit_log SET ... ;`
-- → ERROR: permission denied for table audit_log
