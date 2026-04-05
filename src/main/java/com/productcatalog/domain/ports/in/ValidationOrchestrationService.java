package com.productcatalog.domain.ports.in;

import com.productcatalog.domain.model.ValidationOutcome;

import java.util.UUID;

public interface ValidationOrchestrationService {
    void onProductEvaluated(UUID productId, ValidationOutcome outcome);
    void onTrackEvaluated(UUID trackId, UUID productId, ValidationOutcome outcome);
    void onAllTracksValidated(UUID productId);
}