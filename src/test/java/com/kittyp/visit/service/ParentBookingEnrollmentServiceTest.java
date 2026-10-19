package com.kittyp.visit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.entity.ClinicPetEnrollment;
import com.kittyp.clinic.entity.ClinicPetOwner;
import com.kittyp.clinic.enums.PracticeModel;
import com.kittyp.clinic.repository.ClinicPetEnrollmentRepository;
import com.kittyp.clinic.repository.ClinicPetOwnerRepository;
import com.kittyp.clinic.service.ClinicOwnerUserLinkService;
import com.kittyp.common.exception.CustomException;
import com.kittyp.doctor.entity.DoctorPatientEnrollment;
import com.kittyp.doctor.entity.DoctorProfile;
import com.kittyp.doctor.repository.DoctorPatientEnrollmentRepository;
import com.kittyp.doctor.repository.DoctorProfileRepository;
import com.kittyp.user.entity.Pet;
import com.kittyp.user.entity.User;
import com.kittyp.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class ParentBookingEnrollmentServiceTest {

    @Mock
    private DoctorPatientEnrollmentRepository doctorPatientEnrollmentRepository;
    @Mock
    private ClinicPetEnrollmentRepository clinicPetEnrollmentRepository;
    @Mock
    private ClinicPetOwnerRepository clinicPetOwnerRepository;
    @Mock
    private ClinicOwnerUserLinkService clinicOwnerUserLinkService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private DoctorProfileRepository doctorProfileRepository;

    @InjectMocks
    private ParentBookingEnrollmentService service;

    private User practiceOwner;
    private User parent;
    private Clinic clinic;
    private DoctorProfile doctor;
    private ClinicPetOwner clinicOwner;
    private Pet pet;

    @BeforeEach
    void setUp() {
        practiceOwner = User.builder().uuid("doctor-user").build();
        practiceOwner.setId(1L);
        parent = User.builder().uuid("parent-user").email("parent@example.test").build();
        parent.setId(2L);

        clinic = Clinic.builder()
                .owner(practiceOwner)
                .practiceModel(PracticeModel.PERSONAL_PRACTICE)
                .build();
        clinic.setId(3L);
        doctor = DoctorProfile.builder().user(practiceOwner).build();
        doctor.setId(4L);

        clinicOwner = ClinicPetOwner.builder().clinic(clinic).linkedUser(parent).build();
        clinicOwner.setId(5L);
        pet = Pet.builder().uuid("pet-uuid").clinicOwner(clinicOwner).build();
        pet.setId(6L);
    }

    @Test
    void personalPracticeBookingCreatesClinicAndDoctorRelationshipsWithoutChangingPetClinic() {
        when(clinicPetOwnerRepository.findByClinic_IdAndLinkedUser_IdAndIsActiveTrue(3L, 2L))
                .thenReturn(Optional.of(clinicOwner));
        when(clinicOwnerUserLinkService.linkOwnerIfUserExists(clinicOwner)).thenReturn(clinicOwner);
        when(clinicPetEnrollmentRepository.findByClinic_IdAndPet_Id(3L, 6L))
                .thenReturn(Optional.empty());
        when(doctorPatientEnrollmentRepository.findByDoctor_IdAndPet_Id(4L, 6L))
                .thenReturn(Optional.empty());

        service.enrollAfterParentBooking(clinic, doctor, pet, parent);

        verify(clinicPetEnrollmentRepository).save(any(ClinicPetEnrollment.class));
        verify(doctorPatientEnrollmentRepository).save(any(DoctorPatientEnrollment.class));
        assertNull(pet.getClinic());
    }

    @Test
    void clinicRelationshipReactivatesForNewCare() {
        when(clinicPetOwnerRepository.findByClinic_IdAndLinkedUser_IdAndIsActiveTrue(3L, 2L))
                .thenReturn(Optional.of(clinicOwner));
        when(clinicOwnerUserLinkService.linkOwnerIfUserExists(clinicOwner)).thenReturn(clinicOwner);
        ClinicPetEnrollment inactive = ClinicPetEnrollment.builder()
                .clinic(clinic)
                .pet(pet)
                .clinicOwner(clinicOwner)
                .build();
        inactive.setIsActive(false);
        when(clinicPetEnrollmentRepository.findByClinic_IdAndPet_Id(3L, 6L))
                .thenReturn(Optional.of(inactive));

        service.enrollAfterParentBooking(clinic, doctor, pet, parent);

        assertTrue(inactive.getIsActive());
        verify(clinicPetEnrollmentRepository).save(inactive);
    }

    @Test
    void organizationClinicDoesNotEnrollPatientInUnrelatedDoctorRoster() {
        User organizationOwner = User.builder().uuid("clinic-owner").build();
        organizationOwner.setId(7L);
        Clinic organizationClinic = Clinic.builder().owner(organizationOwner).build();
        organizationClinic.setId(8L);
        ClinicPetOwner organizationClient = ClinicPetOwner.builder()
                .clinic(organizationClinic)
                .linkedUser(parent)
                .build();
        when(clinicPetOwnerRepository.findByClinic_IdAndLinkedUser_IdAndIsActiveTrue(8L, 2L))
                .thenReturn(Optional.of(organizationClient));
        when(clinicOwnerUserLinkService.linkOwnerIfUserExists(organizationClient)).thenReturn(organizationClient);
        when(clinicPetEnrollmentRepository.findByClinic_IdAndPet_Id(8L, 6L))
                .thenReturn(Optional.empty());

        service.enrollAfterParentBooking(organizationClinic, doctor, pet, parent);

        verify(clinicPetEnrollmentRepository).save(any(ClinicPetEnrollment.class));
        verify(doctorPatientEnrollmentRepository, never()).save(any(DoctorPatientEnrollment.class));
        assertFalse(ParentBookingEnrollmentService.isPersonalPractice(organizationClinic, doctor));
    }

    @Test
    void staffCareReusesEnrollmentOwnerInsteadOfPlaceholder() {
        ClinicPetOwner enrolled = ClinicPetOwner.builder().clinic(clinic).firstName("Ada").build();
        Pet orphan = Pet.builder().uuid("orphan").build();
        orphan.setId(10L);
        when(userRepository.findByPets_Uuid("orphan")).thenReturn(Optional.empty());
        when(clinicPetEnrollmentRepository.findByClinic_IdAndPet_Id(3L, 10L))
                .thenReturn(Optional.of(ClinicPetEnrollment.builder()
                        .clinic(clinic).pet(orphan).clinicOwner(enrolled).build()));

        service.enrollAfterStaffCare(clinic, null, orphan);

        ArgumentCaptor<ClinicPetEnrollment> captor = ArgumentCaptor.forClass(ClinicPetEnrollment.class);
        verify(clinicPetEnrollmentRepository).save(captor.capture());
        assertEquals(enrolled, captor.getValue().getClinicOwner());
        verify(clinicPetOwnerRepository, never()).save(any());
    }

    @Test
    void staffCareRejectsPetWithNoOwner() {
        Pet orphan = Pet.builder().uuid("orphan").build();
        orphan.setId(10L);
        when(userRepository.findByPets_Uuid("orphan")).thenReturn(Optional.empty());
        when(clinicPetEnrollmentRepository.findByClinic_IdAndPet_Id(3L, 10L)).thenReturn(Optional.empty());

        assertThrows(CustomException.class, () -> service.enrollAfterStaffCare(clinic, null, orphan));
        verify(clinicPetOwnerRepository, never()).save(any());
    }
}
