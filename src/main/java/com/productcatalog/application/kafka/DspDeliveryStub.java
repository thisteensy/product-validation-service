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
 * Stub simulating a downstream DSP delivery service.
 * In production this would push published products to configured
 * DSP targets (Spotify, Apple Music, etc.) and process takedowns.
 */
@Component
public class DspDeliveryStub {

    private static final Logger log = LoggerFactory.getLogger(DspDeliveryStub.class);

    private static final Set<String> ACTIONABLE_STATUSES = Set.of(
            "PUBLISHED", "TAKEN_DOWN"
    );

    private final ObjectMapper objectMapper;

    public DspDeliveryStub(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "catalog.music_catalog.products",
            groupId = "dsp-delivery-stub"
    )
    public void onProductEvent(@Payload String message,
                               @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        try {
            ProductEventDto event = objectMapper.readValue(message, ProductEventDto.class);

            if (event.getPayload() == null || event.getPayload().getAfter() == null) {
                return;
            }

            String status = event.getPayload().getAfter().getStatus();
            if (!ACTIONABLE_STATUSES.contains(status)) {
                return;
            }

            String productId = event.getPayload().getAfter().getId();
            log.info("[DspDeliveryStub] Would deliver to DSPs for product={} status={}", productId, status);

        } catch (Exception e) {
            log.error("[DspDeliveryStub] Failed to process event", e);
        }
    }
}