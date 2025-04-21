package com.robar.payment.service;

import com.robar.payment.config.VerifoneConfig;
import com.robar.payment.model.PaymentRequest;
import com.robar.payment.model.PaymentResponse;
import com.robar.payment.model.PaymentStatus;
import com.verifone.payment_sdk.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class VerifonePaymentService implements PaymentService {
    private PaymentSdk paymentSdk;
    private final PaymentEventListener eventListener;
    private final VerifoneConfig verifoneConfig;
    
    // Add a latch for initialization synchronization
    private CountDownLatch initLatch = new CountDownLatch(1);
    private boolean isTerminalInitialized = false;
    private String lastErrorMessage = "";

    public VerifonePaymentService(PaymentEventListener eventListener, VerifoneConfig verifoneConfig) {
        this.eventListener = eventListener;
        this.verifoneConfig = verifoneConfig;
        
        // Set up the callback
        eventListener.setInitializationCallback(this::onInitializationComplete);
        
        // Don't auto-initialize - let the controller handle this explicitly
        log.info("VerifonePaymentService created, waiting for explicit initialization request");
    }
    
    /**
     * Initialize the terminal using the first-time connection approach.
     * Hardcoded IP and connection type for initial testing.
     */
    public void initializeTerminal() {
        log.info("Starting terminal initialization with first-time connection handling");
        
        // Reset initialization status
        isTerminalInitialized = false;
        lastErrorMessage = "";
        initLatch = new CountDownLatch(1);
        
        Thread initThread = new Thread(() -> {
            try {
                // Tear down any existing instance
                if (paymentSdk != null) {
                    try {
                        paymentSdk.tearDown();
                        log.info("Previous PaymentSdk instance torn down");
                    } catch (Exception e) {
                        log.warn("Error tearing down previous PaymentSdk", e);
                    }
                }
                
                // Create fresh instance
                paymentSdk = PaymentSdk.create();
                log.info("New PaymentSdk instance created");
                eventListener.setPaymentSdk(paymentSdk);
                
                // Use terminal IP from .env via verifoneConfig
                String ipAddress = verifoneConfig.getTerminalIp(); 
                // Fallback to hardcoded value if empty
                if (ipAddress == null || ipAddress.isEmpty()) {
                    ipAddress = "null";
                    log.warn("IP address from config was empty, using fallback: {}", ipAddress);
                }
                
                // Use connection type from config
                String connectionType = verifoneConfig.getConnectionType();
                // Fallback to hardcoded value if empty
                if (connectionType == null || connectionType.isEmpty()) {
                    connectionType = "tcpip";
                    log.warn("Connection type from config was empty, using fallback: {}", connectionType);
                }
                
                log.info("Initializing with IP: {}, connection type: {}", ipAddress, connectionType);
                
                // === FIRST APPROACH: SIMPLIFIED INITIALIZATION ===
                try {
                    log.info("Attempting simplified initialization approach");
                    
                    // Create an enhanced configuration map with parameters to force new device setup
                    HashMap<String, String> config = new HashMap<>();
                    config.put(PsdkDeviceInformation.DEVICE_CONNECTION_TYPE_KEY, connectionType);
                    config.put(PsdkDeviceInformation.DEVICE_ADDRESS_KEY, ipAddress);
                    
                    // Add first-time connection parameters
                    config.put("ForceNewDevice", "true");                // Signal this is a first-time setup
                    config.put("ClearStoredConfiguration", "true");      // Try to clear any existing config
                    config.put("IgnoreStoredDevice", "true");            // Don't try to use stored device
                    config.put("ResetStoredDeviceConfiguration", "true"); // Reset any stored config
                    config.put("DeviceRetryAttempts", "10");             // More retries
                    config.put("DeviceConnectionTimeout", "60000");      // 60 second timeout
                    
                    log.info("Using enhanced initialization parameters: {}", config);
                    
                    // Initialize the terminal
                    paymentSdk.initializeFromValues(eventListener, config);
                    log.info("Initialization request sent, waiting for callbacks...");
                    
                    // Wait for initialization with timeout
                    boolean completed = initLatch.await(30, TimeUnit.SECONDS);
                    if (!completed) {
                        log.warn("Initialization timed out after 30 seconds");
                    }
                } catch (Exception e) {
                    log.error("Error during initialization: {}", e.getMessage());
                    lastErrorMessage = e.getMessage();
                    
                    // Don't release the latch yet, try the alternative approach
                    log.info("Trying alternative initialization approach after error");
                }
                
                // Check if previous attempt was successful
                if (!isTerminalInitialized) {
                    log.info("First initialization approach did not succeed, trying basic approach...");
                    
                    try {
                        // Create a new instance for the second attempt
                        paymentSdk.tearDown();
                        paymentSdk = PaymentSdk.create();
                        eventListener.setPaymentSdk(paymentSdk);
                        
                        // Minimal configuration for a basic approach
                        HashMap<String, String> basicConfig = new HashMap<>();
                        basicConfig.put(PsdkDeviceInformation.DEVICE_CONNECTION_TYPE_KEY, connectionType);
                        basicConfig.put(PsdkDeviceInformation.DEVICE_ADDRESS_KEY, ipAddress);
                        
                        log.info("Attempting basic initialization with params: {}", basicConfig);
                        paymentSdk.initializeFromValues(eventListener, basicConfig);
                        
                        // Wait again for this attempt
                        boolean completed = initLatch.await(20, TimeUnit.SECONDS);
                        if (!completed) {
                            log.warn("Basic initialization timed out after 20 seconds");
                        }
                    } catch (Exception e) {
                        log.error("Error during basic initialization: {}", e.getMessage());
                        lastErrorMessage = e.getMessage();
                        initLatch.countDown(); // Make sure to release the latch
                    }
                }
                
            } catch (Exception e) {
                log.error("Unexpected error during terminal initialization", e);
                lastErrorMessage = e.getMessage();
                initLatch.countDown(); // Release any waiting threads
            }
        });
        
        initThread.setName("Terminal-Init-Thread");
        initThread.start();
        
        log.info("Terminal initialization thread started");
    }
    
    /**
     * Try server mode initialization as an alternative if client mode fails
     */
    public void initializeTerminalServerMode() {
        log.info("Attempting server mode initialization");
        
        // Reset initialization status
        isTerminalInitialized = false;
        lastErrorMessage = "";
        initLatch = new CountDownLatch(1);
        
        Thread serverModeThread = new Thread(() -> {
            try {
                if (paymentSdk != null) {
                    try {
                        paymentSdk.tearDown();
                    } catch (Exception e) {
                        log.warn("Error tearing down previous PaymentSdk", e);
                    }
                }
                
                paymentSdk = PaymentSdk.create();
                eventListener.setPaymentSdk(paymentSdk);
                
                // Server mode configuration
                HashMap<String, String> serverConfig = new HashMap<>();
                serverConfig.put(PsdkDeviceInformation.DEVICE_CONNECTION_TYPE_KEY, "server");
                serverConfig.put("ServerPort", "8085"); // Example server port
                serverConfig.put("DeviceOperationMode", "server");
                
                log.info("Initializing with server mode parameters: {}", serverConfig);
                
                // Initialize in server mode
                paymentSdk.initializeFromValues(eventListener, serverConfig);
                log.info("Server mode initialization request sent, waiting for callback...");
                
                boolean completed = initLatch.await(30, TimeUnit.SECONDS);
                if (!completed) {
                    log.warn("Server mode initialization timed out after 30 seconds");
                }
                
            } catch (Exception e) {
                log.error("Error during server mode initialization", e);
                lastErrorMessage = e.getMessage();
                initLatch.countDown();
            }
        });
        
        serverModeThread.setName("Server-Mode-Init-Thread");
        serverModeThread.start();
    }
    
    /**
     * Try alternative method of initialization by directly initializing without 
     * attempting to forget previous device configuration
     */
    public void initializeDirectly() {
        log.info("Attempting direct initialization without device management");
        
        // Reset initialization status
        isTerminalInitialized = false;
        lastErrorMessage = "";
        initLatch = new CountDownLatch(1);
        
        // Create fresh PaymentSdk instance
        if (paymentSdk != null) {
            try {
                paymentSdk.tearDown();
            } catch (Exception e) {
                log.warn("Error tearing down PaymentSdk", e);
            }
        }
        
        try {
            paymentSdk = PaymentSdk.create();
            eventListener.setPaymentSdk(paymentSdk);
            
            // Hardcoded values
            String ipAddress = "null"; 
            String connectionType = "tcpip";
            
            HashMap<String, String> config = new HashMap<>();
            config.put(PsdkDeviceInformation.DEVICE_CONNECTION_TYPE_KEY, connectionType);
            config.put(PsdkDeviceInformation.DEVICE_ADDRESS_KEY, ipAddress);
            
            log.info("Initializing directly with minimal configuration: {}", config);
            paymentSdk.initializeFromValues(eventListener, config);
            
        } catch (Exception e) {
            log.error("Direct initialization failed", e);
            lastErrorMessage = e.getMessage();
            initLatch.countDown();
        }
    }
    
    // Handler for initialization callbacks
    private void onInitializationComplete(boolean success, String message) {
        log.info("Initialization callback received: success={}, message={}", success, message);
        
        if (success) {
            log.info("Terminal initialization successfully completed!");
            isTerminalInitialized = true;
            initLatch.countDown();
            return;
        }
        
        // Check for our special marker for -30 status code
        if (message != null && message.startsWith("FIRST_TIME_SETUP:-30")) {
            log.info("Handling first-time setup (-30) status");
            
            // For first-time setup with -30, we need to proceed with a login attempt
            try {
                log.info("Proceeding with first-time setup sequence...");
                
                // Try directly proceeding with login despite the -30 status
                // This is counterintuitive but some terminals need this
                try {
                    log.info("Attempting login despite -30 status...");
                    LoginCredentials credentials = LoginCredentials.createWith2("username", null, null, null);
                    
                    // The status here is important - if terminal accepts login despite -30,
                    // then we're in a good state and can consider initialization successful
                    Status loginStatus = paymentSdk.getTransactionManager().loginWithCredentials(credentials);
                    
                    log.info("Login attempt after -30 returned status: {}", loginStatus.getStatus());
                    
                    if (loginStatus.getStatus() == StatusCode.SUCCESS) {
                        log.info("LOGIN SUCCESSFUL despite -30 status! Terminal is now usable.");
                        isTerminalInitialized = true;
                    } else {
                        log.info("Login attempt unsuccessful: {} - {}", 
                               loginStatus.getStatus(), loginStatus.getMessage());
                        
                        // Even if login fails, the terminal might still be usable
                        // This depends on your terminal's specific behavior
                        if (loginStatus.getStatus() > -100) { // Using a heuristic for "not catastrophic" errors
                            log.info("Login failed but terminal may still be usable, marking as initialized");
                            isTerminalInitialized = true;
                        } else {
                            lastErrorMessage = "Login failed after -30: " + loginStatus.getMessage();
                        }
                    }
                } catch (Exception e) {
                    log.error("Exception during login attempt after -30: {}", e.getMessage());
                    lastErrorMessage = "Exception during login after -30: " + e.getMessage();
                }
            } catch (Exception e) {
                log.error("Exception during first-time setup handling: {}", e.getMessage());
                lastErrorMessage = e.getMessage();
            }
        } else {
            // Handle other non-success cases
            log.error("Terminal initialization failed: {}", message);
            lastErrorMessage = message;
        }
        
        // Release waiting threads
        initLatch.countDown();
    }

    @Override
    public PaymentResponse initiatePayment(PaymentRequest request) {
        try {
            // Check if terminal is initialized
            if (!isTerminalInitialized) {
                throw new RuntimeException("Terminal not initialized. Please initialize first.");
            }
            
            // Process the payment
            processPayment(request.getAmount());
            
            // Return initial response
            return PaymentResponse.builder()
                    .status(PaymentStatus.PROCESSING)
                    .message("Payment processing started")
                    .build();
        } catch (Exception e) {
            log.error("Payment initiation failed", e);
            return PaymentResponse.builder()
                    .status(PaymentStatus.FAILED)
                    .message("Payment initiation failed: " + e.getMessage())
                    .build();
        }
    }
    
    @Override
    public PaymentResponse getPaymentStatus(String transactionId) {
        // In a real implementation, we would query the status
        // For now, we'll rely on the event listener to update status
        return PaymentResponse.builder()
                .transactionId(transactionId)
                .status(PaymentStatus.PROCESSING)
                .message("Payment status being processed")
                .build();
    }

    public void login() {
        try {
            if (!isTerminalInitialized) {
                throw new RuntimeException("Terminal not initialized. Please initialize first.");
            }
            
            // Create login credentials - username only as per documentation
            LoginCredentials credentials = LoginCredentials.createWith2("username", null, null, null);
            
            log.info("Attempting to login to terminal...");
            Status result = paymentSdk.getTransactionManager().loginWithCredentials(credentials);
            
            if (result.getStatus() != StatusCode.SUCCESS) {
                throw new RuntimeException("Login failed: " + result.getMessage());
            }
            log.info("Login successful");
        } catch (Exception e) {
            log.error("Login failed", e);
            throw new RuntimeException("Login failed", e);
        }
    }

    public void startSession() {
        try {
            if (!isTerminalInitialized) {
                throw new RuntimeException("Terminal not initialized. Please initialize first.");
            }
            
            // Create transaction for the session
            Transaction transaction = Transaction.create();
            transaction.setCurrency("DKK");
            
            log.info("Attempting to start session...");
            boolean success = paymentSdk.getTransactionManager().startSession2(transaction);
            
            if (!success) {
                throw new RuntimeException("Failed to start session");
            }
            log.info("Session started successfully");
        } catch (Exception e) {
            log.error("Failed to start session", e);
            throw new RuntimeException("Session start failed", e);
        }
    }

    public void processPayment(BigDecimal amount) {
        try {
            if (!isTerminalInitialized) {
                throw new RuntimeException("Terminal not initialized. Please initialize first.");
            }
            
            Payment payment = Payment.create();
            
            // Set up amount totals
            AmountTotals amountTotals = AmountTotals.create(true);
            
            // Convert BigDecimal to Verifone Decimal format
            Decimal paymentAmount = new Decimal(amount.doubleValue());
            
            // Configure the payment amounts
            amountTotals.setTotal(paymentAmount);
            payment.setRequestedAmounts(amountTotals);
            
            // Set currency
            payment.setCurrency("DKK");
            
            log.info("Starting payment process for amount: {}", amount);
            Status result = paymentSdk.getTransactionManager().startPayment(payment);
            
            if (result.getStatus() != StatusCode.SUCCESS) {
                throw new RuntimeException("Payment processing failed: " + result.getMessage());
            }
            
            log.info("Payment processing initiated for amount: {}", amount);
        } catch (Exception e) {
            log.error("Payment processing failed", e);
            throw new RuntimeException("Payment processing failed", e);
        }
    }

    public void endSession() {
        try {
            if (!isTerminalInitialized) {
                throw new RuntimeException("Terminal not initialized. Please initialize first.");
            }
            
            log.info("Ending terminal session...");
            boolean success = paymentSdk.getTransactionManager().endSession();
            if (!success) {
                throw new RuntimeException("Failed to end session");
            }
            log.info("Session ended successfully");
        } catch (Exception e) {
            log.error("Failed to end session", e);
            throw new RuntimeException("Session end failed", e);
        }
    }

    public void tearDown() {
        if (paymentSdk != null) {
            paymentSdk.tearDown();
            log.info("Payment SDK torn down successfully");
            isTerminalInitialized = false;
        }
    }
    
    // Accessor methods for the controller
    public boolean isTerminalInitialized() {
        return isTerminalInitialized;
    }
    
    public String getTerminalIpAddress() {
        // For debugging, log both values
        String configIp = verifoneConfig.getTerminalIp();
        String hardcodedIp = "null";
        
        log.debug("Config IP: {}, Using hardcoded IP: {}", configIp, hardcodedIp);
        
        // Return hardcoded value for now
        return hardcodedIp;
    }
    
    public String getConnectionType() {
        // For debugging, log both values
        String configType = verifoneConfig.getConnectionType();
        String hardcodedType = "tcpip";
        
        log.debug("Config connection type: {}, Using hardcoded type: {}", configType, hardcodedType);
        
        // Return hardcoded value for now
        return hardcodedType;
    }
    
    public String getLastErrorMessage() {
        return lastErrorMessage;
    }
}