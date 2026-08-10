package com.kittyp.clinic.service;

import java.util.List;

import com.kittyp.clinic.dto.DiscoverDtos.DiscoverClinicCard;
import com.kittyp.clinic.dto.DiscoverDtos.DiscoverDoctorCard;

public interface DiscoverService {

    List<DiscoverClinicCard> discoverClinics(Double lat, Double lng, Double radiusKm, String city, String q);

    /** Personal-practice doctors (clinic owned by the affiliated doctor). */
    List<DiscoverDoctorCard> discoverPersonalDoctors(Double lat, Double lng, Double radiusKm, String city, String q);

    List<DiscoverDoctorCard> discoverClinicDoctors(String clinicUuid);
}
