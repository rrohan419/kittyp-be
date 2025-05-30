package com.kittyp.order.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PaymentDetailsDto {
    @NotBlank(message = "Payment method is required")
    private String paymentMethod;
    
    @NotBlank(message = "Payment gateway order ID is required")
    private String paymentGatewayOrderId;
    
    private String paymentGatewaySignature;
    
    // Additional fields specific to your payment gateway
    private String currency;
    private String email;
    private String contact;
} 