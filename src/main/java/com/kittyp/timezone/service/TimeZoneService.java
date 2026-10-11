package com.kittyp.timezone.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kittyp.common.model.PaginationModel;
import com.kittyp.common.util.PaginationSupport;
import com.kittyp.timezone.dto.TimeZoneModel;
import com.kittyp.timezone.entity.TimeZoneEntry;
import com.kittyp.timezone.repository.TimeZoneRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TimeZoneService {

	private final TimeZoneRepository timeZoneRepository;

	@Transactional(readOnly = true)
	public PaginationModel<TimeZoneModel> search(String search, Integer pageNumber, Integer pageSize) {
		int page = pageNumber == null || pageNumber < 1 ? 1 : pageNumber;
		Pageable pageable = PageRequest.of(page - 1, PaginationSupport.clampSize(pageSize),
				Sort.by(Sort.Direction.ASC, "timezoneId"));
		Page<TimeZoneEntry> result = timeZoneRepository.findAll(TimeZoneSpecification.search(search), pageable);
		return PaginationSupport.fromPage(result.map(timeZone -> new TimeZoneModel(
				timeZone.getTimezoneId(), timeZone.getDisplayName())));
	}
}
