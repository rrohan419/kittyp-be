CREATE TABLE IF NOT EXISTS clinic_email_activity (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    clinic_id BIGINT NOT NULL REFERENCES clinics(id),
    period_start DATE NOT NULL,
    recipient_email VARCHAR(320) NOT NULL,
    email_type VARCHAR(160) NOT NULL,
    status VARCHAR(16) NOT NULL,
    provider_request_id VARCHAR(160),
    failure_code VARCHAR(80)
);

CREATE INDEX IF NOT EXISTS idx_clinic_email_activity_period
    ON clinic_email_activity (clinic_id, period_start, status);

CREATE INDEX IF NOT EXISTS idx_clinic_email_activity_history
    ON clinic_email_activity (clinic_id, created_at);
