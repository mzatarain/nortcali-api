ALTER TABLE expense_categories
    ADD COLUMN is_active TINYINT(1) NOT NULL DEFAULT 1;
