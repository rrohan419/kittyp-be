/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.product.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kittyp.common.constants.ApiUrl;
import com.kittyp.common.constants.KeyConstant;
import com.kittyp.common.constants.ResponseMessage;
import com.kittyp.common.dto.ApiResponse;
import com.kittyp.common.dto.SuccessResponse;
import com.kittyp.common.model.PaginationModel;
import com.kittyp.product.dto.ProductFilterDto;
import com.kittyp.product.dto.ProductSaveDto;
import com.kittyp.product.model.ProductModel;
import com.kittyp.product.service.ProductService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * @author rrohan419@gmail.com
 */
@RestController
@RequestMapping(ApiUrl.BASE_URL)
@RequiredArgsConstructor
public class ProductController {

	private final ApiResponse<?> responseBuilder;
	private final ProductService productService;

	@PostMapping(ApiUrl.PRODUCT_BASE_URL)
	@PreAuthorize(KeyConstant.IS_ROLE_ADMIN)
	public ResponseEntity<SuccessResponse<ProductModel>> addProduct(@Valid @RequestBody ProductSaveDto productSaveDto) {
		ProductModel response = productService.saveProduct(productSaveDto);

		return responseBuilder.buildSuccessResponse(response, ResponseMessage.SUCCESS, HttpStatus.OK);
	}

	@GetMapping(ApiUrl.PRODUCT_BY_UUID)
	public ResponseEntity<SuccessResponse<ProductModel>> productByUuid(@PathVariable String uuid) {
		ProductModel response = productService.productByUuid(uuid);

		return responseBuilder.buildSuccessResponse(response, ResponseMessage.SUCCESS, HttpStatus.OK);
	}
	
	@PreAuthorize(KeyConstant.IS_ROLE_ADMIN)
	@GetMapping(ApiUrl.PRODUCT_COUNT)
	public ResponseEntity<SuccessResponse<Integer>> productCount(@RequestParam(required = false) Boolean isActive) {
		Integer response = productService.productCount(isActive);

		return responseBuilder.buildSuccessResponse(response, ResponseMessage.SUCCESS, HttpStatus.OK);
	}

	@PostMapping(ApiUrl.ALL_PRODUCT)
    public ResponseEntity<SuccessResponse<PaginationModel<ProductModel>>> getAllProducts(@RequestParam(defaultValue = KeyConstant.PAGE_NUMBER) int page,
			@RequestParam(defaultValue = KeyConstant.PAGE_SIZE) int size, @Valid @RequestBody  ProductFilterDto productFilterDto) {
		PaginationModel<ProductModel> response = productService.productsByFilter(productFilterDto, page, size);
		
		return responseBuilder.buildSuccessResponse(response, ResponseMessage.SUCCESS, HttpStatus.OK);
	}

}
