package com.robar.payment.controller;

import com.robar.payment.model.*;
import com.robar.payment.service.VerifonePaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final VerifonePaymentService paymentService;

    @PostMapping("/initialize")
    public ResponseEntity<String> initializeTerminal() {
        paymentService.login();
        paymentService.startSession();
        return ResponseEntity.ok("Terminal initialized and ready");
    }

    @PostMapping("/process")
    public ResponseEntity<PaymentResponse> processPayment(@RequestBody PaymentRequest request) {
        paymentService.processPayment(request.getAmount());
        return ResponseEntity.ok(PaymentResponse.builder()
            .status(PaymentStatus.PROCESSING)
            .message("Payment processing started")
            .build());
    }
}