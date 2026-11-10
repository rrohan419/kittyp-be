package com.kittyp.vaccine.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.kittyp.user.entity.Pet;
import com.kittyp.vaccine.entity.PetVaccineSchedule;
import com.kittyp.vaccine.entity.VaccineMaster;

import jakarta.persistence.EntityManager;

@DataJpaTest(properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.datasource.url=jdbc:h2:mem:vaccine-reminders;MODE=PostgreSQL;NON_KEYWORDS=DAY",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PetVaccineScheduleRepositoryTest {
    @Autowired private PetVaccineScheduleRepository repository;
    @Autowired private EntityManager entityManager;

    @Test
    void selectsOnlyActiveIncompleteUnsentSchedulesInWindow() {
        LocalDate today = LocalDate.of(2026, 11, 10);
        Pet pet = Pet.builder().uuid("PET001").name("Milo").build();
        pet.setCreatedAt(LocalDateTime.now());
        entityManager.persist(pet);
        VaccineMaster vaccine = VaccineMaster.builder().name("Rabies").build();
        vaccine.setCreatedAt(LocalDateTime.now());
        entityManager.persist(vaccine);

        PetVaccineSchedule dueToday = save(pet, vaccine, today, false, false, true);
        PetVaccineSchedule dueInSeven = save(pet, vaccine, today.plusDays(7), false, false, true);
        save(pet, vaccine, today.minusDays(1), false, false, true);
        save(pet, vaccine, today.plusDays(8), false, false, true);
        save(pet, vaccine, today.plusDays(1), true, false, true);
        save(pet, vaccine, today.plusDays(1), false, true, true);
        save(pet, vaccine, today.plusDays(1), false, false, false);
        entityManager.flush();
        entityManager.clear();

        List<PetVaccineSchedule> pending = repository.findPendingReminders(today, today.plusDays(7));

        assertEquals(List.of(dueToday.getId(), dueInSeven.getId()),
                pending.stream().map(PetVaccineSchedule::getId).toList());
    }

    private PetVaccineSchedule save(Pet pet, VaccineMaster vaccine, LocalDate dueDate,
            boolean completed, boolean reminderSent, boolean active) {
        PetVaccineSchedule schedule = PetVaccineSchedule.builder().pet(pet).vaccine(vaccine).dueDate(dueDate)
                .completed(completed).reminderSent(reminderSent).build();
        schedule.setIsActive(active);
        schedule.setCreatedAt(LocalDateTime.now());
        return repository.save(schedule);
    }
}
