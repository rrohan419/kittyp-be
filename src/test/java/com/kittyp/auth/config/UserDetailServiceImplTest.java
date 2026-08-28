package com.kittyp.auth.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.kittyp.user.entity.Role;
import com.kittyp.user.entity.User;
import com.kittyp.user.entity.UserRole;
import com.kittyp.user.enums.ERole;
import com.kittyp.user.repository.UserRepository;
import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.repository.ClinicRepository;
import com.kittyp.doctor.entity.DoctorProfile;
import com.kittyp.doctor.repository.DoctorProfileRepository;

@ExtendWith(MockitoExtension.class)
class UserDetailServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private DoctorProfileRepository doctorProfileRepository;

    @Mock
    private ClinicRepository clinicRepository;

    @InjectMocks
    private UserDetailServiceImpl userDetailService;

    @Test
    void loadUserByUsername_allowsLegacyUsersWithNullActiveFlag() {
        User user = User.builder()
                .uuid("legacy-uuid")
                .email("clinic1@gmail.com")
                .password("encoded-password")
                .build();
        user.setIsActive(null);

        Role role = new Role();
        role.setName(ERole.ROLE_CLINIC_ADMIN);
        user.setUserRoles(Set.of(new UserRole(user, role)));

        when(userRepository.findByEmail("clinic1@gmail.com")).thenReturn(Optional.of(user));

        UserDetails details = userDetailService.loadUserByUsername("clinic1@gmail.com");

        assertNotNull(details);
        assertTrue(details.isEnabled());
        assertTrue(details.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_CLINIC_ADMIN")));
    }

    @Test
    void loadUserByUsername_rejectsExplicitlyDisabledUsers() {
        User user = User.builder()
                .uuid("disabled-uuid")
                .email("disabled@clinic.com")
                .password("encoded-password")
                .build();
        user.setIsActive(false);
        user.setUserRoles(Set.of());

        when(userRepository.findByEmail("disabled@clinic.com")).thenReturn(Optional.of(user));

        assertThrows(DisabledException.class,
                () -> userDetailService.loadUserByUsername("disabled@clinic.com"));
    }

    @Test
    void loadUserByUsername_resolvesUserPublicId() {
        User user = enabledUser("AB12CD", "doc@example.com");
        when(userRepository.findByUuid("AB12CD")).thenReturn(Optional.of(user));

        UserDetails details = userDetailService.loadUserByUsername("AB12CD");

        assertNotNull(details);
        assertEquals("doc@example.com", details.getUsername());
    }

    @Test
    void loadUserByUsername_resolvesDoctorPublicId() {
        User user = enabledUser("USER01", "vet@example.com");
        DoctorProfile profile = DoctorProfile.builder().uuid("DOC9K2").user(user).build();
        when(userRepository.findByUuid("doc9k2")).thenReturn(Optional.empty());
        when(userRepository.findByUuid("DOC9K2")).thenReturn(Optional.empty());
        when(doctorProfileRepository.findByUuid("doc9k2")).thenReturn(Optional.empty());
        when(doctorProfileRepository.findByUuid("DOC9K2")).thenReturn(Optional.of(profile));

        UserDetails details = userDetailService.loadUserByUsername("doc9k2");

        assertNotNull(details);
        assertEquals("vet@example.com", details.getUsername());
    }

    @Test
    void loadUserByUsername_resolvesClinicPublicIdToOwner() {
        User owner = enabledUser("OWN001", "clinic@example.com");
        Clinic clinic = Clinic.builder().uuid("CLN9K2").owner(owner).build();
        when(userRepository.findByUuid("cln9k2")).thenReturn(Optional.empty());
        when(userRepository.findByUuid("CLN9K2")).thenReturn(Optional.empty());
        when(doctorProfileRepository.findByUuid("cln9k2")).thenReturn(Optional.empty());
        when(doctorProfileRepository.findByUuid("CLN9K2")).thenReturn(Optional.empty());
        when(clinicRepository.findByUuidFetchOwner("cln9k2")).thenReturn(null);
        when(clinicRepository.findByUuidFetchOwner("CLN9K2")).thenReturn(clinic);

        UserDetails details = userDetailService.loadUserByUsername("cln9k2");

        assertNotNull(details);
        assertEquals("clinic@example.com", details.getUsername());
    }

    @Test
    void loadUserByUsername_unknownId_throws() {
        when(userRepository.findByUuid("ZZZZZZ")).thenReturn(Optional.empty());
        when(doctorProfileRepository.findByUuid("ZZZZZZ")).thenReturn(Optional.empty());
        when(clinicRepository.findByUuidFetchOwner("ZZZZZZ")).thenReturn(null);

        assertThrows(UsernameNotFoundException.class,
                () -> userDetailService.loadUserByUsername("ZZZZZZ"));
    }

    private static User enabledUser(String uuid, String email) {
        User user = User.builder()
                .uuid(uuid)
                .email(email)
                .password("encoded-password")
                .build();
        user.setIsActive(true);
        user.setUserRoles(Set.of());
        return user;
    }
}
