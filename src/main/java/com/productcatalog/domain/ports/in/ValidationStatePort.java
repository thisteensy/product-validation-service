package com.productcatalog.domain.ports.in;

import java.util.Map;
import java.util.UUID;

public interface ValidationStatePort {
    boolean onValidationStateUpdated(UUID productId, String productStatus, Map<UUID, String> trackStatuses);
}