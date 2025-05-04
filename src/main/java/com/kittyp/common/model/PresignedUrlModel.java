package com.kittyp.common.model;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PresignedUrlModel {

    private String uuid;

    private String presignedUrl;

    private Instant expiration;

}