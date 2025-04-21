package com.robar.payment.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
public class TestControllerInPackage {
    private static final Logger log = LoggerFactory.getLogger(TestControllerInPackage.class);
    
    public TestControllerInPackage() {
        log.info("!!! TestControllerInPackage constructed !!!");
    }
    
    @GetMapping("/test-in-controller-package")
    public String test() {
        log.info("!!! test-in-controller-package endpoint called !!!");
        return "Test endpoint in controller package is working!";
    }
}