package com.kittyp.clinic.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kittyp.booking.dao.BookingDao;
import com.kittyp.clinic.dao.ClinicDao;
import com.kittyp.clinic.dao.ClinicStaffDao;
import com.kittyp.clinic.dto.ClinicDtos.ClinicPetMedicalProfileModel;
import com.kittyp.clinic.dto.ClinicDtos.InvoiceSummaryModel;
import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.entity.ClinicPetOwner;
import com.kittyp.clinic.enums.ClinicStatus;
import com.kittyp.clinic.repository.ClinicDoctorRepository;
import com.kittyp.clinic.repository.ClinicPetEnrollmentRepository;
import com.kittyp.doctor.entity.ConsultationInvoice;
import com.kittyp.doctor.enums.ConsultationInvoiceStatus;
import com.kittyp.doctor.repository.ConsultationInvoiceRepository;
import com.kittyp.health.dao.HealthEventDao;
import com.kittyp.user.dao.UserDao;
import com.kittyp.user.entity.Pet;
import com.kittyp.user.entity.Role;
import com.kittyp.user.entity.User;
import com.kittyp.user.entity.UserRole;
import com.kittyp.user.enums.ERole;
import com.kittyp.user.repository.PetsRepository;
import com.kittyp.vaccine.dao.PetVaccineScheduleDao;

@ExtendWith(MockitoExtension.class)
class ClinicServiceImplPetInvoicesTest {

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
    private BookingDao bookingDao;
    @Mock
    private HealthEventDao healthEventDao;
    @Mock
    private PetVaccineScheduleDao petVaccineScheduleDao;
    @Mock
    private ConsultationInvoiceRepository consultationInvoiceRepository;

    @InjectMocks
    private ClinicServiceImpl clinicService;

    @Test
    void petMedicalProfile_includesVisitInvoice_whenPetUuidSetAndOwnerNull() {
        User admin = userWithRoles(1L, EMAIL, ERole.ROLE_CLINIC_ADMIN);
        Clinic clinic = clinic(admin);
        ClinicPetOwner clinicOwner = clinicOwner(clinic);
        Pet pet = pet(clinic, clinicOwner);

        ConsultationInvoice invoice = ConsultationInvoice.builder()
                .uuid("inv-visit-1")
                .clinic(clinic)
                .petUuid(PET_UUID)
                .owner(null)
                .amount(new BigDecimal("1500.00"))
                .currency("INR")
                .status(ConsultationInvoiceStatus.ISSUED)
                .lineItems("[]")
                .build();
        invoice.setCreatedAt(LocalDateTime.of(2026, 8, 20, 10, 0));

        stubAccess(admin, clinic, pet);
        when(healthEventDao.findByClinicAndPet(clinic.getId(), PET_UUID)).thenReturn(List.of());
        when(petVaccineScheduleDao.findByPetUuid(PET_UUID)).thenReturn(List.of());
        when(bookingDao.findByClinic(clinic.getId())).thenReturn(List.of());
        when(consultationInvoiceRepository.findAllByPetUuidOrderByCreatedAtDesc(PET_UUID))
                .thenReturn(List.of(invoice));

        ClinicPetMedicalProfileModel profile = clinicService.petMedicalProfile(CLINIC_UUID, PET_UUID, EMAIL);

        assertEquals(1, profile.invoices().size());
        InvoiceSummaryModel row = profile.invoices().get(0);
        assertEquals("inv-visit-1", row.uuid());
        assertEquals(PET_UUID, row.petUuid());
        assertEquals("1500.00", row.amount());
        assertEquals("ISSUED", row.status());
        assertTrue(row.createdAt() != null);
    }

    @Test
    void invoicesForMedicalProfile_mergesLegacyOwnerRowsMissingPetUuid() {
        User admin = userWithRoles(1L, EMAIL, ERole.ROLE_CLINIC_ADMIN);
        Clinic clinic = clinic(admin);
        ConsultationInvoice petInvoice = ConsultationInvoice.builder()
                .uuid("inv-pet")
                .clinic(clinic)
                .petUuid(PET_UUID)
                .owner(null)
                .amount(new BigDecimal("500"))
                .currency("INR")
                .status(ConsultationInvoiceStatus.PAID)
                .lineItems("[]")
                .build();
        ConsultationInvoice legacy = ConsultationInvoice.builder()
                .uuid("inv-legacy")
                .clinic(clinic)
                .petUuid(null)
                .amount(new BigDecimal("200"))
                .currency("INR")
                .status(ConsultationInvoiceStatus.PAID)
                .lineItems("[]")
                .build();

        when(consultationInvoiceRepository.findAllByPetUuidOrderByCreatedAtDesc(PET_UUID))
                .thenReturn(List.of(petInvoice));
        when(consultationInvoiceRepository.findAllByOwner_IdAndClinic_IdOrderByCreatedAtDesc(9L, clinic.getId()))
                .thenReturn(List.of(legacy));

        List<InvoiceSummaryModel> invoices = clinicService.invoicesForMedicalProfile(clinic.getId(), PET_UUID, 9L);

        assertEquals(2, invoices.size());
        assertEquals("inv-pet", invoices.get(0).uuid());
        assertEquals("inv-legacy", invoices.get(1).uuid());
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
        ClinicPetOwner owner = ClinicPetOwner.builder()
                .uuid("owner-1")
                .clinic(clinic)
                .firstName("Pat")
                .lastName("Owner")
                .email("pat@example.com")
                .phone("9876543210")
                .build();
        owner.setId(10L);
        return owner;
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
