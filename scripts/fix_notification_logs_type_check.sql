-- Allow clinic visit assignment + invite response notification types.
-- Hibernate enum updates do not refresh this Postgres CHECK constraint.
ALTER TABLE notification_logs DROP CONSTRAINT IF EXISTS notification_logs_type_check;
ALTER TABLE notification_logs ADD CONSTRAINT notification_logs_type_check CHECK (
  (type)::text = ANY ((ARRAY[
    'MISSED_NUTRITION_LOG',
    'VACCINATION_DUE',
    'DEWORMING_DUE',
    'FOOD_REMINDER',
    'APPOINTMENT_REMINDER',
    'ORDER_STATUS_CHANGE',
    'NEW_MESSAGE',
    'WEEKLY_DIGEST',
    'CLINIC_DOCTOR_INVITE_RESPONSE',
    'CLINIC_VISIT_ASSIGNED'
  ])::text[])
);
