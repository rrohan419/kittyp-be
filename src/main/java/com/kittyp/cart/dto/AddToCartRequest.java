package com.kittyp.cart.dto;

import lombok.Data;

@Data
public class AddToCartRequest {
    private String productUuid;
    private int quantity;
}

