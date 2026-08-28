-- Clinic multi-context indexes (apply manually on staging/prod; ddl-auto does not create these)
CREATE INDEX IF NOT EXISTS idx_bookings_clinic_id ON bookings (clinic_id);
CREATE INDEX IF NOT EXISTS idx_health_events_clinic_id ON health_events (clinic_id);
CREATE INDEX IF NOT EXISTS idx_clinic_doctors_clinic_doctor ON clinic_doctors (clinic_id, doctor_id);
CREATE INDEX IF NOT EXISTS idx_clinic_staff_clinic_user ON clinic_staff (clinic_id, user_id);
