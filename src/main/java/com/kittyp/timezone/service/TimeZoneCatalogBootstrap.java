package com.kittyp.timezone.service;

import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.kittyp.timezone.entity.TimeZoneEntry;
import com.kittyp.timezone.repository.TimeZoneRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class TimeZoneCatalogBootstrap implements ApplicationRunner {

	private final TimeZoneRepository timeZoneRepository;

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		List<String> zoneIds = ZoneId.getAvailableZoneIds().stream().sorted().toList();
		Set<String> existingIds = timeZoneRepository.findAllById(zoneIds).stream()
				.map(TimeZoneEntry::getTimezoneId)
				.collect(Collectors.toSet());
		List<TimeZoneEntry> missing = zoneIds.stream()
				.filter(zoneId -> !existingIds.contains(zoneId))
				.map(zoneId -> new TimeZoneEntry(zoneId, zoneId.replace('_', ' ')))
				.toList();
		if (!missing.isEmpty()) {
			timeZoneRepository.saveAll(missing);
			log.info("Seeded {} IANA time zones", missing.size());
		}
	}
}
