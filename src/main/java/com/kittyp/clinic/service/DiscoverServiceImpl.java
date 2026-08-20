package com.kittyp.clinic.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kittyp.clinic.dto.DiscoverDtos.DiscoverClinicCard;
import com.kittyp.clinic.dto.DiscoverDtos.DiscoverDoctorCard;
import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.entity.ClinicDoctor;
import com.kittyp.clinic.enums.ClinicStatus;
import com.kittyp.clinic.repository.ClinicDoctorRepository;
import com.kittyp.clinic.repository.ClinicRepository;
import com.kittyp.common.exception.CustomException;
import com.kittyp.common.exception.ResourceNotFoundException;
import com.kittyp.doctor.entity.DoctorProfile;
import com.kittyp.visit.service.ParentBookingEnrollmentService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DiscoverServiceImpl implements DiscoverService {

    private static final double DEFAULT_RADIUS_KM = 25.0;

    private final ClinicRepository clinicRepository;
    private final ClinicDoctorRepository clinicDoctorRepository;

    @Override
    @Transactional(readOnly = true)
    public List<DiscoverClinicCard> discoverClinics(Double lat, Double lng, Double radiusKm, String city, String q) {
        String cityFilter = blankToNull(city);
        String query = blankToNull(q);
        double radius = radiusKm == null || radiusKm <= 0 ? DEFAULT_RADIUS_KM : radiusKm;
        boolean hasGps = lat != null && lng != null;

        List<DiscoverClinicCard> cards = new ArrayList<>();
        for (Clinic clinic : clinicRepository.findDiscoverable()) {
            List<ClinicDoctor> affiliations = clinicDoctorRepository.findByClinic_IdAndIsActiveTrue(clinic.getId());
            Double distance = distanceKm(clinic, lat, lng, hasGps);
            List<DiscoverDoctorCard> doctorCards = affiliations.stream()
                    .filter(aff -> {
                        DoctorProfile doctor = aff.getDoctor();
                        return doctor != null && doctor.getStatus() != null && doctor.getStatus().isPracticeReady();
                    })
                    .map(aff -> toDoctorCard(aff, clinic, distance))
                    .sorted(Comparator
                            .comparing(DiscoverDoctorCard::rating, Comparator.nullsLast(Comparator.reverseOrder()))
                            .thenComparing(DiscoverDoctorCard::name, Comparator.nullsLast(String::compareToIgnoreCase)))
                    .toList();

            boolean clinicMatched = matchesClinicFields(clinic, cityFilter, query);
            boolean doctorMatched = query != null && doctorCards.stream().anyMatch(d -> matchesDoctorQuery(d, query));
            if (cityFilter != null && !matchesClinicCity(clinic, cityFilter)) {
                continue;
            }
            if (query != null && !clinicMatched && !doctorMatched) {
                continue;
            }

            List<DiscoverDoctorCard> nestedDoctors = doctorCards;
            if (query != null && !clinicMatched) {
                // Doctor-name search: only show matching doctors under the clinic.
                nestedDoctors = doctorCards.stream().filter(d -> matchesDoctorQuery(d, query)).toList();
            }

            DiscoverClinicCard card = toClinicCard(clinic, affiliations, nestedDoctors, distance);
            if (!withinRadius(card.distanceKm(), hasGps, radius)) {
                continue;
            }
            cards.add(card);
        }

        cards.sort(clinicComparator(hasGps, cityFilter));
        return cards;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DiscoverDoctorCard> discoverPersonalDoctors(Double lat, Double lng, Double radiusKm, String city,
            String q) {
        String cityFilter = blankToNull(city);
        String query = blankToNull(q);
        double radius = radiusKm == null || radiusKm <= 0 ? DEFAULT_RADIUS_KM : radiusKm;
        boolean hasGps = lat != null && lng != null;

        List<DiscoverDoctorCard> cards = new ArrayList<>();
        for (Clinic clinic : clinicRepository.findDiscoverable()) {
            List<ClinicDoctor> affiliations = clinicDoctorRepository.findByClinic_IdAndIsActiveTrue(clinic.getId());
            ClinicDoctor personalAff = findPersonalAffiliation(clinic, affiliations);
            if (personalAff == null) {
                continue;
            }
            DoctorProfile personalDoctor = personalAff.getDoctor();
            if (personalDoctor == null || personalDoctor.getStatus() == null
                    || !personalDoctor.getStatus().isPracticeReady()) {
                continue;
            }
            DiscoverDoctorCard card = toDoctorCard(personalAff, clinic, distanceKm(clinic, lat, lng, hasGps));
            if (!matchesPersonalDoctorFilters(clinic, card, cityFilter, query)) {
                continue;
            }
            if (!withinRadius(card.distanceKm(), hasGps, radius)) {
                continue;
            }
            cards.add(card);
        }

        Comparator<DiscoverDoctorCard> byDistance = Comparator
                .comparing(DiscoverDoctorCard::distanceKm, Comparator.nullsLast(Double::compareTo));
        Comparator<DiscoverDoctorCard> byRating = Comparator
                .comparing(DiscoverDoctorCard::rating, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(DiscoverDoctorCard::name, Comparator.nullsLast(String::compareToIgnoreCase));
        cards.sort(hasGps ? byDistance.thenComparing(byRating) : byRating);
        return cards;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DiscoverDoctorCard> discoverClinicDoctors(String clinicUuid) {
        Clinic clinic = clinicRepository.findByUuid(clinicUuid);
        if (clinic == null || Boolean.FALSE.equals(clinic.getIsActive())) {
            throw new ResourceNotFoundException("clinic", "uuid", clinicUuid);
        }
        if (clinic.getStatus() == null || !clinic.getStatus().isActivated()) {
            throw new CustomException(ClinicStatus.NOT_ACTIVATED_MESSAGE, HttpStatus.BAD_REQUEST);
        }
        return clinicDoctorRepository.findByClinic_IdAndIsActiveTrue(clinic.getId()).stream()
                .filter(aff -> {
                    DoctorProfile doctor = aff.getDoctor();
                    return doctor != null && doctor.getStatus() != null && doctor.getStatus().isPracticeReady();
                })
                .map(aff -> toDoctorCard(aff, clinic, null))
                .sorted(Comparator
                        .comparing(DiscoverDoctorCard::rating, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(DiscoverDoctorCard::name, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
    }

    private static boolean matchesClinicCity(Clinic clinic, String cityFilter) {
        String city = clinic.getCity() == null ? "" : clinic.getCity().toLowerCase();
        String address = clinic.getAddress() == null ? "" : clinic.getAddress().toLowerCase();
        String cf = cityFilter.toLowerCase();
        return city.equals(cf) || city.contains(cf) || address.contains(cf);
    }

    private static boolean matchesClinicFields(Clinic clinic, String cityFilter, String query) {
        if (cityFilter == null && query == null) {
            return true;
        }
        String city = clinic.getCity() == null ? "" : clinic.getCity().toLowerCase();
        String address = clinic.getAddress() == null ? "" : clinic.getAddress().toLowerCase();
        String name = clinic.getName() == null ? "" : clinic.getName().toLowerCase();
        if (cityFilter != null && !matchesClinicCity(clinic, cityFilter)) {
            return false;
        }
        if (query != null) {
            String qf = query.toLowerCase();
            return name.contains(qf) || city.contains(qf) || address.contains(qf);
        }
        return true;
    }

    private static boolean matchesDoctorQuery(DiscoverDoctorCard card, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        String qf = query.toLowerCase();
        String doctorName = card.name() == null ? "" : card.name().toLowerCase();
        String specialization = card.specialization() == null ? ""
                : card.specialization().toLowerCase().replace('_', ' ');
        return doctorName.contains(qf) || specialization.contains(qf);
    }

    private static boolean matchesPersonalDoctorFilters(Clinic clinic, DiscoverDoctorCard card, String cityFilter,
            String query) {
        if (cityFilter == null && query == null) {
            return true;
        }
        String city = clinic.getCity() == null ? "" : clinic.getCity().toLowerCase();
        String address = clinic.getAddress() == null ? "" : clinic.getAddress().toLowerCase();
        String clinicName = clinic.getName() == null ? "" : clinic.getName().toLowerCase();
        String doctorName = card.name() == null ? "" : card.name().toLowerCase();
        String specialization = card.specialization() == null ? ""
                : card.specialization().toLowerCase().replace('_', ' ');
        if (cityFilter != null) {
            String cf = cityFilter.toLowerCase();
            if (!(city.equals(cf) || city.contains(cf) || address.contains(cf))) {
                return false;
            }
        }
        if (query != null) {
            String qf = query.toLowerCase();
            if (!(doctorName.contains(qf) || specialization.contains(qf) || clinicName.contains(qf)
                    || city.contains(qf) || address.contains(qf))) {
                return false;
            }
        }
        return true;
    }

    private DiscoverClinicCard toClinicCard(Clinic clinic, List<ClinicDoctor> affiliations,
            List<DiscoverDoctorCard> nestedDoctors, Double distance) {
        double weightedSum = 0;
        long weight = 0;
        for (ClinicDoctor affiliation : affiliations) {
            DoctorProfile d = affiliation.getDoctor();
            if (d == null || d.getRating() == null || d.getReviewsCount() == null || d.getReviewsCount() <= 0) {
                continue;
            }
            weightedSum += d.getRating() * d.getReviewsCount();
            weight += d.getReviewsCount();
        }
        Double clinicRating = weight == 0 ? null : Math.round((weightedSum / weight) * 10.0) / 10.0;
        boolean personal = findPersonalAffiliation(clinic, affiliations) != null;
        return new DiscoverClinicCard(
                clinic.getUuid(),
                clinic.getName(),
                clinic.getAddress(),
                clinic.getCity(),
                clinic.getPhone(),
                clinic.getProfileImageUrl(),
                clinic.getLatitude(),
                clinic.getLongitude(),
                distance,
                clinicRating,
                weight == 0 ? null : (int) weight,
                ratingLabel(clinicRating),
                affiliations.size(),
                personal,
                nestedDoctors == null ? List.of() : nestedDoctors);
    }

    private static ClinicDoctor findPersonalAffiliation(Clinic clinic, List<ClinicDoctor> affiliations) {
        if (clinic == null || clinic.getOwner() == null || affiliations == null) {
            return null;
        }
        Long ownerUserId = clinic.getOwner().getId();
        for (ClinicDoctor affiliation : affiliations) {
            DoctorProfile doctor = affiliation.getDoctor();
            if (doctor != null && doctor.getUser() != null
                    && ParentBookingEnrollmentService.isPersonalPractice(clinic, doctor)
                    && ownerUserId.equals(doctor.getUser().getId())) {
                return affiliation;
            }
        }
        return null;
    }

    private DiscoverDoctorCard toDoctorCard(ClinicDoctor affiliation, Clinic clinic, Double distanceKm) {
        DoctorProfile doctor = affiliation.getDoctor();
        String name = doctor.getUser() == null ? "Doctor"
                : ((doctor.getUser().getFirstName() == null ? "" : doctor.getUser().getFirstName()) + " "
                        + (doctor.getUser().getLastName() == null ? "" : doctor.getUser().getLastName())).trim();
        if (name.isBlank()) {
            name = "Doctor";
        }
        return new DiscoverDoctorCard(
                doctor.getUuid(),
                clinic.getUuid(),
                clinic.getName(),
                name,
                doctor.getSpecialization() == null ? null : doctor.getSpecialization().name(),
                doctor.getPhotoUrl(),
                doctor.getExperienceYears(),
                doctor.getRating(),
                doctor.getReviewsCount(),
                ratingLabel(doctor.getRating()),
                doctor.getRegistrationNumber(),
                doctor.getBio(),
                distanceKm);
    }

    private static Double distanceKm(Clinic clinic, Double lat, Double lng, boolean hasGps) {
        if (!hasGps || clinic.getLatitude() == null || clinic.getLongitude() == null) {
            return null;
        }
        return Math.round(haversineKm(lat, lng, clinic.getLatitude(), clinic.getLongitude()) * 10.0) / 10.0;
    }

    private static boolean withinRadius(Double distanceKm, boolean hasGps, double radius) {
        if (!hasGps) {
            return true;
        }
        if (distanceKm == null) {
            return true;
        }
        return distanceKm <= radius;
    }

    private static Comparator<DiscoverClinicCard> clinicComparator(boolean hasGps, String cityFilter) {
        if (hasGps) {
            return Comparator
                    .comparing(DiscoverClinicCard::distanceKm, Comparator.nullsLast(Double::compareTo))
                    .thenComparing(DiscoverClinicCard::rating, Comparator.nullsLast(Comparator.reverseOrder()));
        }
        if (cityFilter != null) {
            return Comparator
                    .comparing((DiscoverClinicCard c) -> cityFilter.equalsIgnoreCase(Objects.toString(c.city(), "")),
                            Comparator.reverseOrder())
                    .thenComparing(DiscoverClinicCard::rating, Comparator.nullsLast(Comparator.reverseOrder()));
        }
        return Comparator
                .comparing(DiscoverClinicCard::rating, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(DiscoverClinicCard::name, Comparator.nullsLast(String::compareToIgnoreCase));
    }

    private static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final double r = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return r * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private static String ratingLabel(Double rating) {
        if (rating == null || rating <= 0) {
            return "Not rated yet";
        }
        int n = (int) Math.round(rating);
        if (n <= 1) {
            return "Still warming up";
        }
        if (n == 2) {
            return "Gentle paws";
        }
        if (n == 3) {
            return "Trusted companion";
        }
        if (n == 4) {
            return "Clinic favorite";
        }
        return "Legend of care";
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
