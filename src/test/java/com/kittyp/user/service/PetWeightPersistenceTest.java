package com.kittyp.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import com.kittyp.booking.enums.BookingMode;
import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.enums.ClinicStatus;
import com.kittyp.user.entity.Pet;
import com.kittyp.user.entity.PetWeightLog;
import com.kittyp.user.entity.User;
import com.kittyp.user.repository.PetWeightLogRepository;
import com.kittyp.visit.entity.Visit;
import com.kittyp.visit.enums.VisitSource;
import com.kittyp.visit.enums.VisitStatus;
import com.kittyp.visit.enums.VisitUrgency;

import jakarta.persistence.EntityManager;

@DataJpaTest(properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.datasource.url=jdbc:h2:mem:weight-history;MODE=PostgreSQL;NON_KEYWORDS=DAY",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({PetWeightService.class, PetWeightPersistenceTest.AuditingConfig.class})
class PetWeightPersistenceTest {
    @TestConfiguration
    @EnableJpaAuditing
    static class AuditingConfig {
    }

    @Autowired private PetWeightService service;
    @Autowired private PetWeightLogRepository logs;
    @Autowired private EntityManager entityManager;

    @Test
    void visitAndManualMeasurementsPersistWithAttributionAndLatestProfile() {
        LocalDateTime now = LocalDateTime.now();
        Pet pet = Pet.builder().uuid("pet-weight-test").name("Pet").build();
        pet.setCreatedAt(now);
        entityManager.persist(pet);
        User user = User.builder().uuid("weight-doctor").email("weight-doctor@example.test")
                .password("test").firstName("Doctor").build();
        user.setCreatedAt(now);
        entityManager.persist(user);
        Clinic clinic = Clinic.builder().uuid("weight-clinic").name("Clinic").status(ClinicStatus.VERIFIED).build();
        clinic.setCreatedAt(now);
        entityManager.persist(clinic);
        Visit visit = Visit.builder().uuid("weight-visit").pet(pet).clinic(clinic).source(VisitSource.WALK_IN)
                .channel(BookingMode.IN_PERSON).status(VisitStatus.IN_PROGRESS).urgency(VisitUrgency.ROUTINE).build();
        visit.setCreatedAt(now);
        entityManager.persist(visit);
        entityManager.flush();

        service.recordVisit(visit, 4.5, user);
        service.recordVisit(visit, 4.5, user);
        service.recordVisit(visit, 4.8, user);
        service.recordManual(pet, 6.0, now.plusDays(1), "At home", user);
        entityManager.flush();
        entityManager.clear();

        assertEquals(2, logs.findByPet_UuidOrderByRecordedAtDescIdDesc("pet-weight-test").size());
        PetWeightLog saved = logs.findByVisit_Id(visit.getId()).orElseThrow();
        assertEquals(4.8, saved.getWeight());
        assertEquals("weight-doctor", saved.getRecordedBy().getUuid());
        assertNotNull(saved.getCorrectedAt());
        assertEquals("6.0", entityManager.find(Pet.class, pet.getId()).getWeight());
    }
}
