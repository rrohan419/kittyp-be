package com.kittyp.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kittyp.clinic.dao.ClinicStaffDao;
import com.kittyp.clinic.repository.ClinicDoctorRepository;
import com.kittyp.common.exception.CustomException;
import com.kittyp.doctor.dao.DoctorProfileDao;
import com.kittyp.user.entity.Pet;
import com.kittyp.user.entity.Role;
import com.kittyp.user.entity.User;
import com.kittyp.user.entity.UserRole;
import com.kittyp.user.enums.ERole;
import com.kittyp.user.repository.PetsRepository;
import com.kittyp.visit.repository.VisitRepository;

@ExtendWith(MockitoExtension.class)
class PetAccessGuardTest {

    @Mock
    private PetsRepository petsRepository;
    @Mock
    private ClinicDoctorRepository clinicDoctorRepository;
    @Mock
    private ClinicStaffDao clinicStaffDao;
    @Mock
    private DoctorProfileDao doctorProfileDao;
    @Mock
    private VisitRepository visitRepository;

    @InjectMocks
    private PetAccessGuard guard;

    private User owner;
    private User stranger;
    private Pet pet;

    @BeforeEach
    void setUp() {
        owner = User.builder().uuid("owner-uuid").email("owner@test.com").build();
        owner.setId(1L);
        owner.setUserRoles(new HashSet<>());
        stranger = User.builder().uuid("stranger-uuid").email("stranger@test.com").build();
        stranger.setId(2L);
        stranger.setUserRoles(new HashSet<>());

        pet = Pet.builder().uuid("pet-uuid").build();
        pet.setParentUserUuid("owner-uuid");
        List<Pet> pets = new ArrayList<>();
        pets.add(pet);
        owner.setPets(pets);
    }

    @Test
    void ownerCanAccessOwnPet() {
        assertTrue(guard.canAccessPet(owner, "pet-uuid"));
        guard.requirePetAccess(owner, "pet-uuid");
    }

    @Test
    void strangerDeniedForeignPet() {
        when(petsRepository.findOptionalByUuid("pet-uuid")).thenReturn(Optional.of(pet));
        when(doctorProfileDao.findByUserId(2L)).thenReturn(null);
        assertFalse(guard.canAccessPet(stranger, "pet-uuid"));
        assertThrows(CustomException.class, () -> guard.requirePetAccess(stranger, "pet-uuid"));
    }

    @Test
    void clinicalAccessDeniedForOwnerOnly() {
        when(petsRepository.findOptionalByUuid("pet-uuid")).thenReturn(Optional.of(pet));
        when(doctorProfileDao.findByUserId(1L)).thenReturn(null);
        assertThrows(CustomException.class, () -> guard.requireClinicalAccess(owner, "pet-uuid"));
    }

    @Test
    void canonicalPetUuidUsesStoredPublicId() {
        when(petsRepository.findOptionalByUuid("6up32b")).thenReturn(Optional.empty());
        when(petsRepository.findByUuidIgnoreCase("6up32b")).thenReturn(Optional.of(pet));
        pet.setUuid("6UP32B");
        assertEquals("6UP32B", guard.canonicalPetUuid("6up32b"));
    }

    @Test
    void adminBypassesChecks() {
        Role adminRole = new Role();
        adminRole.setName(ERole.ROLE_ADMIN);
        stranger.getUserRoles().add(new UserRole(stranger, adminRole));
        guard.requirePetAccess(stranger, "foreign-pet");
        guard.requireClinicalAccess(stranger, "foreign-pet");
    }
}
