package com.kittyp.cart.model;

import java.math.BigDecimal;
import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CartResponse {
    private String uuid;
    private List<CartItemResponse> items;
    private BigDecimal totalAmount;
}

