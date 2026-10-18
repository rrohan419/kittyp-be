-- Clinic multi-context indexes (apply manually on staging/prod; ddl-auto does not create these)
CREATE INDEX IF NOT EXISTS idx_bookings_clinic_id ON bookings (clinic_id);
CREATE INDEX IF NOT EXISTS idx_health_events_clinic_id ON health_events (clinic_id);
CREATE INDEX IF NOT EXISTS idx_clinic_doctors_clinic_doctor ON clinic_doctors (clinic_id, doctor_id);
CREATE INDEX IF NOT EXISTS idx_clinic_staff_clinic_user ON clinic_staff (clinic_id, user_id);
CREATE INDEX IF NOT EXISTS idx_clinic_inventory_clinic_active_name
	ON clinic_inventory_items (clinic_id, is_active, name);
CREATE INDEX IF NOT EXISTS idx_clinic_inventory_movements_clinic_created
	ON clinic_inventory_movements (clinic_id, created_at);
CREATE INDEX IF NOT EXISTS idx_clinic_inventory_reservations_invoice
	ON clinic_inventory_reservations (invoice_uuid, status, expires_at);
CREATE INDEX IF NOT EXISTS idx_clinic_inventory_reservations_expiry
	ON clinic_inventory_reservations (status, expires_at);
ALTER TABLE clinic_inventory_movements DROP CONSTRAINT IF EXISTS clinic_inventory_movements_action_check;
ALTER TABLE clinic_inventory_movements ADD CONSTRAINT clinic_inventory_movements_action_check
	CHECK (action IN ('OPENING', 'ADJUST', 'CONSUME', 'RESERVE', 'RELEASE'));
