package com.robar.payment.model;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PaymentRequest {
    private BigDecimal amount;
    private String currency = "DKK";
    private PaymentType paymentType = PaymentType.CARD; // Default to card payment
}   
