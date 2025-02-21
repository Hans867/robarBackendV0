package com.robar.payment.service;

import com.robar.payment.model.PaymentStatus;
import lombok.Getter;

@Getter
public class PaymentStatusEvent {
    private final String paymentId;
    private final PaymentStatus status;
    private final String message;

    public PaymentStatusEvent(String paymentId, PaymentStatus status, String message) {
        this.paymentId = paymentId;
        this.status = status;
        this.message = message;
    }
}