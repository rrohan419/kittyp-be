package com.kittyp.cart.dto;

import com.kittyp.order.emus.ShippingTypes;
import com.kittyp.order.entity.AddressInfo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CartCheckoutRequest {
    @NotNull(message = "Billing address is required")
    private AddressInfo billingAddress;
    
    @NotNull(message = "Shipping address is required")
    private AddressInfo shippingAddress;
        
    private String notes;
    
    // Shipping preferences
    private ShippingTypes shippingMethod; // e.g., "STANDARD", "EXPRESS"
    
    // Additional customer info
    private String email;
    private String phone;
} 