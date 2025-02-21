package com.robar.payment;

import com.robar.payment.controller.PaymentController;
import com.robar.payment.service.VerifonePaymentService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ConfigurationPropertiesScan("com.robar.payment.config")
@ComponentScan(basePackages = {
    "com.robar.payment",
    "com.robar.payment.controller",
    "com.robar.payment.service",
    "com.robar.payment.config"
})
public class RobarPaymentServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(RobarPaymentServiceApplication.class, args);
    }
    
    // Explicitly register the controller as a fallback
    @Bean
    public PaymentController paymentController(VerifonePaymentService verifonePaymentService) {
        return new PaymentController(verifonePaymentService);
    }
}