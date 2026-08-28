package com.kittyp.common.enums;

import com.kittyp.user.enums.ERole;

/**
 * Public self-service signup roles. Intentionally narrower than {@link ERole}
 * so ROLE_ADMIN, ROLE_MODERATOR, and ROLE_CLINIC_STAFF cannot be requested.
 */
public enum SignupRole {
	USER,
	DOCTOR,
	CLINIC;

	public ERole toERole() {
		return switch (this) {
			case USER -> ERole.ROLE_USER;
			case DOCTOR -> ERole.ROLE_DOCTOR;
			case CLINIC -> ERole.ROLE_CLINIC_ADMIN;
		};
	}
}
