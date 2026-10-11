-- Store the latest admin rejection reason for doctors to review before resubmitting.
ALTER TABLE doctor_profiles ADD COLUMN IF NOT EXISTS rejection_reason TEXT;
