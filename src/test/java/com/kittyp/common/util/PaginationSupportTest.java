package com.kittyp.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.kittyp.common.model.PaginationModel;

class PaginationSupportTest {

	@Test
	void slice_returnsRequestedPage() {
		List<Integer> all = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11);
		PaginationModel<Integer> page = PaginationSupport.slice(all, 2, 5);
		assertEquals(List.of(6, 7, 8, 9, 10), page.getModels());
		assertEquals(11L, page.getTotalElements());
		assertEquals(3, page.getTotalPages());
		assertEquals(Boolean.FALSE, page.getIsFirst());
		assertEquals(Boolean.FALSE, page.getIsLast());
	}

	@Test
	void slice_emptyList_isFirstAndLast() {
		PaginationModel<Integer> page = PaginationSupport.slice(List.of(), 1, 10);
		assertTrue(page.getModels().isEmpty());
		assertEquals(0L, page.getTotalElements());
		assertEquals(0, page.getTotalPages());
		assertEquals(Boolean.TRUE, page.getIsFirst());
		assertEquals(Boolean.TRUE, page.getIsLast());
	}
}
