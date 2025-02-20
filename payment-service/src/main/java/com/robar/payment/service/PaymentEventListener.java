package com.robar.payment.service;

import com.verifone.payment_sdk.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.context.ApplicationEventPublisher;

@Slf4j
@Component
public class PaymentEventListener extends CommerceListener2 {
    private final ApplicationEventPublisher eventPublisher;

    public PaymentEventListener(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }
    /* 
    @Override
    public void handlePaymentCompletedEvent(PaymentCompletedEvent event) {
        if (event.getStatus() == 0) {
            log.info("Payment completed successfully");
            eventPublisher.publishEvent(new PaymentStatusEvent(
                event.getPayment().getPaymentId(), 
                PaymentStatus.COMPLETED, 
                "Payment successful"
            ));
        } else {
            log.error("Payment failed: {}", event.getMessage());
            eventPublisher.publishEvent(new PaymentStatusEvent(
                event.getPayment().getPaymentId(),
                PaymentStatus.FAILED,
                event.getMessage()
            ));
        }
    }

    @Override
    public void handleStatus(Status status) {
        log.info("Terminal status update: {}", status.getMessage());
        if (status.getStatusCode() != 0) {
            log.error("Terminal error: {}", status.getMessage());
        }
    }
    */

    @Override
    public void handleStatus(Status status) {
        // Keep your existing implementation
    }

    // Required empty implementations

    @Override
    public void handlePaymentCompletedEvent(PaymentCompletedEvent event) {
    System.out.println("Payment completed: " + event);
}


    @Override
    public void handleNotificationEvent(NotificationEvent event) {
        log.debug("Notification event received");
    }

    @Override
    public void handlePrintEvent(PrintEvent event) {
        log.debug("Print event received");
    }

    @Override
    public void handleHostFinalizeTransactionEvent(HostFinalizeTransactionEvent event) {
        log.debug("Host finalize transaction event received");
    }

    @Override
    public void handleTransactionEvent(TransactionEvent event) {
        log.debug("Transaction event received");
    }

    @Override
    public void handleDeviceVitalsInformationEvent(DeviceVitalsInformationEvent event) {
        log.debug("Device vitals information event received");
    }

    @Override
    public void handleScannerStateEvent(ScannerStateEvent event) {
        log.debug("Scanner state event received");
    }

    @Override
    public void handleBasketEvent(BasketEvent event) {
        log.debug("Basket event received");
    }

    @Override
    public void handleHostAuthorizationEvent(HostAuthorizationEvent event) {
        log.debug("Host authorization event received");
    }

    @Override
    public void handleAmountAdjustedEvent(AmountAdjustedEvent event) {
        log.debug("Amount adjusted event received");
    }

    @Override
    public void handleStoredValueCardEvent(StoredValueCardEvent event) {
        log.debug("Stored value card event received");
    }

    @Override
    public void handleDeviceManagementEvent(DeviceManagementEvent event) {
        log.debug("Device management event received");
    }

    @Override
    public void handleScannerDataEvent(ScannerDataEvent event) {
        log.debug("Scanner data event received");
    }

    @Override
    public void handleCardInformationReceivedEvent(CardInformationReceivedEvent event) {
        log.debug("Card information received event");
    }

    @Override
    public void handleBasketAdjustedEvent(BasketAdjustedEvent event) {
        log.debug("Basket adjusted event received");
    }

    @Override
    public void handleLoyaltyReceivedEvent(LoyaltyReceivedEvent event) {
        log.debug("Loyalty received event received");
    }

    @Override
    public void handleReconciliationsListEvent(ReconciliationsListEvent event) {
        log.debug("Reconciliations list event received");
    }

    @Override
    public void handleCommerceEvent(CommerceEvent event) {
        log.debug("Commerce event received");
    }
    

    @Override
    public void handleReconciliationEvent(ReconciliationEvent event) {
        log.debug("Reconciliation event received");
    }

    @Override
    public void handlePinEvent(PinEvent event) {
        log.debug("Pin event received");
    }

    @Override
    public void handleReceiptDeliveryMethodEvent(ReceiptDeliveryMethodEvent event) {
        log.debug("Receipt delivery method event received");
    }

    @Override
    public void handleTerminalConfigRequestEvent(ConfigurationRequestEvent event) {
        log.debug("Terminal config request event received");
    }

    @Override
    public void handleTransactionQueryEvent(TransactionQueryEvent event) {
        log.debug("Transaction query event received");
    }

    @Override
    public void handleUserInputEvent(UserInputEvent event) {
        log.debug("User input event received");
    }
 
}