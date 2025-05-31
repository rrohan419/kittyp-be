package com.kittyp.cart.model;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CartItemResponse {
    private String productUuid;
    private String productName;
    private BigDecimal price;
    private int quantity;
    private BigDecimal totalPrice;
}

