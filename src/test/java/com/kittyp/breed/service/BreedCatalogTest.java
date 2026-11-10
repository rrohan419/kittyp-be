package com.kittyp.breed.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kittyp.breed.service.BreedCatalogBootstrap.SeedBreed;

class BreedCatalogTest {
    @Test
    void catalogHasUniqueNamesAndExpectedAliases() throws IOException {
        List<SeedBreed> breeds;
        try (var input = new ClassPathResource("breed-catalog.json").getInputStream()) {
            breeds = new ObjectMapper().readValue(input, new TypeReference<>() {});
        }
        assertEquals(105, breeds.size());
        Set<String> keys = new HashSet<>();
        for (SeedBreed breed : breeds) {
            assertTrue(keys.add(breed.species() + ":" + breed.name().toLowerCase(Locale.ROOT)));
            assertTrue(breed.aliases().stream().noneMatch(String::isBlank));
        }
        assertTrue(breeds.stream().anyMatch(b -> b.name().equals("German Shepherd Dog")
                && b.aliases().contains("GSD")));
        assertTrue(breeds.stream().anyMatch(b -> b.name().equals("Persian")
                && b.aliases().contains("Persian Longhair")));
    }
}
