-- support_mail stores the opening mail and later replies on the same support_id.
-- ddl-auto=update creates the table and the unique constraint on zoho_message_id.
-- Hibernate does not emit the partial unique index, so apply this script as well.
-- Do not add another unique index on zoho_message_id.
-- The script does not alter users, email_audits, notification_logs, or webhook_event.

CREATE TABLE IF NOT EXISTS support_mail (
	id BIGSERIAL PRIMARY KEY,
	created_at TIMESTAMP NOT NULL DEFAULT NOW(),
	updated_at TIMESTAMP,
	is_active BOOLEAN NOT NULL DEFAULT TRUE,
	support_id VARCHAR(20) NOT NULL,
	sender_email VARCHAR(254) NOT NULL,
	subject VARCHAR(500) NOT NULL,
	body VARCHAR(8000) NOT NULL,
	zoho_message_id VARCHAR(255) NOT NULL,
	thread_id VARCHAR(255),
	opening BOOLEAN NOT NULL,
	ack_sent BOOLEAN NOT NULL DEFAULT FALSE,
	user_id BIGINT
);

DROP INDEX IF EXISTS uk_support_mail_zoho_message;

CREATE UNIQUE INDEX IF NOT EXISTS uk_support_mail_opening_support_id
	ON support_mail (support_id)
	WHERE opening = true;
