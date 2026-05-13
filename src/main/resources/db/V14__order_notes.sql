ALTER TABLE orders
    ADD COLUMN notes TEXT NULL COMMENT 'Notas o solicitudes especiales del cliente'
    AFTER payment_method;
