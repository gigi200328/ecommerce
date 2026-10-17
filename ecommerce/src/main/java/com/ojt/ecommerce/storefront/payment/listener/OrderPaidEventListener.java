package com.ojt.ecommerce.storefront.payment.listener;

import com.ojt.ecommerce.storefront.payment.event.OrderPaidEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class OrderPaidEventListener {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${backoffice.g4.base-url:http://localhost:8084}")
    private String g4BaseUrl;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderPaid(OrderPaidEvent event) {
        log.info("Transaction COMMITTED for Order {}. Notifying G4 Backoffice...", event.orderId());

        String targetUrl = g4BaseUrl + "/api/internal/v1/orders/" + event.orderId() + "/notify-payment";

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<OrderPaidEvent> request = new HttpEntity<>(event, headers);

            restTemplate.postForEntity(targetUrl, request, Void.class);
            log.info("Successfully notified G4 Backoffice for order {} (Ref: {})", event.orderId(), event.transactionRef());
        } catch (Exception ex) {
            log.warn("Could not notify G4 Backoffice at {} (G4 may be offline or mock mode active). Error: {}", targetUrl, ex.getMessage());
        }
    }
}
