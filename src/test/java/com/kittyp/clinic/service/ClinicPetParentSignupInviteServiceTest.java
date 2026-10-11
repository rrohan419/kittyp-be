package com.kittyp.clinic.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.kittyp.clinic.entity.ClinicPetParentSignupInvite;
import com.kittyp.clinic.repository.ClinicPetParentSignupInviteRepository;

class ClinicPetParentSignupInviteServiceTest {

    private ClinicPetParentSignupInviteRepository repository;
    private ClinicPetParentSignupInviteService service;

    @BeforeEach
    void setUp() {
        repository = mock(ClinicPetParentSignupInviteRepository.class);
        service = new ClinicPetParentSignupInviteService(repository);
    }

    @Test
    void createInvitePersistsNormalizedEmailAndOnlyTokenHash() {
        when(repository.save(any(ClinicPetParentSignupInvite.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        String token = service.createInvite("  Parent@Example.com ");

        ArgumentCaptor<ClinicPetParentSignupInvite> inviteCaptor =
                ArgumentCaptor.forClass(ClinicPetParentSignupInvite.class);
        verify(repository).save(inviteCaptor.capture());
        ClinicPetParentSignupInvite invite = inviteCaptor.getValue();
        assertEquals("parent@example.com", invite.getEmail());
        assertTrue(token.matches("[A-Za-z0-9_-]{43}"));
        assertEquals(64, invite.getTokenHash().length());
        assertFalse(token.equals(invite.getTokenHash()));
        assertTrue(invite.getExpiresAt().isAfter(LocalDateTime.now().plusHours(23)));
    }

    @Test
    void consumeInviteRequiresMatchingEmailAndConsumesExactlyOnce() {
        String token = "A".repeat(43);
        when(repository.consumeIfValid(anyString(), eq("parent@example.com"), any(LocalDateTime.class)))
                .thenReturn(1, 0);

        assertTrue(service.consumeInvite(token, "PARENT@example.com"));
        assertFalse(service.consumeInvite(token, "parent@example.com"));

        verify(repository, org.mockito.Mockito.times(2))
                .consumeIfValid(anyString(), eq("parent@example.com"), any(LocalDateTime.class));
    }

    @Test
    void getInvitedEmailRejectsExpiredInvite() {
        ClinicPetParentSignupInvite expired = ClinicPetParentSignupInvite.builder()
                .tokenHash("hash")
                .email("parent@example.com")
                .expiresAt(LocalDateTime.now().minusSeconds(1))
                .build();
        when(repository.findByTokenHashAndIsActiveTrue(anyString())).thenReturn(Optional.of(expired));

        org.junit.jupiter.api.Assertions.assertThrows(
                com.kittyp.common.exception.CustomException.class,
                () -> service.getInvitedEmail("invite-token"));
    }
}
