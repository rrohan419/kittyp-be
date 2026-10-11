-- Store the latest admin rejection reason so clinic owners can review it and resubmit.
ALTER TABLE clinics ADD COLUMN IF NOT EXISTS rejection_reason TEXT;
