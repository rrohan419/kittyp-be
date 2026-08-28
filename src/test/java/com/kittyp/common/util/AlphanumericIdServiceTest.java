package com.kittyp.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kittyp.clinic.repository.ClinicRepository;
import com.kittyp.common.entity.PublicIdEntityListener;
import com.kittyp.common.exception.CustomException;
import com.kittyp.doctor.entity.ConsultationInvoice;
import com.kittyp.doctor.repository.ConsultationInvoiceRepository;
import com.kittyp.doctor.repository.DoctorProfileRepository;
import com.kittyp.user.entity.Pet;
import com.kittyp.user.entity.User;
import com.kittyp.user.repository.PetsRepository;
import com.kittyp.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AlphanumericIdServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PetsRepository petsRepository;
    @Mock
    private ClinicRepository clinicRepository;
    @Mock
    private DoctorProfileRepository doctorProfileRepository;
    @Mock
    private ConsultationInvoiceRepository consultationInvoiceRepository;

    @InjectMocks
    private AlphanumericIdService service;

    @Test
    void generateIsSixUppercaseAlphanumeric() {
        for (int i = 0; i < 200; i++) {
            String id = AlphanumericIdService.generate();
            assertEquals(AlphanumericIdService.LENGTH, id.length());
            assertTrue(id.chars().allMatch(c -> AlphanumericIdService.ALPHABET.indexOf(c) >= 0));
        }
    }

    @Test
    void generateProducesDistinctValues() {
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < 500; i++) {
            ids.add(AlphanumericIdService.generate());
        }
        assertTrue(ids.size() > 490);
    }

    @Test
    void allocateUniqueRetriesWhenOccupied() {
        when(userRepository.existsByUuid(anyString())).thenReturn(true, true, false);

        String id = service.allocateUnique(User.class);

        assertEquals(AlphanumericIdService.LENGTH, id.length());
        verify(userRepository, times(3)).existsByUuid(anyString());
    }

    @Test
    void allocateUniqueFailsAfterMaxAttempts() {
        when(userRepository.existsByUuid(anyString())).thenReturn(true);

        assertThrows(CustomException.class, () -> service.allocateUnique(User.class));
        verify(userRepository, times(AlphanumericIdService.MAX_ATTEMPTS)).existsByUuid(anyString());
    }

    @Test
    void persistListenerDoesNotMutateExistingId() {
        User user = User.builder().uuid("legacy-uuid").email("a@b.c").password("x").build();
        new PublicIdEntityListener().assignPublicId(user);
        assertEquals("legacy-uuid", user.getUuid());
    }

    @Test
    void persistListenerAssignsIdWhenBlank() {
        Pet pet = Pet.builder().name("Bruno").build();
        new PublicIdEntityListener().assignPublicId(pet);
        assertEquals(AlphanumericIdService.LENGTH, pet.getUuid().length());
        assertTrue(pet.getUuid().chars().allMatch(c -> AlphanumericIdService.ALPHABET.indexOf(c) >= 0));
        assertNotEquals("legacy-uuid", pet.getUuid());
    }

    @Test
    void persistListenerDoesNotQueryDatabase() {
        Pet pet = Pet.builder().name("Bruno").build();
        new PublicIdEntityListener().assignPublicId(pet);
        verify(petsRepository, never()).existsByUuid(anyString());
        verify(userRepository, never()).existsByUuid(anyString());
        verify(clinicRepository, never()).existsByUuid(anyString());
        verify(doctorProfileRepository, never()).existsByUuid(anyString());
        verify(consultationInvoiceRepository, never()).existsByUuid(anyString());
    }

    @Test
    void generateInvoiceLengthIsSixAlphanumeric() {
        for (int i = 0; i < 200; i++) {
            String id = AlphanumericIdService.generate(AlphanumericIdService.INVOICE_LENGTH);
            assertEquals(AlphanumericIdService.INVOICE_LENGTH, id.length());
            assertTrue(id.chars().allMatch(c -> AlphanumericIdService.ALPHABET.indexOf(c) >= 0));
        }
    }

    @Test
    void allocateUniqueInvoiceUsesSixChars() {
        when(consultationInvoiceRepository.existsByUuid(anyString())).thenReturn(false);

        String id = service.allocateUnique(ConsultationInvoice.class);

        assertEquals(AlphanumericIdService.INVOICE_LENGTH, id.length());
        assertTrue(id.chars().allMatch(c -> AlphanumericIdService.ALPHABET.indexOf(c) >= 0));
        verify(consultationInvoiceRepository, times(1)).existsByUuid(anyString());
    }
}
