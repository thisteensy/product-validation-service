package com.productcatalog.domain.service;

import com.productcatalog.domain.model.*;
import com.productcatalog.domain.ports.in.ValidationStatePort;
import com.productcatalog.domain.ports.out.RuleEngine;
import com.productcatalog.domain.ports.out.StatusUpdatePublisher;
import com.productcatalog.domain.ports.in.ValidationOrchestrationService;
import com.productcatalog.domain.ports.out.ValidationStateStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ValidationOrchestrationServiceImpl implements ValidationOrchestrationService, ValidationStatePort {

    private static final Logger log = LoggerFactory.getLogger(ValidationOrchestrationServiceImpl.class);

    private final RuleEngine ruleEngine;
    private final StatusUpdatePublisher statusUpdatePublisher;
    private final ValidationStateStore validationStateStore;

    public ValidationOrchestrationServiceImpl(RuleEngine ruleEngine, StatusUpdatePublisher statusUpdatePublisher, ValidationStateStore validationStateStore) {
        this.ruleEngine = ruleEngine;
        this.statusUpdatePublisher = statusUpdatePublisher;
        this.validationStateStore = validationStateStore;
    }

    @Override
    public void submitProduct(Product product) {
        ValidationOutcome outcome = ruleEngine.evaluateProduct(product);
        onProductEvaluated(product.getId(), outcome);
    }

    @Override
    public void submitTrack(Track track, UUID productId) {
        List<String> dspTargets = validationStateStore.getDspTargets(productId);
        ValidationOutcome outcome = ruleEngine.evaluateTrack(track, dspTargets);
        onTrackEvaluated(track.getId(), productId, outcome);
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

    @Override
    public boolean onValidationStateUpdated(UUID productId, String productStatus, Map<UUID, String> trackStatuses) {
        boolean awaitingTracks = ProductStatus.AWAITING_TRACK_VALIDATION.name().equals(productStatus);
        boolean allValidated = !trackStatuses.isEmpty() &&
                trackStatuses.values().stream().allMatch(s -> TrackStatus.VALIDATED.name().equals(s));

        if (awaitingTracks && allValidated) {
            onAllTracksValidated(productId);
            return true;
        }
        return false;
    }
}
