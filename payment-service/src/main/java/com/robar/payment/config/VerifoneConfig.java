package com.robar.payment.config;

import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import lombok.Getter;

import org.slf4j.Logger;

@Configuration
@Getter
public class VerifoneConfig {
    private final VerifoneProperties verifoneProperties;

    private static final Logger log = LoggerFactory.getLogger(VerifoneConfig.class);

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

    @PostConstruct
    public void init() {
        log.info("VERIFONE CONFIG LOADED WITH: IP={}, ConnectionType={}", 
             verifoneProperties.getIp(), verifoneProperties.getConnectionType());
    }


    
} 
