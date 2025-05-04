package com.kittyp.common.model;

import org.springframework.core.io.InputStreamResource;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FileDownloadModel {

    private String finalName;

    private String contentType;

    private InputStreamResource fileStream;

}
