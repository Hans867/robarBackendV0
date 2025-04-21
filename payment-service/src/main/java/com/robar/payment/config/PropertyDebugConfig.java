package com.robar.payment.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class PropertyDebugConfig {
    private static final Logger log = LoggerFactory.getLogger(PropertyDebugConfig.class);
    
    @Autowired
    private VerifoneProperties verifoneProperties;
    
    @Bean
    public CommandLineRunner logPropertiesOnStartup(Environment env) {
        return args -> {
            log.info("=========== ENVIRONMENT PROPERTIES CHECKING ===========");
            log.info("verifone.terminal.ip from Environment: {}", env.getProperty("verifone.terminal.ip"));
            log.info("verifone.terminal.connection-type from Environment: {}", env.getProperty("verifone.terminal.connection-type"));
            
            log.info("=========== BOUND PROPERTIES CHECKING ===========");
            log.info("VerifoneProperties.ip: {}", verifoneProperties.getIp());
            log.info("VerifoneProperties.connectionType: {}", verifoneProperties.getConnectionType());
            
            // Check the exact case format to ensure we don't have case sensitivity issues
            log.info("Raw environment property names that contain 'verifone':");
            for (String propertyName : System.getenv().keySet()) {
                if (propertyName.toLowerCase().contains("verifone")) {
                    log.info("Found environment variable: {} = {}", propertyName, System.getenv(propertyName));
                }
            }
        };
    }
}