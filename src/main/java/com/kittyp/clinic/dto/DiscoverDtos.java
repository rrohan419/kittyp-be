package com.kittyp.clinic.dto;

import java.util.List;

public final class DiscoverDtos {

    private DiscoverDtos() {
    }

    public record DiscoverClinicCard(
            String clinicUuid,
            String name,
            String address,
            String city,
            String phone,
            String profileImageUrl,
            Double latitude,
            Double longitude,
            Double distanceKm,
            Double rating,
            Integer reviewsCount,
            String ratingLabel,
            Integer doctorCount,
            Boolean personal,
            List<DiscoverDoctorCard> doctors) {
        /** Backward-compatible ctor without personal / doctors. */
        public DiscoverClinicCard(
                String clinicUuid,
                String name,
                String address,
                String city,
                String phone,
                String profileImageUrl,
                Double latitude,
                Double longitude,
                Double distanceKm,
                Double rating,
                Integer reviewsCount,
                String ratingLabel,
                Integer doctorCount) {
            this(clinicUuid, name, address, city, phone, profileImageUrl, latitude, longitude, distanceKm, rating,
                    reviewsCount, ratingLabel, doctorCount, false, List.of());
        }

        /** Backward-compatible ctor without nested doctors. */
        public DiscoverClinicCard(
                String clinicUuid,
                String name,
                String address,
                String city,
                String phone,
                String profileImageUrl,
                Double latitude,
                Double longitude,
                Double distanceKm,
                Double rating,
                Integer reviewsCount,
                String ratingLabel,
                Integer doctorCount,
                Boolean personal) {
            this(clinicUuid, name, address, city, phone, profileImageUrl, latitude, longitude, distanceKm, rating,
                    reviewsCount, ratingLabel, doctorCount, personal, List.of());
        }
    }

    public record DiscoverDoctorCard(
            String doctorUuid,
            String clinicUuid,
            String clinicName,
            String name,
            String specialization,
            String photoUrl,
            Double experienceYears,
            Double rating,
            Integer reviewsCount,
            String ratingLabel,
            String registrationNumber,
            String bio,
            Double distanceKm) {
        /** Backward-compatible ctor without distance. */
        public DiscoverDoctorCard(
                String doctorUuid,
                String clinicUuid,
                String clinicName,
                String name,
                String specialization,
                String photoUrl,
                Double experienceYears,
                Double rating,
                Integer reviewsCount,
                String ratingLabel,
                String registrationNumber,
                String bio) {
            this(doctorUuid, clinicUuid, clinicName, name, specialization, photoUrl, experienceYears, rating,
                    reviewsCount, ratingLabel, registrationNumber, bio, null);
        }
    }
}
