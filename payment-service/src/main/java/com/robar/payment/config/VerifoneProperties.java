package com.robar.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import lombok.Data;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "verifone.terminal")
public class VerifoneProperties {
    private String ip;
    private String connectionType;
} 