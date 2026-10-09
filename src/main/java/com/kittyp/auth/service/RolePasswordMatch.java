package com.kittyp.auth.service;

import java.util.List;

import com.kittyp.user.enums.ERole;

/**
 * Login precedence for the account password and role password hashes.
 * A unique role hash wins even when the account password matches the same input.
 */
public final class RolePasswordMatch {

	public enum Kind {
		UNIQUE,
		ACCOUNT,
		AMBIGUOUS,
		REJECT
	}

	public record Decision(Kind kind, ERole loginRole) {
	}

	private RolePasswordMatch() {
	}

	public static Decision decide(boolean accountMatch, List<ERole> roleHits) {
		int matches = roleHits == null ? 0 : roleHits.size();
		if (matches == 1) {
			return new Decision(Kind.UNIQUE, roleHits.get(0));
		}
		if (accountMatch && matches > 1) {
			return new Decision(Kind.AMBIGUOUS, null);
		}
		if (accountMatch) {
			return new Decision(Kind.ACCOUNT, null);
		}
		if (matches > 1) {
			return new Decision(Kind.AMBIGUOUS, null);
		}
		return new Decision(Kind.REJECT, null);
	}
}
