package com.kittyp.breed.service;

import java.io.UncheckedIOException;
import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kittyp.breed.dto.BreedModel;
import com.kittyp.breed.repository.BreedRepository;
import com.kittyp.common.model.PaginationModel;
import com.kittyp.common.util.PaginationSupport;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BreedService {
    private final BreedRepository repository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public PaginationModel<BreedModel> search(String species, String search, Integer pageNumber, Integer pageSize) {
        int page = pageNumber == null || pageNumber < 1 ? 1 : pageNumber;
        String normalizedSpecies = species == null ? "" : species.trim().toUpperCase(Locale.ROOT);
        String query = search == null ? "" : search.trim();
        return PaginationSupport.fromPage(repository.search(normalizedSpecies, query,
                PageRequest.of(page - 1, PaginationSupport.clampSize(pageSize)))
                .map(breed -> new BreedModel(breed.getId(), breed.getSpecies(), breed.getName(),
                        parseAliases(breed.getAliasesJson()))));
    }

    private List<String> parseAliases(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException("Invalid breed aliases in catalogue", e);
        }
    }
}
