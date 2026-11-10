ALTER TABLE pet_vaccine_schedule
    ADD COLUMN IF NOT EXISTS seven_day_sent_at timestamp,
    ADD COLUMN IF NOT EXISTS one_day_sent_at timestamp;
