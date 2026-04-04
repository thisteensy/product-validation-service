package com.productcatalog.domain.ports;

import com.productcatalog.domain.model.Product;
import com.productcatalog.domain.model.ProductStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository {
    Product save(Product product);
    Optional<Product> findById(UUID id);
    List<Product> findAll();
    void updateStatus(UUID id, ProductStatus status, String reviewerNotes);
    List<Product> findByStatus(ProductStatus status);
    void deleteById(UUID id);
    void update(Product product);
    void resubmit(UUID id);
}