package com.productcatalog.application.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.productcatalog.domain.model.ChangedByType;
import com.productcatalog.domain.model.Track;
import com.productcatalog.domain.model.TrackStatus;
import com.productcatalog.domain.ports.ProductRepository;
import com.productcatalog.domain.ports.TrackRepository;
import com.productcatalog.infrastructure.rules.RuleResult;
import com.productcatalog.infrastructure.rules.RuleSeverity;
import com.productcatalog.infrastructure.rules.TrackRules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class TrackValidationConsumer {

    private static final Logger log = LoggerFactory.getLogger(TrackValidationConsumer.class);

    private final ObjectMapper objectMapper;
    private final TrackRepository trackRepository;
    private final ProductRepository productRepository;
    private final TrackRules trackRules;

    public TrackValidationConsumer(ObjectMapper objectMapper,
                                   TrackRepository trackRepository,
                                   ProductRepository productRepository,
                                   TrackRules trackRules) {
        this.objectMapper = objectMapper;
        this.trackRepository = trackRepository;
        this.productRepository = productRepository;
        this.trackRules = trackRules;
    }

    @KafkaListener(topics = "catalog.music_catalog.tracks", groupId = "track-validation")
    public void consume(@Payload String message,
                        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        try {
            TrackEventDto event = objectMapper.readValue(message, TrackEventDto.class);

            if (event.getPayload() == null || event.getPayload().getAfter() == null) {
                log.debug("Skipping track event with no after state -- likely a delete");
                return;
            }

            String op = event.getPayload().getOp();
            if (!"c".equals(op) && !"u".equals(op)) {
                log.debug("Skipping track event with op={}", op);
                return;
            }

            String status = event.getPayload().getAfter().getStatus();
            if (!TrackStatus.PENDING.name().equals(status)) {
                log.debug("Skipping track event with status={} -- only processing PENDING", status);
                return;
            }

            UUID trackId = UUID.fromString(event.getPayload().getAfter().getId());
            UUID productId = UUID.fromString(event.getPayload().getAfter().getProductId());

            Track track = trackRepository.findById(trackId)
                    .orElseThrow(() -> new RuntimeException("Track not found: " + trackId));

            List<String> dspTargets = productRepository.findById(productId)
                    .map(p -> p.getDspTargets())
                    .orElse(List.of());

            List<RuleResult> results = trackRules.evaluate(track, dspTargets);

            boolean hasBlockingFailure = results.stream()
                    .anyMatch(r -> r.getSeverity() == RuleSeverity.BLOCKING);
            boolean hasWarning = results.stream()
                    .anyMatch(r -> r.getSeverity() == RuleSeverity.WARNING);

            TrackStatus newStatus;
            if (hasBlockingFailure) {
                newStatus = TrackStatus.VALIDATION_FAILED;
            } else if (hasWarning) {
                newStatus = TrackStatus.NEEDS_REVIEW;
            } else {
                newStatus = TrackStatus.VALIDATED;
            }

            trackRepository.updateStatus(trackId, newStatus, null, ChangedByType.SYSTEM, null);
            log.info("Validated track {} -- status: {}", trackId, newStatus);

        } catch (Exception e) {
            log.error("Failed to process track event -- routing to DLQ", e);
            throw new RuntimeException(e);
        }
    }
}