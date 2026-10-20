package com.kittyp.visit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.kittyp.booking.enums.BookingMode;
import com.kittyp.clinic.dao.ClinicDao;
import com.kittyp.clinic.dao.ClinicStaffDao;
import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.entity.ClinicDoctor;
import com.kittyp.clinic.entity.ClinicPetEnrollment;
import com.kittyp.clinic.entity.ClinicPetOwner;
import com.kittyp.clinic.enums.ClinicStatus;
import com.kittyp.clinic.repository.ClinicDoctorRepository;
import com.kittyp.clinic.repository.ClinicPetEnrollmentRepository;
import com.kittyp.clinic.service.ClinicOwnerUserLinkService;
import com.kittyp.common.exception.CustomException;
import com.kittyp.doctor.dao.DoctorProfileDao;
import com.kittyp.doctor.entity.DoctorProfile;
import com.kittyp.doctor.enums.DoctorStatus;
import com.kittyp.doctor.repository.DoctorReviewRepository;
import com.kittyp.user.dao.UserDao;
import com.kittyp.user.entity.Pet;
import com.kittyp.user.entity.User;
import com.kittyp.user.repository.PetsRepository;
import com.kittyp.visit.dao.VisitDao;
import com.kittyp.visit.dto.VisitDtos.PrescriptionFirstRequest;
import com.kittyp.visit.dto.VisitDtos.VisitChartRequest;
import com.kittyp.visit.dto.VisitDtos.VisitModel;
import com.kittyp.visit.entity.Visit;
import com.kittyp.visit.enums.VisitSource;
import com.kittyp.visit.enums.VisitStatus;
import com.kittyp.visit.enums.VisitUrgency;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class VisitServiceImplPrescriptionFirstTest {

    @Mock
    private VisitDao visitDao;
    @Mock
    private ClinicDao clinicDao;
    @Mock
    private ClinicStaffDao clinicStaffDao;
    @Mock
    private ClinicDoctorRepository clinicDoctorRepository;
    @Mock
    private ClinicPetEnrollmentRepository clinicPetEnrollmentRepository;
    @Mock
    private PetsRepository petsRepository;
    @Mock
    private DoctorProfileDao doctorProfileDao;
    @Mock
    private UserDao userDao;
    @Mock
    private ClinicOwnerUserLinkService clinicOwnerUserLinkService;
    @Mock
    private ParentBookingEnrollmentService parentBookingEnrollmentService;
    @Mock
    private DoctorReviewRepository doctorReviewRepository;
    @Mock
    private com.kittyp.clinic.repository.ClinicPetOwnerRepository clinicPetOwnerRepository;
    @Mock
    private com.kittyp.clinic.service.ClinicPetParentSignupInviteService clinicPetParentSignupInviteService;
    @Mock
    private com.kittyp.email.service.ZeptoMailService zeptoMailService;
    @Mock
    private com.kittyp.notification.repository.NotificationLogRepository notificationLogRepository;
    @Mock
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    @Mock
    private com.kittyp.user.repository.PetWeightLogRepository petWeightLogRepository;

    @InjectMocks
    private VisitServiceImpl visitService;

    private final AtomicReference<Visit> saved = new AtomicReference<>();
    private Clinic clinic;
    private Pet pet;
    private VisitChartRequest chart;

    @BeforeEach
    void setUp() {
        User user = User.builder().email("doc@example.com").password("x").uuid("u-doc")
                .firstName("Ajit").lastName("Attarde").build();
        user.setId(8L);
        DoctorProfile profile = DoctorProfile.builder().user(user).status(DoctorStatus.VERIFIED).build();
        profile.setId(3L);
        profile.setUuid("doc-3");

        clinic = Clinic.builder().uuid("clinic-1").name("Clinic one").status(ClinicStatus.VERIFIED).build();
        clinic.setId(10L);
        pet = Pet.builder().uuid("pet-1").name("Hulk").type("Cat").build();
        pet.setIsActive(true);
        chart = new VisitChartRequest("exam", "assessment", "medicine plan", null,
                java.util.Map.of("weightKg", 4.5), null);

        ClinicPetOwner owner = ClinicPetOwner.builder().clinic(clinic).firstName("Owner")
                .email("owner@example.com").phone("9999999999").build();
        ClinicPetEnrollment enrollment = ClinicPetEnrollment.builder()
                .clinic(clinic).pet(pet).clinicOwner(owner).build();

        when(clinicDao.findByUuid("clinic-1")).thenReturn(clinic);
        when(userDao.userByEmail("doc@example.com")).thenReturn(user);
        when(clinicDoctorRepository.existsByClinic_IdAndDoctor_User_IdAndIsActiveTrue(10L, 8L)).thenReturn(true);
        when(doctorProfileDao.findByUserId(8L)).thenReturn(profile);
        when(clinicDoctorRepository.findByClinic_IdAndDoctor_Uuid(10L, "doc-3"))
                .thenReturn(Optional.of(ClinicDoctor.builder().clinic(clinic).doctor(profile).build()));
        when(petsRepository.findByUuidIgnoreCase("pet-1")).thenReturn(Optional.of(pet));
        when(clinicPetEnrollmentRepository.existsByClinic_IdAndPet_UuidAndIsActiveTrue(10L, "pet-1"))
                .thenReturn(true);
        when(clinicPetEnrollmentRepository.findByClinic_IdAndPet_UuidAndIsActiveTrue(10L, "pet-1"))
                .thenReturn(Optional.of(enrollment));
        when(clinicOwnerUserLinkService.linkOwnerIfUserExists(owner)).thenReturn(owner);
        when(visitDao.save(any(Visit.class))).thenAnswer(inv -> {
            Visit v = inv.getArgument(0);
            saved.set(v);
            return v;
        });
        when(visitDao.findByUuid(anyString())).thenAnswer(inv -> Optional.ofNullable(saved.get()));
        when(doctorReviewRepository.findByVisit_Uuid(anyString())).thenReturn(Optional.empty());
        try {
            when(objectMapper.writeValueAsString(any())).thenReturn("{\"weightKg\":4.5}");
        } catch (Exception ignored) {
        }
    }

    private PrescriptionFirstRequest request(boolean confirm) {
        return new PrescriptionFirstRequest("clinic-1", "pet-1", null, null, "checkup",
                VisitUrgency.ROUTINE, confirm, chart);
    }

    @Test
    void createsWalkInVisitAndSavesChart() {
        when(visitDao.findByPetAndClinic("pet-1", 10L)).thenReturn(List.of());

        VisitModel model = visitService.createPrescriptionFirst(request(false), "doc@example.com");

        assertEquals(VisitSource.WALK_IN, model.source());
        assertEquals(VisitStatus.IN_PROGRESS, model.status());
        assertEquals("medicine plan", model.chart().plan());
        assertEquals("pet-1", model.petUuid());
        assertEquals("4.5", pet.getWeight());
        verify(petWeightLogRepository).save(any(com.kittyp.user.entity.PetWeightLog.class));
    }

    @Test
    void openVisitToday_conflict_nothingCreatedOrAttached() {
        Visit candidate = Visit.builder().uuid("visit-old").clinic(clinic).pet(pet)
                .source(VisitSource.WALK_IN).channel(BookingMode.IN_PERSON)
                .status(VisitStatus.IN_PROGRESS).urgency(VisitUrgency.ROUTINE).build();
        candidate.setIsActive(true);
        candidate.setCreatedAt(LocalDateTime.now());
        when(visitDao.findByPetAndClinic("pet-1", 10L)).thenReturn(List.of(candidate));

        CustomException ex = assertThrows(CustomException.class,
                () -> visitService.createPrescriptionFirst(request(false), "doc@example.com"));

        assertEquals(HttpStatus.CONFLICT, ex.getHttpStatus());
        assertNull(candidate.getPlan());
        verify(visitDao, never()).save(any(Visit.class));
    }

    @Test
    void openVisitToday_confirm_createsDistinctVisit() {
        Visit candidate = Visit.builder().uuid("visit-old").clinic(clinic).pet(pet)
                .source(VisitSource.WALK_IN).channel(BookingMode.IN_PERSON)
                .status(VisitStatus.IN_PROGRESS).urgency(VisitUrgency.ROUTINE).build();
        candidate.setIsActive(true);
        candidate.setCreatedAt(LocalDateTime.now());
        when(visitDao.findByPetAndClinic("pet-1", 10L)).thenReturn(List.of(candidate));

        VisitModel model = visitService.createPrescriptionFirst(request(true), "doc@example.com");

        assertNotEquals("visit-old", model.uuid());
        assertNull(candidate.getPlan());
    }
}
