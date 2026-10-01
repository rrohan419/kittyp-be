package com.kittyp.support.service;

import com.kittyp.support.entity.SupportMail;

/**
 * Result of storing one inbound mail. An acknowledgement is due only for an opening row that has not been claimed.
 */
public final class StoredMail {

	private final SupportMail mail;

	private StoredMail(SupportMail mail) {
		this.mail = mail;
	}

	public static StoredMail ignored() {
		return new StoredMail(null);
	}

	public static StoredMail of(SupportMail mail) {
		return new StoredMail(mail);
	}

	public SupportMail mail() {
		return mail;
	}

	public boolean shouldAcknowledge() {
		return mail != null && mail.isOpening() && !mail.isAckSent();
	}
}
