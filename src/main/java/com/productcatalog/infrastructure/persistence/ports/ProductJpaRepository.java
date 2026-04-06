package com.productcatalog.infrastructure.persistence.ports;

import com.productcatalog.infrastructure.persistence.entities.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductJpaRepository extends JpaRepository<ProductEntity, String>, JpaSpecificationExecutor<ProductEntity> {
}