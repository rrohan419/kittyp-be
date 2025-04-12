package com.kittyp.common.model;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ErrorResponse {
    private int statusCode;
    private Date timestamp;
    private String message;
    private String description;
}
