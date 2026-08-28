package com.kittyp.common.enums;

public enum DoctorSpecialization {
    
    GENERAL_VETERINARY_MEDICINE("General Veterinary Medicine"),
    SURGERY("Surgery"), DERMATOLOGY("Dermatology"), DENTISTRY("Dentistry"), CARDIOLOGY("Cardiology"),
    INTERNAL_MEDICINE("Internal Medicine"), ONCOLOGY("Oncology"), OPHTHALMOLOGY("Ophthalmology"),
    NUROLOGY("Neurology"), EMERGENCY_AND_CRITICAL_CARE("Emergency and Critical Care"), BEHAVIOUR("Behaviour"),
    NUTRITION("Nutrition"), EXOTIC_ANIMAL_MEDICINE("Exotic Animal Medicine"), ANESTHESIOLOGY("Anesthesiology"),
    REHABILITATION_AND_PHYSICAL_THERAPY("Rehabilitation and Physical Therapy"), ZOO_MEDICINE("Zoo Medicine"),
    AVIAN_MEDICINE("Avian Medicine"), AQUATIC_ANIMAL_MEDICINE("Aquatic Animal Medicine"), WILDLIFE_MEDICINE("Wildlife Medicine"),
    LABORATORY_ANIMAL_MEDICINE("Laboratory Animal Medicine"), THERIOGENOLOGY("Theriogenology"), VETERINARY_PATHOLOGY("Veterinary Pathology"), VETERINARY_PUBLIC_HEALTH("Veterinary Public Health"),
    VETERINARY_TOXICOLOGY("Veterinary Toxicology"), VETERINARY_PARASITOLOGY("Veterinary Parasitology"), VETERINARY_PHARMACOLOGY("Veterinary Pharmacology"), VETERINARY_MICROBIOLOGY("Veterinary Microbiology"),
    VETERINARY_IMMUNOLOGY("Veterinary Immunology"), VETERINARY_GENETICS("Veterinary Genetics"), VETERINARY_EPIZOOTIOLOGY("Veterinary Epizootiology"), VETERINARY_BIOTECHNOLOGY("Veterinary Biotechnology"),
    VETERINARY_NUTRITION("Veterinary Nutrition"), VETERINARY_SURGERY("Veterinary Surgery"), VETERINARY_DERMATOLOGY("Veterinary Dermatology"), VETERINARY_CARDIOLOGY("Veterinary Cardiology"),
    VETERINARY_INTERNAL_MEDICINE("Veterinary Internal Medicine");

    private final String specialization;

    DoctorSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public String getSpecialization() {
        return specialization;
    }

}
