package com.kittyp.clinic.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kittyp.clinic.dao.ClinicDao;
import com.kittyp.clinic.dao.ClinicStaffDao;
import com.kittyp.clinic.dto.ClinicDtos.AddVaccineDueRequest;
import com.kittyp.clinic.dto.ClinicDtos.MarkVaccineGivenRequest;
import com.kittyp.clinic.dto.ClinicDtos.VaccineScheduleModel;
import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.entity.ClinicPetOwner;
import com.kittyp.clinic.enums.ClinicStatus;
import com.kittyp.clinic.repository.ClinicDoctorRepository;
import com.kittyp.clinic.repository.ClinicPetEnrollmentRepository;
import com.kittyp.user.dao.UserDao;
import com.kittyp.user.entity.Pet;
import com.kittyp.user.entity.Role;
import com.kittyp.user.entity.User;
import com.kittyp.user.entity.UserRole;
import com.kittyp.user.enums.ERole;
import com.kittyp.user.repository.PetsRepository;
import com.kittyp.vaccine.dao.PetVaccineScheduleDao;
import com.kittyp.vaccine.entity.PetVaccineSchedule;
import com.kittyp.vaccine.entity.VaccineMaster;
import com.kittyp.vaccine.repository.VaccineMasterRepository;

@ExtendWith(MockitoExtension.class)
class ClinicServiceImplVaccinesTest {

    private static final String CLINIC_UUID = "clinic-1";
    private static final String PET_UUID = "9AP1AU";
    private static final String EMAIL = "clinic@example.com";

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
    private PetVaccineScheduleDao petVaccineScheduleDao;
    @Mock
    private VaccineMasterRepository vaccineMasterRepository;

    @InjectMocks
    private ClinicServiceImpl clinicService;

    @Test
    void addVaccineDue_createsScheduleRow() {
        User admin = user(1L, EMAIL);
        Clinic clinic = clinic(admin);
        Pet pet = pet(clinic);
        VaccineMaster master = VaccineMaster.builder().name("Rabies").species("DOG").build();
        master.setId(3L);
        stubAccess(admin, clinic, pet);
        when(vaccineMasterRepository.findById(3L)).thenReturn(Optional.of(master));
        when(petVaccineScheduleDao.save(any(PetVaccineSchedule.class))).thenAnswer(inv -> {
            PetVaccineSchedule saved = inv.getArgument(0);
            saved.setId(11L);
            return saved;
        });

        VaccineScheduleModel model = clinicService.addVaccineDue(CLINIC_UUID, PET_UUID,
                new AddVaccineDueRequest(3L, LocalDate.of(2026, 9, 15)), EMAIL);

        assertEquals(11L, model.id());
        assertEquals("Rabies", model.vaccineName());
        assertEquals(LocalDate.of(2026, 9, 15), model.dueDate());
        assertEquals(false, model.completed());
    }

    @Test
    void markVaccineGiven_setsCompletedAndCertificate() {
        User admin = user(1L, EMAIL);
        Clinic clinic = clinic(admin);
        Pet pet = pet(clinic);
        VaccineMaster master = VaccineMaster.builder().name("Rabies").species("DOG").build();
        PetVaccineSchedule schedule = PetVaccineSchedule.builder()
                .pet(pet)
                .vaccine(master)
                .dueDate(LocalDate.of(2026, 8, 1))
                .completed(false)
                .build();
        schedule.setId(11L);
        stubAccess(admin, clinic, pet);
        when(petVaccineScheduleDao.findById(11L)).thenReturn(Optional.of(schedule));
        when(petVaccineScheduleDao.save(any(PetVaccineSchedule.class))).thenAnswer(inv -> inv.getArgument(0));

        VaccineScheduleModel model = clinicService.markVaccineGiven(CLINIC_UUID, PET_UUID, 11L,
                new MarkVaccineGivenRequest(LocalDate.of(2026, 8, 20), "https://s3.example/rabies.pdf"), EMAIL);

        assertTrue(model.completed());
        assertEquals(LocalDate.of(2026, 8, 20), model.completedDate());
        assertEquals("https://s3.example/rabies.pdf", model.certificateUrl());
    }

    private void stubAccess(User admin, Clinic clinic, Pet pet) {
        when(userDao.userByEmail(EMAIL)).thenReturn(admin);
        when(clinicDao.findByUuid(CLINIC_UUID)).thenReturn(clinic);
        when(clinicStaffDao.isActiveMember(clinic.getId(), admin.getId())).thenReturn(false);
        when(clinicDoctorRepository.existsByClinic_IdAndDoctor_User_IdAndIsActiveTrue(clinic.getId(), admin.getId()))
                .thenReturn(false);
        when(petsRepository.findByUuidAndClinic_Id(PET_UUID, clinic.getId())).thenReturn(Optional.of(pet));
    }

    private static Clinic clinic(User owner) {
        Clinic clinic = Clinic.builder().uuid(CLINIC_UUID).name("Branch").status(ClinicStatus.VERIFIED).owner(owner)
                .build();
        clinic.setId(100L);
        return clinic;
    }

    private static Pet pet(Clinic clinic) {
        ClinicPetOwner owner = ClinicPetOwner.builder().uuid("owner-1").clinic(clinic).firstName("Pat").lastName("Owner")
                .email("pat@example.com").phone("9876543210").build();
        Pet pet = Pet.builder().uuid(PET_UUID).clinic(clinic).clinicOwner(owner).name("Milo").type("DOG").build();
        pet.setId(5L);
        return pet;
    }

    private static User user(Long id, String email) {
        User user = User.builder().email(email).password("x").uuid("u-" + id).firstName("First").lastName("Last").build();
        user.setId(id);
        Set<UserRole> userRoles = new HashSet<>();
        Role role = new Role();
        role.setName(ERole.ROLE_CLINIC_ADMIN);
        userRoles.add(new UserRole(user, role));
        user.setUserRoles(userRoles);
        return user;
    }
}
