package com.kittyp.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kittyp.clinic.entity.Clinic;
import com.kittyp.common.exception.CustomException;
import com.kittyp.user.entity.Pet;
import com.kittyp.user.entity.PetWeightLog;
import com.kittyp.user.entity.User;
import com.kittyp.user.repository.PetWeightLogRepository;
import com.kittyp.user.repository.PetsRepository;
import com.kittyp.visit.entity.Visit;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class PetWeightServiceTest {
    @Mock private PetsRepository pets;
    @Mock private PetWeightLogRepository logs;
    @InjectMocks private PetWeightService service;

    private final List<PetWeightLog> entries = new ArrayList<>();
    private final Pet pet = Pet.builder().uuid("pet-1").build();
    private final User doctor = User.builder().firstName("Doctor").build();
    private final User parent = User.builder().firstName("Parent").build();
    private final Clinic clinic = Clinic.builder().name("Clinic").build();

    @BeforeEach
    void setUp() {
        when(pets.findForWeightUpdate("pet-1")).thenReturn(Optional.of(pet));
        when(logs.findByVisit_Id(any())).thenAnswer(invocation -> entries.stream()
                .filter(entry -> entry.getVisit() != null
                        && entry.getVisit().getId().equals(invocation.getArgument(0)))
                .findFirst());
        when(logs.saveAndFlush(any())).thenAnswer(invocation -> {
            PetWeightLog entry = invocation.getArgument(0);
            if (entry.getId() == null) {
                entry.setId((long) entries.size() + 1);
                entries.add(entry);
            }
            return entry;
        });
        when(logs.findFirstByPet_UuidOrderByRecordedAtDescIdDesc("pet-1")).thenAnswer(invocation ->
                entries.stream().max(Comparator.comparing(PetWeightLog::getRecordedAt)
                        .thenComparing(PetWeightLog::getId)));
    }

    private Visit visit(long id) {
        Visit visit = Visit.builder().pet(pet).clinic(clinic).uuid("visit-" + id).build();
        visit.setId(id);
        return visit;
    }

    @Test
    void equalWeightsOnDifferentVisitsAreSeparateButRepeatedSaveIsIdempotent() {
        Visit first = visit(1);
        service.recordVisit(first, 4.5, doctor);
        service.recordVisit(first, 4.5, doctor);
        service.recordVisit(visit(2), 4.5, doctor);

        assertEquals(2, entries.size());
        assertSame(doctor, entries.get(0).getRecordedBy());
        assertSame(first, entries.get(0).getVisit());
        assertEquals("Doctor visit at Clinic", entries.get(0).getNote());
        assertEquals("4.5", pet.getWeight());
    }

    @Test
    void correctionPreservesRecordingTimeAndDoesNotReplaceNewerProfileWeight() {
        Visit visit = visit(1);
        service.recordVisit(visit, 4.5, doctor);
        PetWeightLog measurement = entries.get(0);
        LocalDateTime recordedAt = measurement.getRecordedAt();
        service.recordManual(pet, 6.0, recordedAt.plusDays(1), "At home", parent);
        service.recordVisit(visit, 4.8, parent);

        assertEquals(2, entries.size());
        assertEquals(4.8, measurement.getWeight());
        assertEquals(recordedAt, measurement.getRecordedAt());
        assertSame(doctor, measurement.getRecordedBy());
        assertSame(parent, measurement.getCorrectedBy());
        assertNotNull(measurement.getCorrectedAt());
        assertEquals("6.0", pet.getWeight());
    }

    @Test
    void correctionOfLatestVisitUpdatesProfileAndKeepsOriginalRecorder() {
        Visit visit = visit(1);
        service.recordVisit(visit, 4.5, doctor);
        service.recordVisit(visit, 4.8, parent);
        assertEquals(1, entries.size());
        assertEquals("4.8", pet.getWeight());
        assertSame(doctor, entries.get(0).getRecordedBy());
        assertSame(parent, entries.get(0).getCorrectedBy());
    }

    @Test
    void backdatedManualWeightDoesNotReplaceLatestAndCapturesRecorder() {
        service.recordManual(pet, 6.0, LocalDateTime.now(), null, parent);
        PetWeightLog older = service.recordManual(pet, 4.0, LocalDateTime.now().minusDays(1), "Old", parent);
        assertEquals("6.0", pet.getWeight());
        assertSame(parent, older.getRecordedBy());
        assertEquals("Old", older.getNote());
    }

    @Test
    void invalidWeightsAreRejectedBeforeAnyWrite() {
        for (String value : List.of("0", "-1", "NaN", "Infinity", "invalid")) {
            assertThrows(CustomException.class, () -> PetWeightService.validateWeight(value));
        }
        verify(logs, never()).saveAndFlush(any());
    }

    @Test
    void equalTimestampsUseEntryIdToDetermineLatest() {
        LocalDateTime time = LocalDateTime.now();
        service.recordManual(pet, 4.0, time, null, parent);
        service.recordManual(pet, 5.0, time, null, parent);
        assertEquals("5.0", pet.getWeight());
    }
}
