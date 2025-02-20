package com.robar.payment.service;

import com.verifone.payment_sdk.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class VerifonePaymentService {
    private PaymentSdk paymentSdk;
    private final PaymentEventListener eventListener;
    
    public VerifonePaymentService(PaymentEventListener eventListener) {
        this.eventListener = eventListener;
        initializeTerminal();
    }

    private void initializeTerminal() {
        paymentSdk = new PaymentSdk();
        Map<String, String> config = new HashMap<>();
        config.put("DEVICE_CONNECTION_TYPE_KEY", "tcpip");
        config.put("DEVICE_ADDRESS_KEY", "192.168.1.242");
        
        try {
            paymentSdk.InitializeFromValues(eventListener, config);
            log.info("Terminal initialization successful");
        } catch (Exception e) {
            log.error("Terminal initialization failed", e);
            throw new RuntimeException("Terminal initialization failed", e);
        }
    }

    public void login() {
        try {
            LoginCredentials credentials = LoginCredentials.Create();
            credentials.setUserId("username");
            paymentSdk.getTransactionManager().LoginWithCredentials(credentials);
            log.info("Login successful");
        } catch (Exception e) {
            log.error("Login failed", e);
            throw new RuntimeException("Login failed", e);
        }
    }

    public void startSession() {
        try {
            Transaction transaction = Transaction.Create();
            paymentSdk.getTransactionManager().StartSession(transaction);
            log.info("Session started successfully");
        } catch (Exception e) {
            log.error("Failed to start session", e);
            throw new RuntimeException("Session start failed", e);
        }
    }

    public void processPayment(BigDecimal amount) {
        try {
            Payment payment = Payment.Create();
            // Converting amount to Verifone's decimal format
            VerifoneSdk.Decimal paymentAmount = new VerifoneSdk.Decimal(amount.doubleValue());
            payment.getRequestedAmounts().AddAmounts(paymentAmount, null, null, null, null, null, paymentAmount);
            
            paymentSdk.getTransactionManager().StartPayment(payment);
            log.info("Payment processing initiated for amount: {}", amount);
        } catch (Exception e) {
            log.error("Payment processing failed", e);
            throw new RuntimeException("Payment processing failed", e);
        }
    }
}
