-- Recuperación de contraseña (login): tabla de tokens de un solo uso.
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at DATETIME NOT NULL,
    used_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_password_reset_tokens_hash UNIQUE (token_hash),
    CONSTRAINT fk_password_reset_tokens_employee FOREIGN KEY (employee_id) REFERENCES employees(id)
);

CREATE INDEX idx_password_reset_tokens_employee ON password_reset_tokens(employee_id);
