package com.robar.payment.controller;

import com.robar.payment.model.PaymentRequest;
import com.robar.payment.model.PaymentResponse;
import com.robar.payment.model.PaymentStatus;
import com.robar.payment.service.PaymentEventListener;
import com.robar.payment.service.VerifonePaymentService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/payments")
public class PaymentController {
    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);
    
    // Remove @RequiredArgsConstructor and use explicit constructor injection
    private final VerifonePaymentService paymentService;
    private final PaymentEventListener eventListener;
    
    // Explicit constructor instead of Lombok's @RequiredArgsConstructor
    public PaymentController(VerifonePaymentService paymentService, PaymentEventListener eventListener) {
        this.paymentService = paymentService;
        this.eventListener = eventListener;
        log.info("PaymentController constructed with dependencies");
    }

    @PostConstruct
    public void init() {
        log.info("PaymentController initialized - endpoints available at /payments/*");
    }

    @GetMapping("/test")
    public String test() {
        log.info("PaymentController test endpoint called");
        return "PaymentController is working!";
    }

    @PostMapping("/initialize")
    public ResponseEntity<Map<String, String>> initializeTerminal() {
        log.info("Initialize terminal endpoint called");
        Map<String, String> response = new HashMap<>();
        
        try {
            // Request initialization using the "Connecting to a New Device" implementation
            paymentService.initializeTerminal();
            
            response.put("status", "requested");
            response.put("message", "Terminal initialization requested - check logs for status");
            response.put("ip", paymentService.getTerminalIpAddress());
            response.put("connectionType", paymentService.getConnectionType());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error initializing terminal", e);
            
            response.put("status", "error");
            response.put("message", "Terminal initialization error: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    @PostMapping("/initialize-server-mode")
    public ResponseEntity<Map<String, String>> initializeTerminalServerMode() {
        log.info("Initialize terminal in server mode endpoint called");
        Map<String, String> response = new HashMap<>();
        
        try {
            // Try server mode initialization as an alternative
            paymentService.initializeTerminalServerMode();
            
            response.put("status", "requested");
            response.put("message", "Terminal server mode initialization requested - check logs for status");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error initializing terminal in server mode", e);
            
            response.put("status", "error");
            response.put("message", "Terminal server mode initialization error: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    @GetMapping("/terminal-status")
    public ResponseEntity<Map<String, Object>> getTerminalStatus() {
        Map<String, Object> status = new HashMap<>();
        
        boolean isInitialized = paymentService.isTerminalInitialized();
        String ipAddress = paymentService.getTerminalIpAddress();
        String connectionType = paymentService.getConnectionType();
        String lastError = paymentService.getLastErrorMessage();
        
        // Get additional status from the event listener
        int lastStatusCode = eventListener.getLastStatusCode();
        String lastStatusMessage = eventListener.getLastStatusMessage();
        int errorCount = eventListener.getErrorCount();
        
        status.put("initialized", isInitialized);
        status.put("ipAddress", ipAddress);
        status.put("connectionType", connectionType);
        status.put("lastError", lastError);
        status.put("lastStatusCode", lastStatusCode);
        status.put("lastStatusMessage", lastStatusMessage);
        status.put("errorCount", errorCount);
        
        return ResponseEntity.ok(status);
    }
    
    @GetMapping("/network-test")
    public ResponseEntity<Map<String, String>> testTerminalNetwork() {
        String terminalIp = paymentService.getTerminalIpAddress();
        log.info("Testing network connectivity to terminal at {}", terminalIp);
        
        Map<String, String> results = new HashMap<>();
        results.put("targetIp", terminalIp);
        
        // Test ping
        try {
            InetAddress address = InetAddress.getByName(terminalIp);
            boolean reachable = address.isReachable(5000); // 5 second timeout
            
            results.put("ping", reachable ? "success" : "failed");
            results.put("pingDetails", reachable ? 
                    "Terminal is responding to ping" : 
                    "Terminal is not responding to ping");
        } catch (IOException e) {
            results.put("ping", "error");
            results.put("pingDetails", "Error testing ping: " + e.getMessage());
        }
        
        // Test TCP connection
        try {
            // Try common ports that might be open on the terminal
            int[] portsToTry = {80, 443, 8080, 8443, 9100, 9200};
            boolean anyPortOpen = false;
            
            for (int port : portsToTry) {
                try (Socket socket = new Socket()) {
                    socket.connect(new InetSocketAddress(terminalIp, port), 2000); // 2 second timeout per port
                    results.put("tcp_" + port, "open");
                    anyPortOpen = true;
                } catch (IOException e) {
                    results.put("tcp_" + port, "closed");
                }
            }
            
            results.put("tcpConnectivity", anyPortOpen ? "success" : "failed");
            results.put("tcpDetails", anyPortOpen ? 
                    "At least one port is open on the terminal" : 
                    "No ports are open or reachable on the terminal");
            
        } catch (Exception e) {
            results.put("tcpConnectivity", "error");
            results.put("tcpDetails", "Error testing TCP connectivity: " + e.getMessage());
        }
        
        return ResponseEntity.ok(results);
    }
    
    @PostMapping("/process")
    public ResponseEntity<PaymentResponse> processPayment(@RequestBody PaymentRequest request) {
        log.info("Process payment endpoint called with amount: {}", request.getAmount());
        try {
            PaymentResponse response = paymentService.initiatePayment(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Payment processing failed", e);
            return ResponseEntity.internalServerError().body(
                PaymentResponse.builder()
                    .status(PaymentStatus.FAILED)
                    .message("Payment processing failed: " + e.getMessage())
                    .build()
            );
        }
    }
    
    @PostMapping("/login")
    public ResponseEntity<String> login() {
        log.info("Login endpoint called");
        try {
            paymentService.login();
            return ResponseEntity.ok("Login successful");
        } catch (Exception e) {
            log.error("Login failed", e);
            return ResponseEntity.internalServerError().body("Login failed: " + e.getMessage());
        }
    }
    
    @PostMapping("/start-session")
    public ResponseEntity<String> startSession() {
        log.info("Start session endpoint called");
        try {
            paymentService.startSession();
            return ResponseEntity.ok("Session started successfully");
        } catch (Exception e) {
            log.error("Session start failed", e);
            return ResponseEntity.internalServerError().body("Session start failed: " + e.getMessage());
        }
    }
    
    @PostMapping("/end-session")
    public ResponseEntity<String> endSession() {
        log.info("End session endpoint called");
        try {
            paymentService.endSession();
            return ResponseEntity.ok("Session ended successfully");
        } catch (Exception e) {
            log.error("Session end failed", e);
            return ResponseEntity.internalServerError().body("Session end failed: " + e.getMessage());
        }
    }
    
    @PostMapping("/teardown")
    public ResponseEntity<String> tearDown() {
        log.info("Teardown endpoint called");
        try {
            paymentService.tearDown();
            return ResponseEntity.ok("Terminal connection torn down successfully");
        } catch (Exception e) {
            log.error("Teardown failed", e);
            return ResponseEntity.internalServerError().body("Teardown failed: " + e.getMessage());
        }
    }
}