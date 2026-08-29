package com.kittyp.article.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kittyp.article.dto.AuthorDto;
import com.kittyp.article.model.AuthorModel;
import com.kittyp.article.service.AuthorService;
import com.kittyp.common.constants.ApiUrl;
import com.kittyp.common.constants.KeyConstant;
import com.kittyp.common.constants.ResponseMessage;
import com.kittyp.common.dto.ApiResponse;
import com.kittyp.common.dto.SuccessResponse;
import com.kittyp.common.exception.ResourceNotFoundException;
import com.kittyp.common.model.PaginationModel;
import com.kittyp.user.entity.User;
import com.kittyp.user.enums.ERole;
import com.kittyp.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(ApiUrl.BASE_URL)
@RequiredArgsConstructor
public class AuthorController {

	private final ApiResponse<?> responseBuilder;
	private final AuthorService authorService;
	private final UserRepository userRepository;

	@GetMapping(ApiUrl.ALL_AUTHORS)
	public ResponseEntity<SuccessResponse<PaginationModel<AuthorModel>>> getAllAuthors(
			@RequestParam(defaultValue = KeyConstant.PAGE_NUMBER) int page,
			@RequestParam(defaultValue = KeyConstant.PAGE_SIZE) int size) {

		PaginationModel<AuthorModel> response = authorService.getAllAuthors(page, size);
		return responseBuilder.buildSuccessResponse(response, ResponseMessage.SUCCESS, HttpStatus.OK);
	}

	@PreAuthorize(KeyConstant.IS_ROLE_ARTICLE_PUBLISHER)
	@GetMapping(ApiUrl.AUTHOR_ME)
	public ResponseEntity<SuccessResponse<AuthorModel>> myAuthorProfile() {
		User user = currentUser();
		String displayName = ((user.getFirstName() != null ? user.getFirstName() : "") + " "
				+ (user.getLastName() != null ? user.getLastName() : "")).trim();
		AuthorModel author = authorService.getOrCreateForUser(user.getUuid(), displayName, user.getProfilePictureUrl(),
				authorRoleLabel(user));
		return responseBuilder.buildSuccessResponse(author, ResponseMessage.SUCCESS, HttpStatus.OK);
	}

	@PreAuthorize(KeyConstant.IS_ROLE_ADMIN)
	@PostMapping(ApiUrl.CREATE_AUTHOR)
	public ResponseEntity<SuccessResponse<AuthorModel>> createAuthor(@RequestBody AuthorDto authorDto) {
		AuthorModel response = authorService.saveAuthor(authorDto);
		return responseBuilder.buildSuccessResponse(response, ResponseMessage.SUCCESS, HttpStatus.CREATED);
	}

	@PreAuthorize(KeyConstant.IS_ROLE_ADMIN)
	@PatchMapping(ApiUrl.AUTHOR_BY_ID)
	public ResponseEntity<SuccessResponse<AuthorModel>> updateAuthor(@RequestBody AuthorDto authorDto,
			@PathVariable Long id) {
		AuthorModel response = authorService.editAuthorById(authorDto, id);
		return responseBuilder.buildSuccessResponse(response, ResponseMessage.SUCCESS, HttpStatus.OK);
	}

	private User currentUser() {
		String email = SecurityContextHolder.getContext().getAuthentication().getName();
		return userRepository.findByEmail(email)
				.orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
	}

	private static String authorRoleLabel(User user) {
		if (user.getUserRoles() == null) {
			return "AUTHOR";
		}
		boolean doctor = user.getUserRoles().stream()
				.anyMatch(ur -> ur.getRole() != null && ur.getRole().getName() == ERole.ROLE_DOCTOR);
		if (doctor) {
			return "DOCTOR";
		}
		boolean clinic = user.getUserRoles().stream()
				.anyMatch(ur -> ur.getRole() != null && ur.getRole().getName() == ERole.ROLE_CLINIC_ADMIN);
		if (clinic) {
			return "CLINIC";
		}
		boolean admin = user.getUserRoles().stream()
				.anyMatch(ur -> ur.getRole() != null && ur.getRole().getName() == ERole.ROLE_ADMIN);
		return admin ? "ADMIN" : "AUTHOR";
	}
}
