-- Apply before deploying visit-linked weight logging. Existing manual logs remain valid.
ALTER TABLE pet_weight_log ADD COLUMN IF NOT EXISTS visit_id BIGINT;
ALTER TABLE pet_weight_log ADD COLUMN IF NOT EXISTS recorded_by_user_id BIGINT;
ALTER TABLE pet_weight_log ADD COLUMN IF NOT EXISTS corrected_by_user_id BIGINT;
ALTER TABLE pet_weight_log ADD COLUMN IF NOT EXISTS corrected_at TIMESTAMP;

CREATE UNIQUE INDEX IF NOT EXISTS uq_pet_weight_log_visit ON pet_weight_log (visit_id);
CREATE INDEX IF NOT EXISTS idx_pet_weight_log_pet_recorded ON pet_weight_log (pet_id, recorded_at DESC, id DESC);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_pet_weight_log_visit'
            AND conrelid = 'pet_weight_log'::regclass) THEN
        ALTER TABLE pet_weight_log ADD CONSTRAINT fk_pet_weight_log_visit
            FOREIGN KEY (visit_id) REFERENCES visits(id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_pet_weight_log_recorded_by'
            AND conrelid = 'pet_weight_log'::regclass) THEN
        ALTER TABLE pet_weight_log ADD CONSTRAINT fk_pet_weight_log_recorded_by
            FOREIGN KEY (recorded_by_user_id) REFERENCES users(id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_pet_weight_log_corrected_by'
            AND conrelid = 'pet_weight_log'::regclass) THEN
        ALTER TABLE pet_weight_log ADD CONSTRAINT fk_pet_weight_log_corrected_by
            FOREIGN KEY (corrected_by_user_id) REFERENCES users(id);
    END IF;
END $$;
