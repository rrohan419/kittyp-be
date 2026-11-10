package com.kittyp.breed.service;

import java.io.IOException;
import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class BreedCatalogBootstrap implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    record SeedBreed(String species, String name, List<String> aliases) {}

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws IOException {
        List<SeedBreed> breeds;
        try (var input = new ClassPathResource("breed-catalog.json").getInputStream()) {
            breeds = objectMapper.readValue(input, new TypeReference<>() {});
        }
        jdbc.execute("ALTER TABLE breed_master ADD COLUMN IF NOT EXISTS aliases text[] NOT NULL DEFAULT ARRAY[]::text[]");
        jdbc.execute("CREATE UNIQUE INDEX IF NOT EXISTS breed_master_species_name_ci "
                + "ON breed_master (upper(species), lower(name))");
        int inserted = 0;
        for (SeedBreed breed : breeds) {
            inserted += jdbc.update(connection -> {
                var statement = connection.prepareStatement("""
                        INSERT INTO breed_master (species, name, aliases, is_active, created_at)
                        VALUES (?, ?, ?, true, current_timestamp) ON CONFLICT DO NOTHING
                        """);
                statement.setString(1, breed.species());
                statement.setString(2, breed.name());
                statement.setArray(3, connection.createArrayOf("text", breed.aliases().toArray(String[]::new)));
                return statement;
            });
        }
        if (inserted > 0) log.info("Seeded {} breeds", inserted);
    }
}
