package com.kittyp.doctor.smartphrase.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.kittyp.doctor.smartphrase.entity.DoctorSmartPhrase;
import com.kittyp.doctor.smartphrase.service.DoctorSmartPhraseSpecification;

import jakarta.persistence.EntityManager;

@DataJpaTest(properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.datasource.url=jdbc:h2:mem:doctor-smart-phrases;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
class DoctorSmartPhraseRepositoryTest {

    @Autowired
    private DoctorSmartPhraseRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void doctorScopeExcludesOtherOwnersAndInactivePhrases() {
        save("phrase-a", "doctor-a", true);
        save("phrase-b", "doctor-b", true);
        save("phrase-deleted", "doctor-a", false);
        entityManager.flush();
        entityManager.clear();

        List<DoctorSmartPhrase> result = repository.findAll(
                DoctorSmartPhraseSpecification.forDoctor("doctor-a"));

        assertEquals(List.of("phrase-a"), result.stream().map(DoctorSmartPhrase::getUuid).toList());
        assertTrue(repository.findOne(DoctorSmartPhraseSpecification.forDoctor("doctor-a")
                .and(DoctorSmartPhraseSpecification.withUuid("phrase-b"))).isEmpty());
    }

    private void save(String uuid, String doctorUuid, boolean active) {
        DoctorSmartPhrase phrase = DoctorSmartPhrase.builder()
                .uuid(uuid)
                .doctorUserUuid(doctorUuid)
                .phraseText("Reusable note")
                .build();
        phrase.setCreatedAt(LocalDateTime.now());
        phrase.setIsActive(active);
        repository.save(phrase);
    }
}
