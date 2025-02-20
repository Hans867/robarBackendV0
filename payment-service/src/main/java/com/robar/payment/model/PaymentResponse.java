package com.robar.payment.model;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class PaymentResponse {
    private String transactionId;
    private PaymentStatus status;
    private String message;
    private String receiptUrl;
} 