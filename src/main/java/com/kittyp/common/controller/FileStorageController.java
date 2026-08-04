package com.kittyp.common.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.kittyp.common.constants.ApiUrl;
import com.kittyp.common.constants.KeyConstant;
import com.kittyp.common.constants.ResponseMessage;
import com.kittyp.common.dto.ApiResponse;
import com.kittyp.common.dto.FileUploadRequest;
import com.kittyp.common.dto.SuccessResponse;
import com.kittyp.common.exception.CustomException;
import com.kittyp.common.service.S3StorageService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(ApiUrl.BASE_URL)
@RequiredArgsConstructor
public class FileStorageController {

    private final ApiResponse<?> responseBuilder;
    private final S3StorageService s3StorageService;

    @PostMapping(value = "/upload/public-url", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize(KeyConstant.IS_AUTHENTICATED)
    public ResponseEntity<SuccessResponse<List<String>>> uploadFiles(
            @RequestParam("files") List<MultipartFile> multipartFiles, @RequestParam(required = false) Boolean isAdminUpload) {

        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        List<FileUploadRequest> files = multipartFiles.stream()
                .map(file -> {
                    try {
                        return new FileUploadRequest(
                                file.getOriginalFilename(),
                                file.getBytes(),
                                file.getContentType());
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to read file: " + file.getOriginalFilename(), e);
                    }
                })
                .toList();

        String folderName = isAdminUpload != null && isAdminUpload ? "admin-uploads" : "user-uploads/" + email;

        List<String> response = s3StorageService.uploadMultipleFiles(folderName, files);
        return responseBuilder.buildSuccessResponse(response, ResponseMessage.SUCCESS, HttpStatus.OK);
    }

    /**
     * Unauthenticated upload for doctor signup documents (degree, registration cert, etc.).
     * Folder is scoped by email so files are isolated per applicant.
     */
    @PostMapping(value = ApiUrl.UPLOAD_SIGNUP_DOCUMENTS, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SuccessResponse<List<String>>> uploadSignupDocuments(
            @RequestParam("files") List<MultipartFile> multipartFiles,
            @RequestParam("email") String email) {

        if (email == null || email.isBlank()) {
            throw new CustomException("Email is required for signup document upload", HttpStatus.BAD_REQUEST);
        }
        String safeEmail = email.trim().toLowerCase().replaceAll("[^a-z0-9.@_-]", "_");

        List<FileUploadRequest> files = multipartFiles.stream()
                .map(file -> {
                    try {
                        return new FileUploadRequest(
                                file.getOriginalFilename(),
                                file.getBytes(),
                                file.getContentType());
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to read file: " + file.getOriginalFilename(), e);
                    }
                })
                .toList();

        List<String> response = s3StorageService.uploadMultipleFiles("signup-uploads/" + safeEmail, files);
        return responseBuilder.buildSuccessResponse(response, ResponseMessage.SUCCESS, HttpStatus.OK);
    }

}
