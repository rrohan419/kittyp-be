package com.kittyp.timezone.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.kittyp.timezone.entity.TimeZoneEntry;

public interface TimeZoneRepository
		extends JpaRepository<TimeZoneEntry, String>, JpaSpecificationExecutor<TimeZoneEntry> {
}
