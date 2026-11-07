package com.kittyp.clinic.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;

import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.entity.ClinicInventoryItem;
import com.kittyp.clinic.entity.ClinicPetOwner;
import com.kittyp.clinic.enums.ClinicStatus;
import com.kittyp.user.entity.Address;
import com.kittyp.user.entity.Pet;
import com.kittyp.user.entity.Role;
import com.kittyp.user.entity.User;
import com.kittyp.user.entity.UserFcmToken;
import com.kittyp.user.entity.UserRole;

class ClinicInventorySpecificationTest {
    @Test
    void filtersBeforePaginationAndKeepsClinicAndActiveScope() {
        Configuration config = new Configuration()
                .addAnnotatedClass(User.class).addAnnotatedClass(UserRole.class).addAnnotatedClass(Role.class)
                .addAnnotatedClass(Pet.class).addAnnotatedClass(Address.class).addAnnotatedClass(UserFcmToken.class)
                .addAnnotatedClass(Clinic.class).addAnnotatedClass(ClinicPetOwner.class)
                .addAnnotatedClass(ClinicInventoryItem.class);
        config.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
        config.setProperty("hibernate.connection.url", "jdbc:h2:mem:inventory_search;DB_CLOSE_DELAY=-1");
        config.setProperty("hibernate.hbm2ddl.auto", "create-drop");
        try (SessionFactory factory = config.buildSessionFactory(); var session = factory.openSession()) {
            session.beginTransaction();
            Clinic clinic = clinic("clinic-one");
            Clinic other = clinic("clinic-two");
            session.persist(clinic);
            session.persist(other);
            session.persist(item(clinic, "a", "Amoxicillin", "MEDICATION", true));
            session.persist(item(clinic, "b", "Amoxicillin 250", "MEDICATION", true));
            session.persist(item(clinic, "c", "Bandage", "SUPPLY", true));
            session.persist(item(clinic, "d", "Amoxicillin inactive", "MEDICATION", false));
            session.persist(item(other, "e", "Amoxicillin other clinic", "MEDICATION", true));
            session.persist(item(clinic, "f", "Syringe 10%_large", null, true));
            session.flush();
            var repository = new SimpleJpaRepository<ClinicInventoryItem, Long>(ClinicInventoryItem.class, session);
            var pageable = PageRequest.of(0, 1, Sort.by("name", "uuid"));

            var page = repository.findAll(ClinicInventorySpecification.filters(clinic.getId(), " medication ", " AMOX "),
                    pageable);
            assertEquals(2, page.getTotalElements());
            assertEquals(2, page.getTotalPages());
            assertEquals(List.of("a"), page.getContent().stream().map(ClinicInventoryItem::getUuid).toList());
            assertEquals(List.of("b"), repository.findAll(
                    ClinicInventorySpecification.filters(clinic.getId(), "MEDICATION", "amox"),
                    pageable.next()).getContent().stream().map(ClinicInventoryItem::getUuid).toList());
            assertEquals(4, repository.count(ClinicInventorySpecification.filters(clinic.getId(), null, "  ")));
            assertEquals(2, repository.count(ClinicInventorySpecification.filters(clinic.getId(), null, "dica")));
            assertEquals(0, repository.count(ClinicInventorySpecification.filters(clinic.getId(), "SUPPLY", "amox")));
            assertEquals(1, repository.count(ClinicInventorySpecification.filters(clinic.getId(), null, "%_")));
            assertEquals(0, repository.count(ClinicInventorySpecification.filters(clinic.getId(), null, "___")));
            session.getTransaction().rollback();
        }
    }

    private Clinic clinic(String uuid) {
        Clinic clinic = Clinic.builder().uuid(uuid).status(ClinicStatus.VERIFIED).build();
        clinic.setCreatedAt(LocalDateTime.now());
        return clinic;
    }

    private ClinicInventoryItem item(Clinic clinic, String uuid, String name, String category, boolean active) {
        ClinicInventoryItem item = ClinicInventoryItem.builder().clinic(clinic).uuid(uuid).name(name)
                .category(category).quantity(1).reorderLevel(0).unitPrice(BigDecimal.ONE).build();
        item.setCreatedAt(LocalDateTime.now());
        item.setIsActive(active);
        return item;
    }
}
