package com.kittyp.email.service;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.kittyp.email.model.PaymentSuccessEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private final ZeptoMailService zeptoMailService;

    @Async
    @EventListener
    public void handlePaymentSuccess(PaymentSuccessEvent event) {
        zeptoMailService.sendOrderConfirmationEmail(event.getEmail(), event.getOrderNumber());
    }
}
