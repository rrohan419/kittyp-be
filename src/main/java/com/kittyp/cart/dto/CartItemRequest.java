package com.kittyp.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CartItemRequest {
    @NotBlank(message = "Product UUID is required")
    private String productUuid;
    
    @Min(value = 1, message = "Quantity must be at least 1")
    private int quantity;
} 