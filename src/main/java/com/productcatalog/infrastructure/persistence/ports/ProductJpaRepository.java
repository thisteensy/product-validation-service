package com.productcatalog.infrastructure.persistence.ports;

import com.productcatalog.infrastructure.persistence.entities.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductJpaRepository extends JpaRepository<ProductEntity, String> {
    List<ProductEntity> findByStatus(String status);
}