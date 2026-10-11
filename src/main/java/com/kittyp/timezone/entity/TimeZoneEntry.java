package com.kittyp.timezone.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "time_zones")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeZoneEntry {

	@Id
	@Column(name = "timezone_id", nullable = false, length = 100)
	private String timezoneId;

	@Column(name = "display_name", nullable = false, length = 150)
	private String displayName;
}
