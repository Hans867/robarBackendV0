package com.robar.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
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
}