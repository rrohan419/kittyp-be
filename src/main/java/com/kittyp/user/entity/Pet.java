package com.kittyp.user.entity;

import java.time.LocalDate;
import java.time.Period;
import java.util.Set;

import org.hibernate.annotations.DynamicUpdate;

import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.entity.ClinicPetOwner;
import com.kittyp.common.entity.BaseEntity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "pets")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@DynamicUpdate
@EqualsAndHashCode(callSuper = true, exclude = { "clinic", "clinicOwner" })
public class Pet extends BaseEntity {

    @Column(name = "uuid", nullable = false, unique = true)
    private String uuid;

    private String name;

    @Column(columnDefinition = "TEXT")
    private String profilePicture;

    private String type;

    private String breed;

    private LocalDate dateOfBirth;

    private String weight;

    private String activityLevel;

    private String gender;

    @ElementCollection
    @CollectionTable(name = "pet_photos", joinColumns = @JoinColumn(name = "pet_id"))
    @Column(name = "photo_url")
    private Set<String> photos;

    private boolean isNeutered;

    private String currentFoodBrand;

    @Column(columnDefinition = "TEXT")
    private String healthConditions;

    @Column(columnDefinition = "TEXT")
    private String allergies;

    @Column(name = "microchip_number")
    private String microchipNumber;

    @Column(name = "patient_number")
    private String patientNumber;

    private LocalDate registeredAt;

    /**
     * When true, parent removed this pet from My Pets — do not auto-reattach on login/clinic link.
     * Clinic/doctor records remain (isActive / clinicOwner unchanged).
     */
    @Column(name = "hidden_from_parent")
    @Builder.Default
    private Boolean hiddenFromParent = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_id")
    @ToString.Exclude
    private Clinic clinic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_owner_id")
    @ToString.Exclude
    private ClinicPetOwner clinicOwner;

    public int getAgeInMonths() {
        return dateOfBirth == null ? 0 : Period.between(dateOfBirth, LocalDate.now()).getMonths() +
                (Period.between(dateOfBirth, LocalDate.now()).getYears() * 12);
    }

    /** Portable medical identity — same as {@link #uuid}. */
    public String resolveGlobalPetId() {
        return uuid;
    }
}
