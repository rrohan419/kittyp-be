package com.kittyp.breed.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.kittyp.breed.repository.BreedRepository;
import com.kittyp.breed.repository.BreedRepository.BreedSearchResult;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class BreedServiceTest {
    @Mock
    private BreedRepository repository;

    @Test
    void normalizesSpeciesAndReturnsPaginatedBreedAndAliases() {
        BreedSearchResult breed = mock(BreedSearchResult.class);
        when(breed.getId()).thenReturn(42L);
        when(breed.getSpecies()).thenReturn("DOG");
        when(breed.getName()).thenReturn("German Shepherd Dog");
        when(breed.getAliasesJson()).thenReturn("[\"Alsatian\",\"GSD\"]");
        when(repository.search(eq("DOG"), eq("GSD"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(breed), PageRequest.of(1, 1), 3));

        var result = new BreedService(repository, new ObjectMapper()).search(" dog ", " GSD ", 2, 1);

        assertEquals(2, result.getPageNumber());
        assertEquals(3L, result.getTotalElements());
        assertFalse(result.getIsLast());
        assertEquals("German Shepherd Dog", result.getModels().get(0).name());
        assertEquals(List.of("Alsatian", "GSD"), result.getModels().get(0).aliases());
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).search(eq("DOG"), eq("GSD"), pageable.capture());
        assertEquals(1, pageable.getValue().getPageNumber());
        assertEquals(1, pageable.getValue().getPageSize());
    }

    @Test
    void appliesPaginationDefaultsAndSizeLimit() {
        when(repository.search(eq(""), eq(""), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 50), 0));
        var result = new BreedService(repository, new ObjectMapper()).search(null, null, 0, 1000);
        assertEquals(1, result.getPageNumber());
        assertEquals(50, result.getPageSize());
    }
}
