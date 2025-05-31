package com.kittyp.payment.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentResponse {
    private String status;
    private String orderId;
    private String paymentId;
    private String signature;
    private String errorMessage;
    private String errorCode;
} 