package com.kittyp.clinic.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.kittyp.clinic.dao.ClinicDao;
import com.kittyp.clinic.dao.ClinicStaffDao;
import com.kittyp.clinic.dto.ClinicDtos.ClinicalRecordRequest;
import com.kittyp.clinic.dto.ClinicDtos.HealthEventModel;
import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.entity.ClinicPetOwner;
import com.kittyp.clinic.enums.ClinicStatus;
import com.kittyp.clinic.repository.ClinicDoctorRepository;
import com.kittyp.clinic.repository.ClinicPetEnrollmentRepository;
import com.kittyp.health.dao.HealthEventDao;
import com.kittyp.health.entity.HealthEvent;
import com.kittyp.health.enums.HealthEventType;
import com.kittyp.user.dao.UserDao;
import com.kittyp.user.entity.Pet;
import com.kittyp.user.entity.Role;
import com.kittyp.user.entity.User;
import com.kittyp.user.entity.UserRole;
import com.kittyp.user.enums.ERole;
import com.kittyp.user.repository.PetsRepository;
import com.kittyp.visit.dao.VisitDao;
import com.kittyp.visit.entity.Visit;

@ExtendWith(MockitoExtension.class)
class ClinicServiceImplClinicalRecordsTest {

    private static final String CLINIC_UUID = "clinic-1";
    private static final String PET_UUID = "9AP1AU";
    private static final String VISIT_UUID = "visit-3d-ago";
    private static final String EMAIL = "doc@example.com";

    @Mock
    private ClinicDao clinicDao;
    @Mock
    private ClinicStaffDao clinicStaffDao;
    @Mock
    private ClinicDoctorRepository clinicDoctorRepository;
    @Mock
    private UserDao userDao;
    @Mock
    private PetsRepository petsRepository;
    @Mock
    private ClinicPetEnrollmentRepository clinicPetEnrollmentRepository;
    @Mock
    private HealthEventDao healthEventDao;
    @Mock
    private VisitDao visitDao;

    @InjectMocks
    private ClinicServiceImpl clinicService;

    @Test
    void createClinicalRecord_labTiedToPastVisit() {
        User doctor = userWithRoles(2L, EMAIL, ERole.ROLE_DOCTOR);
        Clinic clinic = clinic(userWithRoles(1L, "owner@example.com", ERole.ROLE_CLINIC_ADMIN));
        ClinicPetOwner clinicOwner = clinicOwner(clinic);
        Pet pet = pet(clinic, clinicOwner);
        Visit visit = Visit.builder().uuid(VISIT_UUID).clinic(clinic).pet(pet).build();

        stubClinicAccess(doctor, clinic, true);
        when(petsRepository.findByUuidAndClinic_Id(PET_UUID, clinic.getId())).thenReturn(Optional.of(pet));
        when(visitDao.findByUuidAndClinicId(VISIT_UUID, clinic.getId())).thenReturn(Optional.of(visit));
        when(healthEventDao.save(any(HealthEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        HealthEventModel model = clinicService.createClinicalRecord(CLINIC_UUID, PET_UUID,
                new ClinicalRecordRequest(HealthEventType.LAB_REPORT, "CBC", "Results", LocalDate.of(2026, 8, 27),
                        VISIT_UUID, List.of("https://s3.example/cbc.pdf")),
                EMAIL);

        ArgumentCaptor<HealthEvent> captor = ArgumentCaptor.forClass(HealthEvent.class);
        verify(healthEventDao).save(captor.capture());
        HealthEvent saved = captor.getValue();
        assertEquals(HealthEventType.LAB_REPORT, saved.getType());
        assertEquals(VISIT_UUID, saved.getVisitUuid());
        assertEquals(List.of("https://s3.example/cbc.pdf"), saved.getAttachments());
        assertEquals(VISIT_UUID, model.visitUuid());
        assertEquals("LAB_REPORT", model.type());
    }

    @Test
    void createClinicalRecord_unauthorizedCallerRejected() {
        User owner = userWithRoles(1L, "owner@example.com", ERole.ROLE_CLINIC_ADMIN);
        User stranger = userWithRoles(9L, "other@example.com", ERole.ROLE_DOCTOR);
        Clinic clinic = clinic(owner);
        stubClinicAccess(stranger, clinic, false);

        assertThrows(AccessDeniedException.class, () -> clinicService.createClinicalRecord(CLINIC_UUID, PET_UUID,
                new ClinicalRecordRequest(HealthEventType.LAB_REPORT, "CBC", null, LocalDate.now(), VISIT_UUID,
                        List.of()),
                stranger.getEmail()));
        verify(healthEventDao, never()).save(any());
    }

    @Test
    void assertClinicalUploadAllowed_unauthorizedCallerRejected() {
        User owner = userWithRoles(1L, "owner@example.com", ERole.ROLE_CLINIC_ADMIN);
        User stranger = userWithRoles(9L, "other@example.com", ERole.ROLE_DOCTOR);
        Clinic clinic = clinic(owner);
        stubClinicAccess(stranger, clinic, false);

        assertThrows(AccessDeniedException.class,
                () -> clinicService.assertClinicalUploadAllowed(CLINIC_UUID, PET_UUID, VISIT_UUID, stranger.getEmail()));
    }

    private void stubClinicAccess(User user, Clinic clinic, boolean doctorAtClinic) {
        when(userDao.userByEmail(user.getEmail())).thenReturn(user);
        when(clinicDao.findByUuid(CLINIC_UUID)).thenReturn(clinic);
        when(clinicStaffDao.isActiveMember(clinic.getId(), user.getId())).thenReturn(false);
        when(clinicDoctorRepository.existsByClinic_IdAndDoctor_User_IdAndIsActiveTrue(clinic.getId(), user.getId()))
                .thenReturn(doctorAtClinic);
    }

    private static Clinic clinic(User owner) {
        Clinic clinic = Clinic.builder()
                .uuid(CLINIC_UUID)
                .name("Branch")
                .status(ClinicStatus.VERIFIED)
                .owner(owner)
                .build();
        clinic.setId(100L);
        return clinic;
    }

    private static ClinicPetOwner clinicOwner(Clinic clinic) {
        return ClinicPetOwner.builder()
                .uuid("owner-1")
                .clinic(clinic)
                .firstName("Pat")
                .lastName("Owner")
                .email("pat@example.com")
                .phone("9876543210")
                .build();
    }

    private static Pet pet(Clinic clinic, ClinicPetOwner clinicOwner) {
        Pet pet = Pet.builder()
                .uuid(PET_UUID)
                .clinic(clinic)
                .clinicOwner(clinicOwner)
                .name("Milo")
                .type("Dog")
                .build();
        pet.setId(5L);
        return pet;
    }

    private static User userWithRoles(Long id, String email, ERole... roles) {
        User user = User.builder().email(email).password("x").uuid("u-" + id)
                .firstName("First").lastName("Last").build();
        user.setId(id);
        Set<UserRole> userRoles = new HashSet<>();
        for (ERole eRole : roles) {
            Role role = new Role();
            role.setName(eRole);
            userRoles.add(new UserRole(user, role));
        }
        user.setUserRoles(userRoles);
        return user;
    }
}
