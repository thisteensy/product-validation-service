package com.productcatalog.infrastructure.persistence.ports;

import com.productcatalog.infrastructure.persistence.entities.ProductStatusHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductStatusHistoryJpaRepository extends JpaRepository<ProductStatusHistoryEntity, String> {
}