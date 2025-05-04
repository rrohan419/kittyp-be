package com.kittyp.common.model;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FileRecordModel {

    private String uuid;

    private String fileLocation;

    private String originalName;

    private String finalName;

    private Long size;

    private String contentType;
    
    private Date createdAt;
    
    private Date updatedAt;
    
    private Boolean isPasswordProtected;
    
    public String fileCollectionUuid;

}
