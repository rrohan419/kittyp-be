ALTER TABLE consultation_invoices
    ADD COLUMN IF NOT EXISTS whatsapp_sent_at TIMESTAMP;
