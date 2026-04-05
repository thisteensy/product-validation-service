package com.productcatalog.infrastructure.persistence;

import com.productcatalog.domain.model.ChangedByType;
import com.productcatalog.domain.model.ProductStatus;
import com.productcatalog.domain.ports.out.ProductStatusHistoryRepository;
import com.productcatalog.infrastructure.persistence.entities.ProductStatusHistoryEntity;
import com.productcatalog.infrastructure.persistence.ports.ProductStatusHistoryJpaRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class ProductStatusHistoryRepositoryImpl implements ProductStatusHistoryRepository {

    private final ProductStatusHistoryJpaRepository jpaRepository;

    public ProductStatusHistoryRepositoryImpl(ProductStatusHistoryJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void record(UUID productId,
                       ProductStatus previousStatus,
                       ProductStatus newStatus,
                       ChangedByType changedByType,
                       String changedById,
                       String notes) {
        ProductStatusHistoryEntity entity = new ProductStatusHistoryEntity(
                UUID.randomUUID().toString(),
                productId.toString(),
                previousStatus.name(),
                newStatus.name(),
                changedByType,
                changedById,
                notes,
                LocalDateTime.now()
        );
        jpaRepository.save(entity);
    }
}