package com.kittyp.email.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.enums.ClinicStatus;
import com.kittyp.clinic.repository.ClinicRepository;
import com.kittyp.email.entity.ClinicEmailActivity;
import com.kittyp.email.entity.ClinicEmailActivity.Status;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:clinic-email-usage;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class ClinicEmailActivityRepositoryTest {
    @Autowired ClinicRepository clinics;
    @Autowired ClinicEmailActivityRepository activities;

    @Test
    void locksPracticeAndQueriesCurrentMonthActivity() {
        LocalDateTime now = LocalDateTime.now();
        Clinic clinic = Clinic.builder()
                .uuid("clinic-email-test")
                .name("Test clinic")
                .status(ClinicStatus.PENDING)
                .build();
        clinic.setCreatedAt(now);
        clinic = clinics.saveAndFlush(clinic);

        ClinicEmailActivity activity = new ClinicEmailActivity();
        activity.setClinic(clinic);
        activity.setPeriodStart(LocalDate.now().withDayOfMonth(1));
        activity.setRecipientEmail("owner@example.com");
        activity.setEmailType("INVOICE");
        activity.setStatus(Status.ACCEPTED);
        activity.setCreatedAt(now);
        activities.saveAndFlush(activity);

        assertTrue(clinics.lockById(clinic.getId()).isPresent());
        assertEquals(1, activities.countByClinic_IdAndPeriodStartAndStatusIn(
                clinic.getId(), activity.getPeriodStart(), List.of(Status.ACCEPTED, Status.RESERVED)));
        assertEquals(1, activities.findAllByClinic_IdOrderByCreatedAtDesc(
                clinic.getId(), PageRequest.of(0, 20)).getTotalElements());
    }
}
