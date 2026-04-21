ALTER TABLE sales
    ADD COLUMN cash_session_id BIGINT NULL,
    ADD CONSTRAINT fk_sale_cash_session
        FOREIGN KEY (cash_session_id) REFERENCES cash_sessions(id);
