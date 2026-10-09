-- Role login hashes. One row per user and role. Does not change users.password.
-- Local only. Do not run against production.

CREATE TABLE IF NOT EXISTS user_role_credentials (
	id BIGSERIAL PRIMARY KEY,
	created_at TIMESTAMP NOT NULL DEFAULT NOW(),
	updated_at TIMESTAMP,
	is_active BOOLEAN NOT NULL DEFAULT TRUE,
	user_id BIGINT NOT NULL REFERENCES users (id),
	role_id BIGINT NOT NULL REFERENCES roles (id),
	password_hash VARCHAR(255) NOT NULL,
	CONSTRAINT uq_user_role_credentials_user_role UNIQUE (user_id, role_id)
);
