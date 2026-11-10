package com.kittyp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.kittyp.breed.service.BreedService;

@SpringBootTest(properties = "management.server.port=0")
class KittypApplicationTests {

	@Autowired
	private BreedService breedService;

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void contextLoads() {
	}

	@Test
	@Transactional
	void breedSearchFindsAliasesAndPaginatesDistinctBreeds() {
		String token = UUID.randomUUID().toString();
		String alias = "alias-" + token;
		for (String name : new String[] { "A-" + token, "B-" + token }) {
			jdbc.update("""
					INSERT INTO breed_master (species, name, aliases, is_active, created_at)
					VALUES ('DOG', ?, ARRAY[?, ?], true, current_timestamp)
					""", name, alias, alias + "-alternative");
		}
		jdbc.update("""
				INSERT INTO breed_master (species, name, aliases, is_active, created_at)
				VALUES ('CAT', ?, ARRAY[?], true, current_timestamp)
				""", "C-" + token, alias);

		var first = breedService.search("dog", alias, 1, 1);
		var second = breedService.search("DOG", alias, 2, 1);
		assertEquals(2L, first.getTotalElements());
		assertFalse(first.getIsLast());
		assertTrue(second.getIsLast());
		assertEquals("A-" + token, first.getModels().get(0).name());
		assertEquals(alias, first.getModels().get(0).aliases().get(0));
		assertEquals("B-" + token, second.getModels().get(0).name());
	}
}
