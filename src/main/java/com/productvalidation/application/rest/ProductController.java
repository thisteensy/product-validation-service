package com.productvalidation.application.rest;

import com.productvalidation.domain.model.Product;
import com.productvalidation.domain.model.ProductStatus;
import com.productvalidation.domain.ports.ProductRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductRepository productRepository;

    public ProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @PostMapping
    public ResponseEntity<Product> create(@RequestBody Product product) {
        Product toSave = product.toBuilder()
                .id(UUID.randomUUID())
                .status(ProductStatus.SUBMITTED)
                .build();
        Product saved = productRepository.save(toSave);
        return ResponseEntity.created(URI.create("/products/" + saved.getId())).body(saved);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getById(@PathVariable UUID id) {
        return productRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<Product>> getByStatus(
            @RequestParam(required = false) ProductStatus status) {
        if (status != null) {
            return ResponseEntity.ok(productRepository.findByStatus(status));
        }
        return ResponseEntity.ok(productRepository.findByStatus(ProductStatus.SUBMITTED));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> updateStatus(
            @PathVariable UUID id,
            @RequestBody ReviewDecisionDto decision) {

        if (decision.getStatus() == null) {
            return ResponseEntity.badRequest().build();
        }

        if (decision.getStatus() != ProductStatus.VALIDATED
                && decision.getStatus() != ProductStatus.VALIDATION_FAILED) {
            return ResponseEntity.badRequest().build();
        }

        String notes = "Manual review: " + (decision.getNotes() != null ? decision.getNotes() : "no notes provided");
        productRepository.updateStatus(id, decision.getStatus(), notes);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/pending-review")
    public ResponseEntity<List<Product>> getPendingReviews() {
        return ResponseEntity.ok(productRepository.findByStatus(ProductStatus.NEEDS_REVIEW));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        if (productRepository.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        productRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}