package com.robar.payment.service;

import com.robar.payment.config.VerifoneConfig;
import com.robar.payment.model.PaymentRequest;
import com.robar.payment.model.PaymentResponse;
import com.robar.payment.model.PaymentStatus;
import com.verifone.payment_sdk.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class VerifonePaymentService implements PaymentService {
    private PaymentSdk paymentSdk;
    private final PaymentEventListener eventListener;
    private final VerifoneConfig verifoneConfig;

    public VerifonePaymentService(PaymentEventListener eventListener, VerifoneConfig verifoneConfig) {
        this.eventListener = eventListener;
        this.verifoneConfig = verifoneConfig;
        initializeTerminal();
    }

    private void initializeTerminal() {
        paymentSdk = PaymentSdk.create();
        Map<String, String> config = new HashMap<>();
        config.put("DEVICE_CONNECTION_TYPE_KEY", verifoneConfig.getConnectionType());
        config.put("DEVICE_ADDRESS_KEY", verifoneConfig.getTerminalIp());
        config.put("DEVICE_SERIAL_NUMBER_KEY", "ANY");

        try {
            paymentSdk.initialize(eventListener);
            log.info("Terminal initialization successful");
        } catch (Exception e) {
            log.error("Terminal initialization failed", e);
            throw new RuntimeException("Terminal initialization failed", e);
        }
    }

    @Override
    public PaymentResponse initiatePayment(PaymentRequest request) {
        try {
            // Check if we need to initialize the terminal first
            login();
            startSession();
            
            // Process the payment
            processPayment(request.getAmount());
            
            // Return initial response
            return PaymentResponse.builder()
                    .status(PaymentStatus.PROCESSING)
                    .message("Payment processing started")
                    .build();
        } catch (Exception e) {
            log.error("Payment initiation failed", e);
            return PaymentResponse.builder()
                    .status(PaymentStatus.FAILED)
                    .message("Payment initiation failed: " + e.getMessage())
                    .build();
        }
    }
    
    @Override
    public PaymentResponse getPaymentStatus(String transactionId) {
        // In a real implementation, we would query the status
        // For now, we'll rely on the event listener to update status
        return PaymentResponse.builder()
                .transactionId(transactionId)
                .status(PaymentStatus.PROCESSING)
                .message("Payment status being processed")
                .build();
    }

    public void login() {
        try {
            LoginCredentials credentials = LoginCredentials.createWith2("username", null, null, null);
            Status result = paymentSdk.getTransactionManager().loginWithCredentials(credentials);
            
            if (result.getStatus() != StatusCode.SUCCESS) {
                throw new RuntimeException("Login failed: " + result.getMessage());
            }
            log.info("Login successful");
        } catch (Exception e) {
            log.error("Login failed", e);
            throw new RuntimeException("Login failed", e);
        }
    }

    public void startSession() {
        try {
            Transaction transaction = Transaction.create();
            transaction.setCurrency("DKK");
            
            // Use startSession2 if that's what's available in your SDK version
            boolean success = paymentSdk.getTransactionManager().startSession2(transaction);
            
            if (!success) {
                throw new RuntimeException("Failed to start session");
            }
            log.info("Session started successfully");
        } catch (Exception e) {
            log.error("Failed to start session", e);
            throw new RuntimeException("Session start failed", e);
        }
    }

    public void processPayment(BigDecimal amount) {
        try {
            Payment payment = Payment.create();
            
            // Set up amount totals
            AmountTotals amountTotals = AmountTotals.create(true);
            
            // Convert BigDecimal to Verifone Decimal format
            Decimal paymentAmount = new Decimal(amount.doubleValue());
            
            // Configure the payment amounts
            amountTotals.setTotal(paymentAmount);
            payment.setRequestedAmounts(amountTotals);
            
            // Set currency from the configuration if needed
            payment.setCurrency("DKK"); // Using DKK as seen in your PaymentRequest model
            
            // Start the payment process
            Status result = paymentSdk.getTransactionManager().startPayment(payment);
            if (result.getStatus() != StatusCode.SUCCESS) {
                throw new RuntimeException("Payment processing failed: " + result.getMessage());
            }
            
            log.info("Payment processing initiated for amount: {}", amount);
        } catch (Exception e) {
            log.error("Payment processing failed", e);
            throw new RuntimeException("Payment processing failed", e);
        }
    }

    public void endSession() {
        try {
            boolean success = paymentSdk.getTransactionManager().endSession();
            if (!success) {
                throw new RuntimeException("Failed to end session");
            }
            log.info("Session ended successfully");
        } catch (Exception e) {
            log.error("Failed to end session", e);
            throw new RuntimeException("Session end failed", e);
        }
    }

    public void tearDown() {
        if (paymentSdk != null) {
            paymentSdk.tearDown();
            log.info("Payment SDK torn down successfully");
        }
    }
}