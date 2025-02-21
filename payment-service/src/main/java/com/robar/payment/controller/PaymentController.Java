package com.robar.payment.controller;

import com.robar.payment.model.PaymentRequest;
import com.robar.payment.model.PaymentResponse;
import com.robar.payment.model.PaymentStatus;
import com.robar.payment.service.VerifonePaymentService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.PostConstruct;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {
    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);
    
    private final VerifonePaymentService paymentService;
    
    @PostConstruct
    public void init() {
        log.info("PaymentController initialized - endpoints should be available");
    }

    @PostMapping("/initialize")
    public ResponseEntity<String> initializeTerminal() {
        log.info("Initialize terminal endpoint called");
        paymentService.login();
        paymentService.startSession();
        return ResponseEntity.ok("Terminal initialized and ready");
    }

    @PostMapping("/process")
    public ResponseEntity<PaymentResponse> processPayment(@RequestBody PaymentRequest request) {
        log.info("Process payment endpoint called with amount: {}", request.getAmount());
        paymentService.processPayment(request.getAmount());
        return ResponseEntity.ok(PaymentResponse.builder()
                .status(PaymentStatus.PROCESSING)
                .message("Payment processing started")
                .build());
    }
    
    // Simple test endpoint that doesn't require a request body
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        log.info("Test endpoint called");
        return ResponseEntity.ok("Controller is working");
    }
}