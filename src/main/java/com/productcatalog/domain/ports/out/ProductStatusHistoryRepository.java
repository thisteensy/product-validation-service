package com.productcatalog.domain.ports.out;

import com.productcatalog.domain.model.ChangedByType;
import com.productcatalog.domain.model.ProductStatus;

import java.util.UUID;

public interface ProductStatusHistoryRepository {
    void record(UUID productId,
                ProductStatus previousStatus,
                ProductStatus newStatus,
                ChangedByType changedByType,
                String changedById,
                String notes);
}