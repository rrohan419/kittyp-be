package com.kittyp.user.repository;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;

import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.entity.ClinicPetOwner;
import com.kittyp.clinic.enums.ClinicStatus;
import com.kittyp.clinic.service.ClinicOwnerSpecification;
import com.kittyp.user.entity.Address;
import com.kittyp.user.entity.Pet;
import com.kittyp.user.entity.Role;
import com.kittyp.user.entity.User;
import com.kittyp.user.entity.UserFcmToken;
import com.kittyp.user.entity.UserRole;
import com.kittyp.user.enums.ERole;
import com.kittyp.user.service.PlatformUserSearchSpecification;
import com.kittyp.user.service.PlatformUserSearchSpecification.SearchMode;

import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

/** Execute the search specification against real Hibernate mappings and an in-memory database. */
class AppointmentOwnershipJpaTest {
    @Test
    void persistedOwnershipSupportsBothMappingsAndExcludesUnownedOrIneligiblePets() throws Exception {
        Configuration config = new Configuration()
                .addAnnotatedClass(User.class).addAnnotatedClass(UserRole.class).addAnnotatedClass(Role.class)
                .addAnnotatedClass(Pet.class).addAnnotatedClass(Address.class).addAnnotatedClass(UserFcmToken.class)
                .addAnnotatedClass(Clinic.class).addAnnotatedClass(ClinicPetOwner.class);
        config.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
        config.setProperty("hibernate.connection.url", "jdbc:h2:mem:appointment_ownership;DB_CLOSE_DELAY=-1");
        config.setProperty("hibernate.hbm2ddl.auto", "create-drop");
        config.setProperty("hibernate.show_sql", "false");
        try (SessionFactory factory = config.buildSessionFactory(); Session session = factory.openSession()) {
            session.beginTransaction();
            Role parentRole = role(ERole.ROLE_USER);
            Role doctorRole = role(ERole.ROLE_DOCTOR);
            session.persist(parentRole);
            session.persist(doctorRole);

            User parent = user("parent", parentRole);
            Pet linked = pet("PET123", "UniqueMilo");
            parent.addPet(linked); // User.pets writes pets.user_uuid; Pet mirror is read-only.
            parent.addPet(pet("HIDDEN", "HiddenMilo", true));
            Pet inactivePet = pet("INACTIVE", "InactiveMilo");
            inactivePet.setIsActive(false);
            parent.addPet(inactivePet);
            Pet imported = pet("IMPORT", "ImportedOnly");
            imported.setPatientNumber("doc:123");
            parent.addPet(imported);
            parent.setFirstName("Alice");
            parent.setLastName("Smith");
            parent.setPhoneNumber("1234567890");
            session.persist(parent);
            User doctor = user("doctor", doctorRole);
            doctor.addPet(pet("DOCTORPET", "DoctorOnly"));
            session.persist(doctor);
            User disabled = user("disabled", parentRole);
            disabled.setEnabled(false);
            disabled.addPet(pet("DISABLEDPET", "DisabledOnly"));
            session.persist(disabled);
            User inactive = user("inactive", parentRole);
            inactive.setIsActive(false);
            inactive.addPet(pet("INACTIVEUSERPET", "InactiveUserOnly"));
            session.persist(inactive);
            User legacy = user("legacy", parentRole);
            legacy.addPet(pet("LEGACYPET", "LegacyOnly"));
            session.persist(legacy);
            session.persist(pet("ORPHAN", "ClinicOnly"));
            for (int i = 0; i < 25; i++) {
                User batchUser = user("batch-" + i, parentRole);
                batchUser.setCreatedAt(LocalDateTime.of(2026, 1, 1, 0, 0).plusMinutes(i));
                session.persist(batchUser);
            }
            Clinic clinic = Clinic.builder().uuid("clinic-one").status(ClinicStatus.VERIFIED).build();
            clinic.setCreatedAt(LocalDateTime.now());
            session.persist(clinic);
            Clinic otherClinic = Clinic.builder().uuid("clinic-two").status(ClinicStatus.VERIFIED).build();
            otherClinic.setCreatedAt(LocalDateTime.now());
            session.persist(otherClinic);
            ClinicPetOwner owner = owner(clinic, "owner-one", "Alice", "Smith", "9876543210");
            session.persist(owner);
            session.persist(owner(otherClinic, "owner-two", "Alice", "Smith", "9876543210"));
            ClinicPetOwner inactiveOwner = owner(clinic, "owner-inactive", "Alice", "Smith", "9876543210");
            inactiveOwner.setIsActive(false);
            session.persist(inactiveOwner);
            session.flush();
            Long clinicId = clinic.getId();
            session.createNativeMutationQuery("alter table users alter column is_active drop not null").executeUpdate();
            session.createNativeMutationQuery("update users set is_active = null where uuid = 'legacy'").executeUpdate();
            session.clear();

            assertEquals(List.of("parent"), search(session, "UniqueMilo"));
            assertEquals(List.of("parent"), search(session, "PET123"));
            assertTrue(search(session, "ClinicOnly").isEmpty());
            assertTrue(search(session, "HiddenMilo").isEmpty());
            assertTrue(search(session, "InactiveMilo").isEmpty());
            assertTrue(search(session, "ImportedOnly").isEmpty());
            assertTrue(search(session, "DoctorOnly").isEmpty());
            assertTrue(search(session, "DisabledOnly").isEmpty());
            assertTrue(search(session, "InactiveUserOnly").isEmpty());
            assertEquals(List.of("legacy"), search(session, "LegacyOnly"));
            assertEquals(List.of("parent"), search(session, "ALICE SMITH", SearchMode.APPOINTMENT));
            assertEquals(List.of("parent"), search(session, "alicesmith", SearchMode.APPOINTMENT));
            assertEquals(List.of("parent"), search(session, "45678", SearchMode.GENERAL));
            assertTrue(search(session, "45678", SearchMode.APPOINTMENT).isEmpty());
            assertTrue(search(session, "Alice", SearchMode.EMAIL_OR_ID).isEmpty());
            assertEquals(List.of("parent"), search(session, "PARENT@", SearchMode.EMAIL_OR_ID));
            assertEquals(List.of("owner-one"), searchOwners(session, clinicId, "ALICESMITH",
                    ClinicOwnerSpecification.SearchMode.APPOINTMENT));
            assertEquals(List.of("owner-one"), searchOwners(session, clinicId, "76543",
                    ClinicOwnerSpecification.SearchMode.GENERAL));
            assertTrue(searchOwners(session, clinicId, "76543", ClinicOwnerSpecification.SearchMode.APPOINTMENT).isEmpty());
            assertTrue(searchOwners(session, clinicId, "Alice", ClinicOwnerSpecification.SearchMode.EMAIL_OR_ID).isEmpty());
            assertEquals(List.of("owner-one"), searchOwners(session, clinicId, "OWNER-ONE",
                    ClinicOwnerSpecification.SearchMode.EMAIL_OR_ID));
            var repository = new SimpleJpaRepository<User, Long>(User.class, session);
            var page = repository.findAll(PlatformUserSearchSpecification.search("batch-", SearchMode.APPOINTMENT),
                    PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")));
            assertEquals(25, page.getTotalElements());
            assertEquals(20, page.getContent().size());
            assertEquals("batch-24", page.getContent().get(0).getUuid());

            String petsQuery = PetsRepository.class.getMethod("findPlatformPetsOfUser", String.class)
                    .getAnnotation(Query.class).value();
            assertTrue(session.createQuery(petsQuery, Pet.class).setParameter("userUuid", "parent")
                    .getResultList().stream().anyMatch(p -> "PET123".equals(p.getUuid())));
            assertEquals("parent", session.createQuery("select p.parentUserUuid from Pet p where p.uuid = :id", String.class)
                    .setParameter("id", "PET123").getSingleResult());
            assertEquals("PET123", session.createQuery("""
                    select p.uuid from Pet p, User u
                    where u.uuid = :userUuid and p.uuid = :petUuid and p member of u.pets
                    """, String.class).setParameter("userUuid", "parent").setParameter("petUuid", "PET123")
                    .getSingleResult());
            session.getTransaction().rollback();
        }
    }

    private List<String> search(Session session, String q) {
        return search(session, q, SearchMode.APPOINTMENT);
    }

    private List<String> search(Session session, String q, SearchMode mode) {
        var cb = session.getCriteriaBuilder();
        CriteriaQuery<User> query = cb.createQuery(User.class);
        Root<User> root = query.from(User.class);
        query.select(root).where(PlatformUserSearchSpecification.search(q, mode)
                .toPredicate(root, query, cb));
        return session.createQuery(query).setMaxResults(20)
                .getResultList().stream().map(User::getUuid).toList();
    }

    private List<String> searchOwners(Session session, Long clinicId, String text, ClinicOwnerSpecification.SearchMode mode) {
        var cb = session.getCriteriaBuilder();
        CriteriaQuery<ClinicPetOwner> query = cb.createQuery(ClinicPetOwner.class);
        Root<ClinicPetOwner> root = query.from(ClinicPetOwner.class);
        query.select(root).where(ClinicOwnerSpecification.search(clinicId, text, mode).toPredicate(root, query, cb));
        return session.createQuery(query).getResultList().stream().map(ClinicPetOwner::getUuid).toList();
    }

    private ClinicPetOwner owner(Clinic clinic, String uuid, String first, String last, String phone) {
        ClinicPetOwner owner = ClinicPetOwner.builder().clinic(clinic).uuid(uuid).firstName(first).lastName(last)
                .phone(phone).email(uuid + "@example.test").build();
        owner.setCreatedAt(LocalDateTime.now());
        return owner;
    }

    private Role role(ERole name) {
        Role role = new Role(name);
        role.setCreatedAt(LocalDateTime.now());
        return role;
    }

    private User user(String uuid, Role role) {
        User user = User.builder().uuid(uuid).email(uuid + "@example.test").password("test").build();
        user.setCreatedAt(LocalDateTime.now());
        user.addRole(role);
        user.getUserRoles().forEach(r -> r.setCreatedAt(LocalDateTime.now()));
        return user;
    }

    private Pet pet(String id, String name) {
        return pet(id, name, false);
    }

    private Pet pet(String id, String name, boolean hidden) {
        Pet pet = Pet.builder().uuid(id).name(name).hiddenFromParent(hidden).build();
        pet.setCreatedAt(LocalDateTime.now());
        return pet;
    }
}
