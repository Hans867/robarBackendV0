package com.robar.payment.config;

import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "verifone.terminal")
@ToString
public class VerifoneProperties {
    private String ip;
    private String connectionType;
    
    // Default constructor required for property binding
    public VerifoneProperties() {
    }
    
    // Getters and setters are provided by Lombok @Data
}