package com.productcatalog.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.productcatalog.domain.model.StatusUpdateEvent;
import com.productcatalog.domain.ports.out.StatusUpdatePublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class StatusUpdatePublisherImpl implements StatusUpdatePublisher {

    private static final Logger log = LoggerFactory.getLogger(StatusUpdatePublisherImpl.class);
    private static final String TOPIC = "catalog.validation.status-updates";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public StatusUpdatePublisherImpl(KafkaTemplate<String, String> kafkaTemplate,
                                     ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(StatusUpdateEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(TOPIC, event.getEntityId(), payload);
            log.debug("Published status update for {} {}", event.getEntityType(), event.getEntityId());
        } catch (Exception e) {
            throw new RuntimeException("Failed to publish status update for " + event.getEntityId(), e);
        }
    }
}