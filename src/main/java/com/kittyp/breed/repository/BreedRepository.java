package com.kittyp.breed.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kittyp.breed.entity.Breed;

public interface BreedRepository extends JpaRepository<Breed, Long> {
    interface BreedSearchResult {
        Long getId();
        String getSpecies();
        String getName();
        String getAliasesJson();
    }

    @Query(value = """
            SELECT b.id AS id, b.species AS species, b.name AS name,
                   cast(array_to_json(b.aliases) AS text) AS "aliasesJson"
            FROM breed_master b
            WHERE b.is_active = true
              AND (:species = '' OR upper(b.species) = :species)
              AND (position(lower(:search) in lower(b.name)) > 0
                   OR EXISTS (SELECT 1 FROM unnest(b.aliases) AS alias
                              WHERE position(lower(:search) in lower(alias)) > 0))
            ORDER BY lower(b.name), b.id
            """,
            countQuery = """
            SELECT count(*) FROM breed_master b
            WHERE b.is_active = true
              AND (:species = '' OR upper(b.species) = :species)
              AND (position(lower(:search) in lower(b.name)) > 0
                   OR EXISTS (SELECT 1 FROM unnest(b.aliases) AS alias
                              WHERE position(lower(:search) in lower(alias)) > 0))
            """, nativeQuery = true)
    Page<BreedSearchResult> search(@Param("species") String species, @Param("search") String search, Pageable pageable);
}
