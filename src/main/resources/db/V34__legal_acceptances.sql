-- V34 — Fase 13 (go-live): evidencia de aceptación de Términos y Condiciones y Aviso de
-- Privacidad al registrar un negocio nuevo.
--
-- Una fila por aceptación (no columnas en `tenants`): si los documentos cambian de versión,
-- la re-aceptación se agrega como fila nueva y el historial completo queda como prueba
-- (Código de Comercio art. 89 y ss. — consentimiento por medios electrónicos; LFPDPPP art. 7).
--
-- `terms_version` / `privacy_version` = fecha de la versión publicada que vio el usuario
-- (constantes en `LegalDocuments`). `ip_address` / `user_agent` = contexto técnico del
-- momento de la aceptación. Sin ON DELETE CASCADE a propósito: borrar un tenant con
-- aceptaciones exige borrar primero su evidencia de forma explícita.

CREATE TABLE IF NOT EXISTS legal_acceptances (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    employee_id BIGINT NOT NULL,
    terms_version VARCHAR(20) NOT NULL,
    privacy_version VARCHAR(20) NOT NULL,
    accepted_at DATETIME NOT NULL,
    ip_address VARCHAR(45) NULL,
    user_agent VARCHAR(255) NULL,
    CONSTRAINT fk_legal_acceptances_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_legal_acceptances_employee FOREIGN KEY (employee_id) REFERENCES employees(id)
);

CREATE INDEX idx_legal_acceptances_tenant ON legal_acceptances(tenant_id);
