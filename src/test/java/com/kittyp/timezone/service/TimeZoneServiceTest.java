package com.kittyp.timezone.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.kittyp.timezone.entity.TimeZoneEntry;
import com.kittyp.timezone.repository.TimeZoneRepository;

@ExtendWith(MockitoExtension.class)
class TimeZoneServiceTest {

	@Mock
	private TimeZoneRepository timeZoneRepository;

	@Test
	void search_buildsSpecificationAndReturnsPaginatedResults() {
		TimeZoneEntry zone = new TimeZoneEntry("Asia/Kolkata", "Asia/Kolkata");
		when(timeZoneRepository.findAll(any(Specification.class), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(zone), PageRequest.of(0, 10), 1));
		TimeZoneService service = new TimeZoneService(timeZoneRepository);

		var result = service.search("kolkata", 1, 10);

		ArgumentCaptor<Specification<TimeZoneEntry>> specification =
				ArgumentCaptor.forClass(Specification.class);
		ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
		verify(timeZoneRepository).findAll(specification.capture(), pageable.capture());
		assertNotNull(specification.getValue());
		assertEquals("Asia/Kolkata", result.getModels().get(0).timezoneId());
		assertEquals(1, result.getPageNumber());
		assertEquals(10, result.getPageSize());
		assertEquals("timezoneId: ASC", pageable.getValue().getSort().toString());
	}

	@Test
	void search_usesPaginationDefaultsForInvalidValues() {
		when(timeZoneRepository.findAll(any(Specification.class), any(Pageable.class)))
				.thenReturn(Page.<TimeZoneEntry>empty(
						PageRequest.of(0, 50, Sort.by(Sort.Direction.ASC, "timezoneId"))));
		TimeZoneService service = new TimeZoneService(timeZoneRepository);

		var result = service.search(null, 0, 1000);

		assertEquals(1, result.getPageNumber());
		assertEquals(50, result.getPageSize());
	}
}
