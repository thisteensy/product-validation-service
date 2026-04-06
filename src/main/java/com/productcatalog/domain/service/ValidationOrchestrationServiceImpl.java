package com.productcatalog.domain.service;

import com.productcatalog.domain.model.*;
import com.productcatalog.domain.ports.out.StatusUpdatePublisher;
import com.productcatalog.domain.ports.in.ValidationOrchestrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ValidationOrchestrationServiceImpl implements ValidationOrchestrationService {

    private static final Logger log = LoggerFactory.getLogger(ValidationOrchestrationServiceImpl.class);

    private final StatusUpdatePublisher statusUpdatePublisher;

    public ValidationOrchestrationServiceImpl(StatusUpdatePublisher statusUpdatePublisher) {
        this.statusUpdatePublisher = statusUpdatePublisher;
    }

    @Override
    public void onProductEvaluated(UUID productId, ValidationOutcome outcome) {
        ProductStatus newStatus = switch (outcome) {
            case FAILED -> ProductStatus.VALIDATION_FAILED;
            case NEEDS_REVIEW -> ProductStatus.NEEDS_REVIEW;
            case PASSED -> ProductStatus.AWAITING_TRACK_VALIDATION;
        };

        String notes = switch (outcome) {
            case FAILED -> "Product-level validation failed";
            case NEEDS_REVIEW -> "Needs manual review";
            case PASSED -> null;
        };

        statusUpdatePublisher.publish(new StatusUpdateEvent(
                StatusUpdateEvent.EntityType.PRODUCT,
                productId.toString(),
                null,
                newStatus.name(),
                notes,
                ChangedByType.SYSTEM
        ));
        log.info("Product {} transitioned to {}", productId, newStatus);
    }

    @Override
    public void onTrackEvaluated(UUID trackId, UUID productId, ValidationOutcome outcome) {
        TrackStatus newStatus = switch (outcome) {
            case FAILED -> TrackStatus.VALIDATION_FAILED;
            case NEEDS_REVIEW -> TrackStatus.NEEDS_REVIEW;
            case PASSED -> TrackStatus.VALIDATED;
        };

        statusUpdatePublisher.publish(new StatusUpdateEvent(
                StatusUpdateEvent.EntityType.TRACK,
                trackId.toString(),
                productId.toString(),
                newStatus.name(),
                null,
                ChangedByType.SYSTEM
        ));
        log.info("Track {} transitioned to {}", trackId, newStatus);
    }

    @Override
    public void onAllTracksValidated(UUID productId) {
        statusUpdatePublisher.publish(new StatusUpdateEvent(
                StatusUpdateEvent.EntityType.PRODUCT,
                productId.toString(),
                null,
                ProductStatus.VALIDATED.name(),
                null,
                ChangedByType.SYSTEM
        ));
        log.info("Product {} fully validated -- all tracks passed", productId);
    }
}
