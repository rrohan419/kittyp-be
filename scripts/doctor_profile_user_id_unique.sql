-- One doctor profile per user. Does not delete, update, or merge rows.
-- If any user_id is duplicated, this script stops and leaves the table unchanged.

DO $$
BEGIN
	IF EXISTS (
		SELECT 1
		FROM doctor_profiles
		GROUP BY user_id
		HAVING COUNT(*) > 1
	) THEN
		RAISE EXCEPTION 'duplicate doctor_profiles.user_id rows exist; resolve before adding unique index';
	END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uq_doctor_profiles_user_id ON doctor_profiles (user_id);
