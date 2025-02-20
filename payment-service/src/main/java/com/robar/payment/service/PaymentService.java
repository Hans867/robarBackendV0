package com.robar.payment.service;

import com.robar.payment.model.PaymentRequest;
import com.robar.payment.model.PaymentResponse;

public interface PaymentService {
    PaymentResponse initiatePayment(PaymentRequest request);
    PaymentResponse getPaymentStatus(String transactionId);
}