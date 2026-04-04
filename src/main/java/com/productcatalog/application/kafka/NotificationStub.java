package com.productcatalog.application.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Stub simulating a downstream notification service.
 * In production this would dispatch emails or webhooks to labels
 * on validation outcome and publication events.
 */
@Component
public class NotificationStub {

    private static final Logger log = LoggerFactory.getLogger(NotificationStub.class);

    private static final Set<String> NOTIFIABLE_STATUSES = Set.of(
            "VALIDATED", "VALIDATION_FAILED", "PUBLISHED"
    );

    private final ObjectMapper objectMapper;

    public NotificationStub(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "catalog.music_catalog.products",
            groupId = "notification-stub"
    )
    public void onProductEvent(@Payload String message,
                               @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        try {
            ProductEventDto event = objectMapper.readValue(message, ProductEventDto.class);

            if (event.getPayload() == null || event.getPayload().getAfter() == null) {
                return;
            }

            String status = event.getPayload().getAfter().getStatus();
            if (!NOTIFIABLE_STATUSES.contains(status)) {
                return;
            }

            String productId = event.getPayload().getAfter().getId();
            log.info("[NotificationStub] Would notify label for product={} status={}", productId, status);

        } catch (Exception e) {
            log.error("[NotificationStub] Failed to process event", e);
        }
    }
}