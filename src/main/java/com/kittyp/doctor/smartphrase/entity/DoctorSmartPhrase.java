package com.kittyp.doctor.smartphrase.entity;

import org.hibernate.annotations.DynamicUpdate;

import com.kittyp.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "doctor_smart_phrases")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@DynamicUpdate
@EqualsAndHashCode(callSuper = true)
public class DoctorSmartPhrase extends BaseEntity {

    @Column(name = "uuid", nullable = false, unique = true, length = 36)
    private String uuid;

    @Column(name = "doctor_user_uuid", nullable = false, length = 36)
    private String doctorUserUuid;

    @Column(name = "phrase_text", nullable = false, columnDefinition = "TEXT")
    private String phraseText;
}
