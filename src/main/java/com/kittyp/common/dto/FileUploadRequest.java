package com.kittyp.common.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class FileUploadRequest {
    private String fileName;
    private byte[] data;
    private String contentType;
}
