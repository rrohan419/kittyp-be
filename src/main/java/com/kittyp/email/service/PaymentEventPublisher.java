package com.kittyp.email.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.kittyp.email.model.PaymentSuccessEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PaymentEventPublisher {
    
    private final ApplicationEventPublisher publisher;

    public void publishPaymentSuccess(String orderNumber, String email) {
        publisher.publishEvent(new PaymentSuccessEvent(orderNumber, email));
    }
}
