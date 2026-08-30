package com.kittyp.health.entity;

import java.time.LocalDate;
import java.util.List;

import org.hibernate.annotations.DynamicUpdate;

import com.kittyp.common.entity.BaseEntity;
import com.kittyp.clinic.entity.Clinic;
import com.kittyp.health.enums.HealthEventStatus;
import com.kittyp.health.enums.HealthEventType;
import com.kittyp.user.entity.Pet;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "health_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@DynamicUpdate
@EqualsAndHashCode(callSuper = true)
public class HealthEvent extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String uuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", nullable = false)
    private Pet pet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_id")
    private Clinic clinic;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private HealthEventType type;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private LocalDate date;

    private Boolean isPast;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private HealthEventStatus status;

    @Column(name = "visit_uuid", length = 64)
    private String visitUuid;

    @ElementCollection
    @CollectionTable(name = "health_event_attachments", joinColumns = @JoinColumn(name = "health_event_id"))
    @Column(name = "attachment_url")
    private List<String> attachments;
}
