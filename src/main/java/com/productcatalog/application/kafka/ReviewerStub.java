package com.productcatalog.application.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Stub simulating a downstream reviewer workflow service.
 * In production this would route products flagged for human review
 * to an internal QC team queue for manual disposition.
 */
@Component
public class ReviewerStub {

    private static final Logger log = LoggerFactory.getLogger(ReviewerStub.class);

    private final ObjectMapper objectMapper;

    public ReviewerStub(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "catalog.music_catalog.products",
            groupId = "reviewer-stub"
    )
    public void onProductEvent(@Payload String message,
                               @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        try {
            ProductEventDto event = objectMapper.readValue(message, ProductEventDto.class);

            if (event.getPayload() == null || event.getPayload().getAfter() == null) {
                return;
            }

            String status = event.getPayload().getAfter().getStatus();
            if (!"NEEDS_REVIEW".equals(status)) {
                return;
            }

            String productId = event.getPayload().getAfter().getId();
            log.info("[ReviewerStub] Would route product={} to human review queue", productId);

        } catch (Exception e) {
            log.error("[ReviewerStub] Failed to process event", e);
        }
    }
}