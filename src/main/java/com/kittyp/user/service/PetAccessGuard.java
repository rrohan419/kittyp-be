package com.kittyp.user.service;

import java.util.EnumSet;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.kittyp.clinic.dao.ClinicStaffDao;
import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.entity.ClinicPetOwner;
import com.kittyp.clinic.repository.ClinicDoctorRepository;
import com.kittyp.clinic.service.ClinicOwnerUserLinkService;
import com.kittyp.common.exception.CustomException;
import com.kittyp.doctor.dao.DoctorProfileDao;
import com.kittyp.doctor.entity.DoctorProfile;
import com.kittyp.user.entity.Pet;
import com.kittyp.user.entity.User;
import com.kittyp.user.enums.ERole;
import com.kittyp.user.repository.PetsRepository;
import com.kittyp.visit.enums.VisitStatus;
import com.kittyp.visit.repository.VisitRepository;

import lombok.RequiredArgsConstructor;

/**
 * Central AuthZ for pet-scoped health data (nutrition, daily plans, feeding logs).
 * Owner OR treating doctor / clinic affiliation — never "any ROLE_DOCTOR".
 */
@Service
@RequiredArgsConstructor
public class PetAccessGuard {

    private static final Set<VisitStatus> ATTENDED = EnumSet.of(
            VisitStatus.IN_PROGRESS, VisitStatus.CHECKING_OUT, VisitStatus.COMPLETED);

    private final PetsRepository petsRepository;
    private final ClinicDoctorRepository clinicDoctorRepository;
    private final ClinicStaffDao clinicStaffDao;
    private final DoctorProfileDao doctorProfileDao;
    private final VisitRepository visitRepository;

    public void requirePetAccess(User user, String petUuid) {
        if (!canAccessPet(user, petUuid)) {
            throw new CustomException("You are not authorized to access this pet", HttpStatus.FORBIDDEN);
        }
    }

    /** Doctor/staff clinical relationship — not pet owner alone. */
    public void requireClinicalAccess(User user, String petUuid) {
        if (isAdmin(user)) {
            return;
        }
        if (!hasClinicalRelationship(user, petUuid)) {
            throw new CustomException("You are not authorized to manage this pet's clinical plans",
                    HttpStatus.FORBIDDEN);
        }
    }

    public void requireOwner(User user, String petUuid) {
        if (isAdmin(user)) {
            return;
        }
        if (!isOwner(user, petUuid)) {
            throw new CustomException("You are not authorized to access this pet", HttpStatus.FORBIDDEN);
        }
    }

    public boolean canAccessPet(User user, String petUuid) {
        if (user == null || petUuid == null || petUuid.isBlank()) {
            return false;
        }
        if (isAdmin(user)) {
            return true;
        }
        if (isOwner(user, petUuid)) {
            return true;
        }
        return hasClinicalRelationship(user, petUuid);
    }

    public boolean isOwner(User user, String petUuid) {
        if (user.getPets() != null
                && user.getPets().stream().anyMatch(p -> p.getUuid() != null && p.getUuid().equalsIgnoreCase(petUuid))) {
            return true;
        }
        Pet pet = findPet(petUuid);
        if (pet == null) {
            return false;
        }
        if (user.getUuid() != null && user.getUuid().equals(pet.getParentUserUuid())) {
            return true;
        }
        ClinicPetOwner owner = pet.getClinicOwner();
        if (owner == null) {
            return false;
        }
        if (owner.getLinkedUser() != null && owner.getLinkedUser().getId().equals(user.getId())) {
            return true;
        }
        if (owner.getLinkedUser() != null && !owner.getLinkedUser().getId().equals(user.getId())) {
            return false;
        }
        String ownerEmail = ClinicOwnerUserLinkService.normalizeEmail(owner.getEmail());
        String userEmail = ClinicOwnerUserLinkService.normalizeEmail(user.getEmail());
        if (ownerEmail != null && ownerEmail.equals(userEmail)) {
            return true;
        }
        return false;
    }

    public boolean hasClinicalRelationship(User user, String petUuid) {
        Pet pet = findPet(petUuid);
        if (pet == null) {
            return false;
        }
        String id = pet.getUuid();
        Clinic clinic = pet.getClinic();
        if (clinic != null) {
            Long clinicId = clinic.getId();
            if (clinic.getOwner() != null && clinic.getOwner().getId().equals(user.getId())) {
                return true;
            }
            if (clinicStaffDao.isActiveMember(clinicId, user.getId())) {
                return true;
            }
            if (clinicDoctorRepository.existsByClinic_IdAndDoctor_User_IdAndIsActiveTrue(clinicId, user.getId())) {
                return true;
            }
        }
        DoctorProfile profile = doctorProfileDao.findByUserId(user.getId());
        if (profile == null) {
            return false;
        }
        return visitRepository.existsByDoctor_IdAndPet_UuidAndIsActiveTrueAndStatusIn(
                profile.getId(), id, ATTENDED);
    }

    public boolean isAdmin(User user) {
        return hasRole(user, ERole.ROLE_ADMIN);
    }

    public boolean isDoctorLike(User user) {
        return hasRole(user, ERole.ROLE_DOCTOR)
                || hasRole(user, ERole.ROLE_CLINIC_ADMIN)
                || hasRole(user, ERole.ROLE_CLINIC_STAFF);
    }

    private boolean hasRole(User user, ERole role) {
        return user.getUserRoles() != null && user.getUserRoles().stream()
                .anyMatch(ur -> ur.getRole() != null && ur.getRole().getName() == role);
    }

    private Pet findPet(String petUuid) {
        if (petUuid == null || petUuid.isBlank()) {
            return null;
        }
        String key = petUuid.trim();
        return petsRepository.findOptionalByUuid(key)
                .or(() -> petsRepository.findByUuidIgnoreCase(key))
                .orElse(null);
    }
}
