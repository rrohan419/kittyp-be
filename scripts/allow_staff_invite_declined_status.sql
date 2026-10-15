ALTER TABLE clinic_staff_invites DROP CONSTRAINT IF EXISTS clinic_staff_invites_status_check;
ALTER TABLE clinic_staff_invites ADD CONSTRAINT clinic_staff_invites_status_check CHECK (
    status IN ('PENDING', 'ACCEPTED', 'DECLINED', 'REVOKED', 'EXPIRED')
);
