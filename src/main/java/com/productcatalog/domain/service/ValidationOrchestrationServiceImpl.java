package com.productcatalog.domain.service;

import com.productcatalog.domain.model.ChangedByType;
import com.productcatalog.domain.model.ProductStatus;
import com.productcatalog.domain.model.TrackStatus;
import com.productcatalog.domain.model.ValidationOutcome;
import com.productcatalog.domain.ports.out.ProductRepository;
import com.productcatalog.domain.ports.out.TrackRepository;
import com.productcatalog.domain.ports.in.ValidationOrchestrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ValidationOrchestrationServiceImpl implements ValidationOrchestrationService {

    private static final Logger log = LoggerFactory.getLogger(ValidationOrchestrationServiceImpl.class);

    private final ProductRepository productRepository;
    private final TrackRepository trackRepository;

    public ValidationOrchestrationServiceImpl(ProductRepository productRepository,
                                              TrackRepository trackRepository) {
        this.productRepository = productRepository;
        this.trackRepository = trackRepository;
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

        productRepository.updateStatus(productId, newStatus, notes, ChangedByType.SYSTEM, null);
        log.info("Product {} transitioned to {}", productId, newStatus);
    }

    @Override
    public void onTrackEvaluated(UUID trackId, UUID productId, ValidationOutcome outcome) {
        TrackStatus newStatus = switch (outcome) {
            case FAILED -> TrackStatus.VALIDATION_FAILED;
            case NEEDS_REVIEW -> TrackStatus.NEEDS_REVIEW;
            case PASSED -> TrackStatus.VALIDATED;
        };

        trackRepository.updateStatus(trackId, newStatus, null, ChangedByType.SYSTEM, null);
        log.info("Track {} transitioned to {}", trackId, newStatus);
    }

    @Override
    public void onAllTracksValidated(UUID productId) {
        productRepository.updateStatus(productId, ProductStatus.VALIDATED,
                null, ChangedByType.SYSTEM, null);
        log.info("Product {} fully validated -- all tracks passed", productId);
    }
}