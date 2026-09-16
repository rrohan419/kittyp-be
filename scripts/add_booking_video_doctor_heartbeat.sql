-- Staging/prod: doctor video session heartbeat (Hibernate ddl-auto=update covers local).
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS video_doctor_heartbeat_at timestamp;
