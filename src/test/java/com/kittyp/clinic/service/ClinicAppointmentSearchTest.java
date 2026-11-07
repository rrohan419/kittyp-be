package com.kittyp.clinic.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentMatchers;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.jpa.domain.Specification;

import com.kittyp.clinic.dao.ClinicDao;
import com.kittyp.clinic.dao.ClinicStaffDao;
import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.entity.ClinicPetEnrollment;
import com.kittyp.clinic.entity.ClinicPetOwner;
import com.kittyp.clinic.repository.ClinicDoctorRepository;
import com.kittyp.clinic.repository.ClinicPetEnrollmentRepository;
import com.kittyp.clinic.repository.ClinicPetOwnerRepository;
import com.kittyp.common.util.VerificationCodeService;
import com.kittyp.doctor.repository.ConsultationInvoiceRepository;
import com.kittyp.user.dao.UserDao;
import com.kittyp.user.entity.Pet;
import com.kittyp.user.entity.Role;
import com.kittyp.user.entity.User;
import com.kittyp.user.enums.ERole;
import com.kittyp.user.repository.PetsRepository;
import com.kittyp.user.repository.UserRepository;
import com.kittyp.visit.service.ParentBookingEnrollmentService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClinicAppointmentSearchTest {
    @Mock ClinicDao clinicDao;
    @Mock ClinicStaffDao clinicStaffDao;
    @Mock ClinicDoctorRepository clinicDoctorRepository;
    @Mock UserDao userDao;
    @Mock UserRepository userRepository;
    @Mock PetsRepository petsRepository;
    @Mock ClinicPetOwnerRepository ownerRepository;
    @Mock ClinicPetEnrollmentRepository enrollmentRepository;
    @Mock ClinicOwnerUserLinkService linkService;
    @Mock ParentBookingEnrollmentService enrollmentService;
    @Mock VerificationCodeService verification;
    @Mock ConsultationInvoiceRepository invoices;
    @InjectMocks ClinicServiceImpl service;

    Clinic clinic;
    User parent;
    ClinicPetOwner owner;
    Pet enrolled;
    Pet pending;

    @BeforeEach
    void setup() {
        User staff = User.builder().uuid("staff").email("staff@example.com").build();
        staff.setId(1L);
        clinic = Clinic.builder().uuid("clinic").owner(staff).build();
        clinic.setId(2L);
        parent = User.builder().uuid("parent").email("parent@example.com").firstName("Alice").build();
        parent.setId(3L);
        parent.addRole(new Role(ERole.ROLE_USER));
        owner = ClinicPetOwner.builder().uuid("owner").clinic(clinic).firstName("Alice")
                .email(parent.getEmail()).linkedUser(parent).build();
        owner.setId(4L);
        enrolled = Pet.builder().uuid("pet-one").name("Milo").parentUserUuid("parent").build();
        pending = Pet.builder().uuid("pet-two").name("Milo").parentUserUuid("parent").build();
        parent.setPets(List.of(enrolled, pending));
        when(clinicDao.findByUuid("clinic")).thenReturn(clinic);
        when(userDao.userByEmail("staff@example.com")).thenReturn(staff);
        when(petsRepository.findPlatformPetsOfUser("parent")).thenReturn(List.of(enrolled, pending));
        when(petsRepository.findByParentUserUuid("parent")).thenReturn(List.of(enrolled, pending));
        when(enrollmentRepository.existsByClinic_IdAndPet_UuidAndIsActiveTrue(2L, "pet-one")).thenReturn(true);
    }

    @Test
    void localOwnerResponseIncludesOnlyEnrolledPetsAndRetainsEmptyClients() {
        ClinicPetOwner empty = ClinicPetOwner.builder().uuid("empty").clinic(clinic).firstName("Alice")
                .email("empty@example.com").build();
        empty.setId(5L);
        when(ownerRepository.findAll(any(Specification.class))).thenReturn(List.of(owner, empty));
        when(enrollmentRepository.findByClinic_IdAndIsActiveTrue(2L)).thenReturn(
                List.of(ClinicPetEnrollment.builder().clinic(clinic).clinicOwner(owner).pet(enrolled).build()));
        var result = service.listOwners("clinic", "Alice", "staff@example.com", false, false, true);
        assertEquals(2, result.size());
        var alice = result.stream().filter(o -> o.ownerUuid().equals("owner")).findFirst().orElseThrow();
        assertEquals(List.of("pet-one"), alice.pets().stream().map(p -> p.petUuid()).toList());
        assertTrue(result.stream().anyMatch(o -> o.ownerUuid().equals("empty") && o.pets().isEmpty()));
    }

    @Test
    void localPetSearchDoesNotIncludeInactiveClientPets() {
        ClinicPetOwner inactive = ClinicPetOwner.builder().uuid("inactive").clinic(clinic).firstName("Alice")
                .email("inactive@example.com").build();
        inactive.setId(5L);
        inactive.setIsActive(false);
        Pet inactivePet = Pet.builder().uuid("inactive-pet").name("Milo").build();
        when(enrollmentRepository.findByClinic_IdAndIsActiveTrue(2L)).thenReturn(List.of(
                ClinicPetEnrollment.builder().clinic(clinic).clinicOwner(owner).pet(enrolled).build(),
                ClinicPetEnrollment.builder().clinic(clinic).clinicOwner(inactive).pet(inactivePet).build()));
        var result = service.listPets("clinic", "Milo", "staff@example.com", false, false, true);
        assertEquals(List.of("pet-one"), result.stream().map(p -> p.petUuid()).toList());
    }

    @Test
    void globalSearchIsBoundedAndReturnsEligiblePets() {
        Pet hidden = Pet.builder().uuid("hidden").name("Milo").hiddenFromParent(true).build();
        when(petsRepository.findPlatformPetsOfUser("parent")).thenReturn(List.of(enrolled, pending, hidden));
        when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(parent)));
        var result = service.searchPlatformUsers("clinic", "Milo", "staff@example.com", false, true);
        assertEquals(2, result.get(0).pets().size());
        assertTrue(result.get(0).pets().stream().anyMatch(p -> p.petUuid().equals("pet-one") && p.clinicPatient()));
        assertTrue(result.get(0).pets().stream().anyMatch(p -> p.petUuid().equals("pet-two") && !p.clinicPatient()));
        verify(userRepository, never()).findByUuidIgnoreCase(anyString());
        verify(userRepository).findAll(any(Specification.class), ArgumentMatchers.<Pageable>argThat(p -> p.getPageSize() == 20
                && p.getSort().getOrderFor("createdAt").isDescending()));
    }

    @Test
    void emailMatchedButUnlinkedClientIsNotMarkedAlreadyLinked() {
        ClinicPetOwner unlinked = ClinicPetOwner.builder().uuid("unlinked").clinic(clinic).firstName("Alice")
                .email(parent.getEmail()).build();
        when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(parent)));
        when(ownerRepository.findByClinic_IdAndEmailIgnoreCaseAndIsActiveTrue(2L, parent.getEmail()))
                .thenReturn(Optional.of(unlinked));
        var result = service.searchPlatformUsers("clinic", "Alice", "staff@example.com", false, true).get(0);
        assertEquals("unlinked", result.clinicOwnerUuid());
        assertFalse(result.alreadyClient());
    }

    @Test
    void existingProfileAndFromUserStillAutoAdmitSolePendingPet() {
        parent.setPets(List.of(pending));
        when(petsRepository.findByParentUserUuid("parent")).thenReturn(List.of(pending));
        when(ownerRepository.findByUuidAndClinic_IdAndIsActiveTrue("owner", 2L)).thenReturn(Optional.of(owner));
        when(ownerRepository.findByClinic_IdAndLinkedUser_IdAndIsActiveTrue(2L, 3L)).thenReturn(Optional.of(owner));
        when(userRepository.findByUuid("parent")).thenReturn(Optional.of(parent));
        when(linkService.linkOwnerIfUserExists(owner)).thenReturn(owner);
        service.ensureOwnerFromUser("clinic", "parent", "staff@example.com");
        service.ownerProfile("clinic", "owner", "staff@example.com");
        verify(enrollmentService, times(2)).admitPetToClinic(clinic, pending);
    }

    @Test
    void existingBulkFirstAssociationDoesNotRequirePetConsent() {
        parent.setPets(List.of(pending));
        when(petsRepository.findByParentUserUuid("parent")).thenReturn(List.of(pending));
        when(ownerRepository.findByUuidAndClinic_IdAndIsActiveTrue("owner", 2L)).thenReturn(Optional.of(owner));
        when(petsRepository.findByUuidIgnoreCase("pet-two")).thenReturn(Optional.of(pending));
        service.admitOwnerPets("clinic", "owner", List.of("pet-two"), "staff@example.com");
        verify(enrollmentService).admitPetToClinic(clinic, pending);
        verify(verification, never()).isVerified(anyString());
    }
}
