package com.robar.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;


// Remove any other ComponentScan annotations in the class if they exist
@SpringBootApplication
// Be extremely explicit with component scanning
@ComponentScan(basePackages = {
    "com.robar.payment",
    "com.robar.payment.controller",
    "com.robar.payment.service",
    "com.robar.payment.config",
    "com.robar.payment.model"
})
public class RobarPaymentServiceApplication {
    
    public static void main(String[] args) {
        // This helps ensure environment variables are loaded first
        System.setProperty("spring.config.import", "optional:classpath:.env[.properties]");
        SpringApplication.run(RobarPaymentServiceApplication.class, args);
    }

    
}

