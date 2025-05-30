package com.kittyp.cart.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kittyp.cart.dto.CartCheckoutRequest;
import com.kittyp.cart.dto.CartItemRequest;
import com.kittyp.cart.model.CartResponse;
import com.kittyp.cart.service.CartService;
import com.kittyp.common.constants.ApiUrl;
import com.kittyp.common.constants.KeyConstant;
import com.kittyp.common.constants.ResponseMessage;
import com.kittyp.common.dto.ApiResponse;
import com.kittyp.common.dto.SuccessResponse;
import com.kittyp.order.model.OrderModel;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(ApiUrl.BASE_URL)
@RequiredArgsConstructor
public class CartController {
    private final CartService cartService;
    private final ApiResponse responseBuilder;

    @GetMapping(ApiUrl.GET_CART_BY_USER)
    // @PreAuthorize(KeyConstant.IS_AUTHENTICATED)
    public ResponseEntity<SuccessResponse<CartResponse>> getCart(@PathVariable String userUuid) {
        CartResponse response = cartService.getCartByUser(userUuid);
        return responseBuilder.buildSuccessResponse(response, ResponseMessage.SUCCESS, HttpStatus.OK);
    }

    @PostMapping(ApiUrl.ADD_TO_CART)
    // @PreAuthorize(KeyConstant.IS_AUTHENTICATED)
    public ResponseEntity<SuccessResponse<CartResponse>> addToCart(
            @PathVariable String userUuid,
            @Valid @RequestBody CartItemRequest request) {
        CartResponse response = cartService.addToCart(userUuid, request);
        return responseBuilder.buildSuccessResponse(response, ResponseMessage.SUCCESS, HttpStatus.OK);
    }

    @PutMapping(ApiUrl.UPDATE_CART_ITEM)
    // @PreAuthorize(KeyConstant.IS_AUTHENTICATED)
    public ResponseEntity<SuccessResponse<CartResponse>> updateCartItem(
            @PathVariable String userUuid,
            @Valid @RequestBody CartItemRequest request) {
        CartResponse response = cartService.updateCartItem(userUuid, request);
        return responseBuilder.buildSuccessResponse(response, ResponseMessage.SUCCESS, HttpStatus.OK);
    }

    @DeleteMapping(ApiUrl.REMOVE_FROM_CART)
    // @PreAuthorize(KeyConstant.IS_AUTHENTICATED)
    public ResponseEntity<SuccessResponse<CartResponse>> removeFromCart(
            @PathVariable String userUuid,
            @PathVariable String productUuid) {
        CartResponse response = cartService.removeFromCart(userUuid, productUuid);
        return responseBuilder.buildSuccessResponse(response, ResponseMessage.SUCCESS, HttpStatus.OK);
    }

    @DeleteMapping(ApiUrl.CLEAR_CART)
    // @PreAuthorize(KeyConstant.IS_AUTHENTICATED)
    public ResponseEntity<SuccessResponse<Void>> clearCart(@PathVariable String userUuid) {
        cartService.clearCart(userUuid);
        return responseBuilder.buildSuccessResponse(null, ResponseMessage.SUCCESS, HttpStatus.OK);
    }

   
}
