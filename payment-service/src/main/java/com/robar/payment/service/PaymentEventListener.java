package com.robar.payment.service;

import com.verifone.payment_sdk.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;


import java.util.HashMap;

@Slf4j
@Component
public class PaymentEventListener extends CommerceListener2 {
    private final ApplicationEventPublisher eventPublisher;
    private PaymentSdk paymentSdk;

    public PaymentEventListener(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;

    }

    public void setPaymentSdk(PaymentSdk paymentSdk) {
        this.paymentSdk = paymentSdk;
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
    public void handleStatus(Status status) {
        log.info("Terminal status update: {}", status.getMessage());
        if (status.getStatus() != 0) {
            log.error("Terminal error: {}", status.getMessage());
        }
    }

    @Override
    public void handleNotificationEvent(NotificationEvent event) {
        log.debug("Notification event received");
    }
    
    @Override
    public void handleCommerceEvent(CommerceEvent event) {
        log.info("Commerce event: {} - {}", event.getType(), event.getMessage());
    }

    @Override
    public void handleTransactionEvent(TransactionEvent event) {
        log.info("Transaction event: {} - {}", event.getType(), event.getMessage());
        
        if (event.getType().equals(TransactionEvent.LOGIN_COMPLETED)) {
            if (event.getStatus() == StatusCode.SUCCESS) {
                log.info("Login successful");
            } else {
                log.error("Login failed");
            }
        }
        
        if (event.getType().equals(CommerceEvent.SESSION_STARTED)) {
            if (event.getStatus() == StatusCode.SUCCESS) {
                log.info("Session started successfully");
            } else {
                log.error("Session start failed");
            }
        }
        
        if (event.getType().equals(CommerceEvent.SESSION_ENDED)) {
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
    // Basic implementations of other required methods
    
    @Override
    public void handleAmountAdjustedEvent(AmountAdjustedEvent event) {
        // Basic implementation
    }

    @Override
    public void handleBasketAdjustedEvent(BasketAdjustedEvent event) {
        // Basic implementation
    }

    @Override
    public void handleBasketEvent(BasketEvent event) {
        // Basic implementation
    }

    @Override
    public void handleDeviceManagementEvent(DeviceManagementEvent event) {
        // Basic implementation
    }

    @Override
    public void handleLoyaltyReceivedEvent(LoyaltyReceivedEvent event) {
        // Basic implementation
    }

    @Override
    public void handleCardInformationReceivedEvent(CardInformationReceivedEvent event) {
        // Basic implementation
    }

    @Override
    public void handleReceiptDeliveryMethodEvent(ReceiptDeliveryMethodEvent event) {
        // Basic implementation
    }

    @Override
    public void handleStoredValueCardEvent(StoredValueCardEvent event) {
        // Basic implementation
    }

    @Override
    public void handleUserInputEvent(UserInputEvent event) {
        // Basic implementation
    }

    @Override
    public void handleReconciliationEvent(ReconciliationEvent event) {
        // Basic implementation
    }

    @Override
    public void handleReconciliationsListEvent(ReconciliationsListEvent event) {
        // Basic implementation
    }

    @Override
    public void handleTransactionQueryEvent(TransactionQueryEvent event) {
        // Basic implementation
    }

    @Override
    public void handleHostFinalizeTransactionEvent(HostFinalizeTransactionEvent event) {
        // Basic implementation
    }

    @Override
    public void handlePinEvent(PinEvent event) {
        // Basic implementation
    }

    @Override
    public void handlePrintEvent(PrintEvent event) {
        // Basic implementation
    }
    
    @Override
    public void handleScannerDataEvent(ScannerDataEvent event) {
        // Basic implementation
    }

    @Override
    public void handleScannerStateEvent(ScannerStateEvent event) {
        // Basic implementation
    }

    @Override
    public void handleDeviceVitalsInformationEvent(DeviceVitalsInformationEvent event) {
        // Basic implementation
    }

    @Override
    public void handleTerminalConfigRequestEvent(ConfigurationRequestEvent event) {
        // Basic implementation
    }
}