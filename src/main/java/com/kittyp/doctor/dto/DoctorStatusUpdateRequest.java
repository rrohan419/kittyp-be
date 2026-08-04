package com.kittyp.doctor.dto;

import com.kittyp.doctor.enums.DoctorStatus;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DoctorStatusUpdateRequest {
    @NotNull
    private DoctorStatus status;
    private String reviewNotes;
}
