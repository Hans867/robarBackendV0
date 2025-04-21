package com.robar.payment.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:.env")
public class DotenvConfig {
    // No implementation needed - this class just enables .env loading
}

