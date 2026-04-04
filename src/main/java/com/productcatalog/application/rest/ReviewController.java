package com.productcatalog.application.rest;

import com.productcatalog.domain.model.Product;
import com.productcatalog.domain.model.ProductStatus;
import com.productcatalog.domain.ports.ProductRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/reviews")
public class ReviewController {

    private final ProductRepository productRepository;

    public ReviewController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @GetMapping("/pending")
    public ResponseEntity<List<Product>> getPendingReviews() {
        return ResponseEntity.ok(productRepository.findByStatus(ProductStatus.NEEDS_REVIEW));
    }

    @PatchMapping("/{id}/decision")
    public ResponseEntity<Void> submitDecision(
            @PathVariable UUID id,
            @Valid @RequestBody ReviewDecisionParams decision) {

        if (decision.getStatus() != ProductStatus.VALIDATED
                && decision.getStatus() != ProductStatus.VALIDATION_FAILED) {
            return ResponseEntity.badRequest().build();
        }

        String notes = "Manual review: " + (decision.getNotes() != null ? decision.getNotes() : "no notes provided");
        productRepository.updateStatus(id, decision.getStatus(), notes);
        return ResponseEntity.ok().build();
    }
}