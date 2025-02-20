package com.robar.payment.config;

import org.springframework.context.annotation.Configuration;
import lombok.Getter;

@Configuration
@Getter
public class VerifoneConfig {
    private final VerifoneProperties verifoneProperties;

    // Constructor injection doesn't need @Autowired
    public VerifoneConfig(VerifoneProperties verifoneProperties) {
        this.verifoneProperties = verifoneProperties;
    }

    public String getTerminalIp() {
        return verifoneProperties.getIp();
    }

    public String getConnectionType() {
        return verifoneProperties.getConnectionType();
    }
} 
