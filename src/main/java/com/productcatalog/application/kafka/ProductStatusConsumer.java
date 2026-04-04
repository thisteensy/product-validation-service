package com.productcatalog.application.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.productcatalog.domain.model.ChangedByType;
import com.productcatalog.domain.model.ProductStatus;
import com.productcatalog.domain.model.Track;
import com.productcatalog.domain.model.TrackStatus;
import com.productcatalog.domain.ports.ProductRepository;
import com.productcatalog.domain.ports.TrackRepository;
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
public class ProductStatusConsumer {

    private static final Logger log = LoggerFactory.getLogger(ProductStatusConsumer.class);

    private final ObjectMapper objectMapper;
    private final TrackRepository trackRepository;
    private final ProductRepository productRepository;

    public ProductStatusConsumer(ObjectMapper objectMapper,
                                 TrackRepository trackRepository,
                                 ProductRepository productRepository) {
        this.objectMapper = objectMapper;
        this.trackRepository = trackRepository;
        this.productRepository = productRepository;
    }

    @KafkaListener(topics = "catalog.music_catalog.tracks", groupId = "product-status")
    public void consume(@Payload String message,
                        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        try {
            TrackEventDto event = objectMapper.readValue(message, TrackEventDto.class);

            if (event.getPayload() == null || event.getPayload().getAfter() == null) {
                return;
            }

            String op = event.getPayload().getOp();
            if (!"u".equals(op)) {
                return;
            }

            String status = event.getPayload().getAfter().getStatus();
            if (TrackStatus.PENDING.name().equals(status)) {
                return;
            }

            UUID productId = UUID.fromString(event.getPayload().getAfter().getProductId());

            productRepository.findById(productId).ifPresent(product -> {
                if (product.getStatus() != ProductStatus.AWAITING_TRACK_VALIDATION) {
                    log.debug("Skipping product status recomputation -- product {} is not awaiting track validation", productId);
                    return;
                }

                List<Track> tracks = trackRepository.findByProductId(productId);

                boolean anyFailed = tracks.stream()
                        .anyMatch(t -> t.getStatus() == TrackStatus.VALIDATION_FAILED);
                boolean anyNeedsReview = tracks.stream()
                        .anyMatch(t -> t.getStatus() == TrackStatus.NEEDS_REVIEW);
                boolean allValidated = tracks.stream()
                        .allMatch(t -> t.getStatus() == TrackStatus.VALIDATED);

                ProductStatus newStatus;
                if (anyFailed) {
                    newStatus = ProductStatus.VALIDATION_FAILED;
                } else if (anyNeedsReview) {
                    newStatus = ProductStatus.NEEDS_REVIEW;
                } else if (allValidated) {
                    newStatus = ProductStatus.VALIDATED;
                } else {
                    log.debug("Product {} tracks still in progress -- no status update", productId);
                    return;
                }

                productRepository.updateStatus(productId, newStatus, null,
                        ChangedByType.SYSTEM, null);
                log.info("Recomputed product {} status to {}", productId, newStatus);
            });

        } catch (Exception e) {
            log.error("Failed to process track status event", e);
            throw new RuntimeException(e);
        }
    }
}