package com.kittyp.visit.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.kittyp.common.exception.CustomException;
import com.kittyp.doctor.entity.DoctorProfile;
import com.kittyp.visit.entity.Visit;
import com.kittyp.visit.enums.VisitStatus;

class VisitServiceImplAssignedDoctorTest {

	@Test
	void unassignedCannotLeaveWaitlistToWithDoctor() {
		Visit visit = Visit.builder().status(VisitStatus.WAITLIST).build();
		CustomException ex = assertThrows(CustomException.class,
				() -> VisitServiceImpl.requireAssignedDoctor(visit, VisitStatus.IN_PROGRESS));
		assertEquals("Assign a doctor before moving to With doctor", ex.getMessage());
	}

	@Test
	void unassignedCannotMoveToCheckout() {
		Visit visit = Visit.builder().status(VisitStatus.WAITLIST).build();
		CustomException ex = assertThrows(CustomException.class,
				() -> VisitServiceImpl.requireAssignedDoctor(visit, VisitStatus.CHECKING_OUT));
		assertEquals("Assign a doctor before moving to Checkout", ex.getMessage());
	}

	@ParameterizedTest
	@EnumSource(value = VisitStatus.class, names = { "WAITLIST", "CHECKED_IN", "CANCELLED", "NO_SHOW" })
	void unassignedCanStayOnWaitlistOrCancel(VisitStatus target) {
		Visit visit = Visit.builder().status(VisitStatus.WAITLIST).build();
		assertDoesNotThrow(() -> VisitServiceImpl.requireAssignedDoctor(visit, target));
	}

	@Test
	void assignedDoctorCanMoveToCheckout() {
		Visit visit = Visit.builder().status(VisitStatus.WAITLIST).doctor(new DoctorProfile()).build();
		assertDoesNotThrow(() -> VisitServiceImpl.requireAssignedDoctor(visit, VisitStatus.CHECKING_OUT));
	}
}
