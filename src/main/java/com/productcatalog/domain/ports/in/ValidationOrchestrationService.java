package com.productcatalog.domain.ports.in;

import com.productcatalog.domain.model.Product;
import com.productcatalog.domain.model.Track;
import com.productcatalog.domain.model.ValidationOutcome;

import java.util.List;
import java.util.UUID;

public interface ValidationOrchestrationService {
    void submitProduct(Product product);
    void submitTrack(Track track, UUID productId);
    void onProductEvaluated(UUID productId, ValidationOutcome outcome, List<String> violations);
    void onTrackEvaluated(UUID trackId, UUID productId, ValidationOutcome outcome, List<String> violations);
    void onAllTracksValidated(UUID productId);
}