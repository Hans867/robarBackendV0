package com.robar.payment.service;

import com.verifone.payment_sdk.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.HashMap;

@Slf4j
@Component
public class PaymentEventListener extends CommerceListenerAdapter {
    // Interface for initialization callback
    public interface InitializationCallback {
        void onComplete(boolean success, String message);
    }
    
    private final ApplicationEventPublisher eventPublisher;
    private PaymentSdk paymentSdk;
    private InitializationCallback initializationCallback;
    
    // Add tracking for status updates
    private int lastStatusCode = 0;
    private String lastStatusMessage = "";
    private int errorCount = 0;

    public PaymentEventListener(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
        log.info("PaymentEventListener created");
    }

    public void setPaymentSdk(PaymentSdk paymentSdk) {
        this.paymentSdk = paymentSdk;
        log.info("PaymentSdk reference set in listener");
    }
    
    public void setInitializationCallback(InitializationCallback callback) {
        this.initializationCallback = callback;
        log.info("Initialization callback set in listener");
    }

    @Override
    public void handleStatus(Status status) {
        // Save status details
        lastStatusCode = status.getStatus();
        lastStatusMessage = status.getMessage();
        
        // Log every detail about the status
        log.info("Terminal status update: code={}, message={}, type={}", 
                status.getStatus(), status.getMessage(), status.getType());
        
        // Specific handling for various status codes
        switch (status.getStatus()) {
            case StatusCode.SUCCESS:
                log.info("SUCCESS: Terminal operation completed successfully");
                if (initializationCallback != null) {
                    initializationCallback.onComplete(true, "Success");
                }
                break;
                
            case -30: // Configuration required, no device remembered
                // Critical: Treat -30 as informational for first-time setup, not an error
                log.info("INFORMATION - Status -30: Configuration required, no device remembered");
                log.info("This is expected for first-time connections and will be handled as part of setup");
                
                // Pass the status through to the callback WITHOUT treating it as an error
                if (initializationCallback != null) {
                    // Important: We're passing a parameter to indicate this is a -30 code specifically
                    // to signal the service to handle it as a first-time configuration
                    initializationCallback.onComplete(false, "FIRST_TIME_SETUP:-30");
                }
                break;
                
            case -1: // General error
                log.error("GENERAL ERROR: {}", status.getMessage());
                if (initializationCallback != null) {
                    initializationCallback.onComplete(false, status.getMessage());
                }
                errorCount++;
                break;
                
            default:
                if (status.getStatus() < 0) {
                    log.error("Terminal error (code: {}): {}", status.getStatus(), status.getMessage());
                    
                    if (initializationCallback != null) {
                        initializationCallback.onComplete(false, status.getMessage());
                    }
                    errorCount++;
                } else {
                    log.info("Terminal status (code: {}): {}", status.getStatus(), status.getMessage());
                }
                break;
        }
    }

    @Override
    public void handlePaymentCompletedEvent(PaymentCompletedEvent event) {
        if (event.getStatus() == 0) {
            log.info("Payment completed successfully");
            
            // Use your model's PaymentStatus enum
            com.robar.payment.model.PaymentStatus status = com.robar.payment.model.PaymentStatus.COMPLETED;
            
            // Create and publish payment status event
            PaymentStatusEvent statusEvent = new PaymentStatusEvent(
                event.getPayment().getPaymentId(),
                status,
                "Payment successful"
            );
            eventPublisher.publishEvent(statusEvent);
        } else {
            log.error("Payment failed: {}", event.getMessage());
            
            // Use your model's PaymentStatus enum
            com.robar.payment.model.PaymentStatus status = com.robar.payment.model.PaymentStatus.FAILED;
            
            // Create and publish payment status event
            PaymentStatusEvent statusEvent = new PaymentStatusEvent(
                event.getPayment().getPaymentId(),
                status,
                event.getMessage()
            );
            eventPublisher.publishEvent(statusEvent);
        }
    }

    @Override
    public void handleCommerceEvent(CommerceEvent event) {
        log.info("Commerce event received: type={}, status={}, message={}", 
                event.getType(), event.getStatus(), event.getMessage());
        
        // Check for initialization events by string comparison (safer with SDK version differences)
        if (event.getType() != null && 
            (event.getType().toLowerCase().contains("initialized") || 
             event.getType().toLowerCase().contains("init"))) {
            
            if (event.getStatus() == StatusCode.SUCCESS) {
                log.info("Terminal initialization event successful!");
                if (initializationCallback != null) {
                    initializationCallback.onComplete(true, "Initialization event success");
                }
            } else {
                // For initialization events with non-success status
                log.info("Terminal initialization event status: {} - {}", event.getStatus(), event.getMessage());
                
                // Only treat as error if not -30 (which is expected for first-time)
                if (event.getStatus() == -30) {
                    log.info("Received -30 in initialization event (expected for first-time setup)");
                    if (initializationCallback != null) {
                        initializationCallback.onComplete(false, "FIRST_TIME_SETUP:-30");
                    }
                } else {
                    if (initializationCallback != null) {
                        initializationCallback.onComplete(false, event.getMessage());
                    }
                }
            }
        }
        
        // Also handle device management events
        if (event.getType() != null && event.getType().toLowerCase().contains("device")) {
            log.info("Device management event: {} - {}", event.getStatus(), event.getMessage());
        }
    }

    @Override
    public void handleTransactionEvent(TransactionEvent event) {
        log.info("Transaction event: type={}, status={}, message={}", 
                event.getType(), event.getStatus(), event.getMessage());
        
        if (TransactionEvent.LOGIN_COMPLETED.equals(event.getType())) {
            if (event.getStatus() == StatusCode.SUCCESS) {
                log.info("Login successful");
            } else {
                log.error("Login failed: {}", event.getMessage());
            }
        }
        
        if (CommerceEvent.SESSION_STARTED.equals(event.getType())) {
            if (event.getStatus() == StatusCode.SUCCESS) {
                log.info("Session started successfully");
            } else {
                log.error("Session start failed: {}", event.getMessage());
            }
        }
        
        if (CommerceEvent.SESSION_ENDED.equals(event.getType())) {
            log.info("Session ended");
        }
    }

    @Override
    public void handleHostAuthorizationEvent(HostAuthorizationEvent event) {
        if (event.getStatus() == StatusCode.SUCCESS) {
            log.info("Host authorization requested");
            
            HostTransaction hostTransaction = event.getHostTransaction();
            if (hostTransaction != null && hostTransaction.getTotalAmount() != null) {
                try {
                    // Create EMV data for approval
                    HashMap<String, String> emvData = new HashMap<>();
                    emvData.put("8A", "3030"); // Approval code
                    
                    String totalAmount = hostTransaction.getTotalAmount();
                    double amount = Double.parseDouble(totalAmount);
                    Decimal authAmount = new Decimal(amount);
                    
                    // Use the stored paymentSdk reference
                    if (paymentSdk != null) {
                        Status result = paymentSdk.getTransactionManager()
                            .respondToHostAuthorization(
                                "123456", 
                                HostDecisionType.HOST_AUTHORIZED, 
                                emvData, 
                                authAmount
                            );
                        log.info("Host authorization response status: {}", result.getStatus());
                    } else {
                        log.error("PaymentSdk reference is null, can't respond to host authorization");
                    }
                } catch (Exception e) {
                    log.error("Error responding to host authorization", e);
                }
            }
        }
    }
    
    // Expose status information
    public int getLastStatusCode() {
        return lastStatusCode;
    }
    
    public String getLastStatusMessage() {
        return lastStatusMessage;
    }
    
    public int getErrorCount() {
        return errorCount;
    }
    
    public void resetErrorCount() {
        errorCount = 0;
    }
    
    // Basic implementations of other required methods from CommerceListenerAdapter
    // These are left with minimal implementations since they're not critical for the initialization phase
    
    @Override
    public void handleNotificationEvent(NotificationEvent event) {
        log.debug("Notification event received: {}", event.getMessage());
    }
    
    @Override
    public void handleAmountAdjustedEvent(AmountAdjustedEvent event) {
        log.debug("Amount adjusted event received");
    }

    @Override
    public void handleBasketAdjustedEvent(BasketAdjustedEvent event) {
        log.debug("Basket adjusted event received");
    }

    @Override
    public void handleBasketEvent(BasketEvent event) {
        log.debug("Basket event received");
    }

    @Override
    public void handleDeviceManagementEvent(DeviceManagementEvent event) {
        log.debug("Device management event received: {}", event.getMessage());
    }

    @Override
    public void handleLoyaltyReceivedEvent(LoyaltyReceivedEvent event) {
        log.debug("Loyalty received event received");
    }

    @Override
    public void handleCardInformationReceivedEvent(CardInformationReceivedEvent event) {
        log.debug("Card information received event");
    }

    @Override
    public void handleReceiptDeliveryMethodEvent(ReceiptDeliveryMethodEvent event) {
        log.debug("Receipt delivery method event received");
    }

    @Override
    public void handleStoredValueCardEvent(StoredValueCardEvent event) {
        log.debug("Stored value card event received");
    }

    @Override
    public void handleUserInputEvent(UserInputEvent event) {
        log.debug("User input event received");
    }

    @Override
    public void handleReconciliationEvent(ReconciliationEvent event) {
        log.debug("Reconciliation event received");
    }

    @Override
    public void handleReconciliationsListEvent(ReconciliationsListEvent event) {
        log.debug("Reconciliations list event received");
    }

    @Override
    public void handleTransactionQueryEvent(TransactionQueryEvent event) {
        log.debug("Transaction query event received");
    }

    @Override
    public void handleHostFinalizeTransactionEvent(HostFinalizeTransactionEvent event) {
        log.debug("Host finalize transaction event received");
    }

    @Override
    public void handlePinEvent(PinEvent event) {
        log.debug("Pin event received");
    }

    @Override
    public void handlePrintEvent(PrintEvent event) {
        log.debug("Print event received");
    }
    
    @Override
    public void handleScannerDataEvent(ScannerDataEvent event) {
        log.debug("Scanner data event received");
    }

    @Override
    public void handleScannerStateEvent(ScannerStateEvent event) {
        log.debug("Scanner state event received");
    }

    @Override
    public void handleDeviceVitalsInformationEvent(DeviceVitalsInformationEvent event) {
        log.debug("Device vitals information event received");
    }

    @Override
    public void handleTerminalConfigRequestEvent(ConfigurationRequestEvent event) {
        log.debug("Terminal config request event received");
    }
}