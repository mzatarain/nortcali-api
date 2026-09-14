-- Fase 8 del plan SaaS (ver ../../../../../../SAAS-TASKS.md) — panel de administración interno.
-- Tabla separada de `employees`: un superadmin no pertenece a ningún tenant de cliente y su
-- auth no debe depender del modelo multi-tenant (ver decisión de arquitectura del plan).

CREATE TABLE superadmins (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    email         VARCHAR(150) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    name          VARCHAR(150) NOT NULL,
    created_at    TIMESTAMP    NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_superadmins_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Auditoría de impersonación: qué superadmin entró como qué empleado y cuándo — la
-- impersonación efectivamente concede acceso total a la cuenta de un tenant sin su
-- contraseña, así que queda registrada igual que cualquier otro acceso privilegiado.
CREATE TABLE impersonation_audit (
    id             BIGINT   NOT NULL AUTO_INCREMENT,
    superadmin_id  BIGINT   NOT NULL,
    employee_id    BIGINT   NOT NULL,
    tenant_id      BIGINT   NOT NULL,
    ip_address     VARCHAR(45) NULL,
    started_at     TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_impersonation_superadmin FOREIGN KEY (superadmin_id) REFERENCES superadmins (id),
    CONSTRAINT fk_impersonation_employee FOREIGN KEY (employee_id) REFERENCES employees (id),
    CONSTRAINT fk_impersonation_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
