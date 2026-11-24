ALTER TABLE users
    ADD COLUMN IF NOT EXISTS whatsapp_country_code VARCHAR(255),
    ADD COLUMN IF NOT EXISTS whatsapp_number VARCHAR(255);
