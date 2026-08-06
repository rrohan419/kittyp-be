package com.kittyp.common.controller;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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
import com.kittyp.common.util.VerificationCodeService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(ApiUrl.BASE_URL)
@RequiredArgsConstructor
public class FileStorageController {

	private static final long MAX_FILE_BYTES = 10 * 1024 * 1024;
	private static final Set<String> ALLOWED_TYPES = Set.of(
			"image/jpeg", "image/png", "image/webp", "image/gif", "application/pdf");

	private final ApiResponse<?> responseBuilder;
	private final S3StorageService s3StorageService;
	private final VerificationCodeService verificationCodeService;

	@PostMapping(value = "/upload/public-url", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize(KeyConstant.IS_AUTHENTICATED)
	public ResponseEntity<SuccessResponse<List<String>>> uploadFiles(
			@RequestParam("files") List<MultipartFile> multipartFiles,
			@RequestParam(required = false) Boolean isAdminUpload) {

		String email = SecurityContextHolder.getContext().getAuthentication().getName();
		validateFiles(multipartFiles);

		boolean adminUpload = Boolean.TRUE.equals(isAdminUpload);
		if (adminUpload && SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
				.noneMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()))) {
			throw new CustomException("Admin upload not permitted", HttpStatus.FORBIDDEN);
		}

		String folder = adminUpload ? "admin-uploads" : "doctors/" + sanitizeEmail(email);
		return responseBuilder.buildSuccessResponse(
				s3StorageService.uploadMultipleFiles(folder, toRequests(multipartFiles)),
				ResponseMessage.SUCCESS, HttpStatus.OK);
	}

	/**
	 * Unauthenticated upload for doctor signup documents (degree, registration cert, etc.).
	 * Stored under doctors/{email}/ in the shared user bucket.
	 */
	@PostMapping(value = ApiUrl.UPLOAD_SIGNUP_DOCUMENTS, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<SuccessResponse<List<String>>> uploadSignupDocuments(
			@RequestParam("files") List<MultipartFile> multipartFiles,
			@RequestParam("email") String email) {

		if (email == null || email.isBlank() || !email.contains("@")) {
			throw new CustomException("Valid email is required", HttpStatus.BAD_REQUEST);
		}
		String normalized = email.trim().toLowerCase(Locale.ROOT);
		if (!verificationCodeService.isVerified(VerificationCodeService.emailVerifiedKey(normalized))
				&& !verificationCodeService.isVerified(VerificationCodeService.emailVerifiedKey(email.trim()))) {
			throw new CustomException("Email OTP verification required before uploading documents",
					HttpStatus.UNAUTHORIZED);
		}
		validateFiles(multipartFiles);

		return responseBuilder.buildSuccessResponse(
				s3StorageService.uploadMultipleFiles("doctors/" + sanitizeEmail(normalized), toRequests(multipartFiles)),
				ResponseMessage.SUCCESS, HttpStatus.OK);
	}

	private void validateFiles(List<MultipartFile> files) {
		if (files == null || files.isEmpty()) {
			throw new CustomException("At least one file is required", HttpStatus.BAD_REQUEST);
		}
		if (files.size() > 5) {
			throw new CustomException("Maximum 5 files allowed", HttpStatus.BAD_REQUEST);
		}
		for (MultipartFile file : files) {
			if (file == null || file.isEmpty()) {
				throw new CustomException("Empty files are not allowed", HttpStatus.BAD_REQUEST);
			}
			if (file.getSize() > MAX_FILE_BYTES) {
				throw new CustomException("File exceeds 10MB limit", HttpStatus.BAD_REQUEST);
			}
			String type = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
			if (!ALLOWED_TYPES.contains(type)) {
				throw new CustomException("Unsupported file type", HttpStatus.BAD_REQUEST);
			}
			String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
			if (name.contains("..") || name.contains("/") || name.contains("\\")) {
				throw new CustomException("Invalid file name", HttpStatus.BAD_REQUEST);
			}
		}
	}

	private List<FileUploadRequest> toRequests(List<MultipartFile> files) {
		return files.stream().map(file -> {
			try {
				String name = file.getOriginalFilename() == null ? "upload.bin" : file.getOriginalFilename();
				return new FileUploadRequest(name.replaceAll("[^a-zA-Z0-9._-]", "_"), file.getBytes(),
						file.getContentType());
			} catch (IOException e) {
				throw new CustomException("Failed to read file", HttpStatus.BAD_REQUEST);
			}
		}).toList();
	}

	private String sanitizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9.@_-]", "_");
	}
}
