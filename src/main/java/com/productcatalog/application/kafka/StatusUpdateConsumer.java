package com.productcatalog.application.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.productcatalog.domain.model.ChangedByType;
import com.productcatalog.domain.model.ProductStatus;
import com.productcatalog.domain.model.StatusUpdateEvent;
import com.productcatalog.domain.model.TrackStatus;
import com.productcatalog.domain.ports.out.ProductRepository;
import com.productcatalog.domain.ports.out.TrackRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class StatusUpdateConsumer {

    private static final Logger log = LoggerFactory.getLogger(StatusUpdateConsumer.class);

    private final ProductRepository productRepository;
    private final TrackRepository trackRepository;
    private final ObjectMapper objectMapper;

    public StatusUpdateConsumer(ProductRepository productRepository,
                                TrackRepository trackRepository,
                                ObjectMapper objectMapper) {
        this.productRepository = productRepository;
        this.trackRepository = trackRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "catalog.validation.status-updates", groupId = "status-update-writer")
    public void consume(@Payload String message,
                        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        try {
            StatusUpdateEvent event = objectMapper.readValue(message, StatusUpdateEvent.class);

            if (event.getEntityType() == StatusUpdateEvent.EntityType.PRODUCT) {
                productRepository.updateStatus(
                        UUID.fromString(event.getEntityId()),
                        ProductStatus.valueOf(event.getStatus()),
                        event.getNotes(),
                        ChangedByType.SYSTEM,
                        null
                );
            } else {
                trackRepository.updateStatus(
                        UUID.fromString(event.getEntityId()),
                        TrackStatus.valueOf(event.getStatus()),
                        event.getNotes(),
                        ChangedByType.SYSTEM,
                        null
                );
            }

        } catch (Exception e) {
            log.error("Failed to process status update -- routing to DLQ", e);
            throw new RuntimeException(e);
        }
    }
}