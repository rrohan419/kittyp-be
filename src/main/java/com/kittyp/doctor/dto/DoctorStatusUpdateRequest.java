package com.kittyp.doctor.dto;

import com.kittyp.doctor.enums.DoctorStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DoctorStatusUpdateRequest {
    @NotNull
    private DoctorStatus status;
    private String reviewNotes;
    @Size(max = 2000)
    private String rejectionReason;
}
