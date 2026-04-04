package com.productcatalog.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Builder;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Getter
@AllArgsConstructor
@Builder(toBuilder = true)
public class Product {

    private final UUID id;
    private final String upc;
    private final String isrc;
    private final String title;
    private final List<ProductContributor> contributors;
    private final LocalDate releaseDate;
    private final String genre;
    private final boolean explicit;
    private final String language;
    private final List<OwnershipSplit> ownershipSplits;
    private final String audioFileUri;
    private final String artworkUri;
    private final List<String> dspTargets;
    private ProductStatus status;

    public void updateStatus(ProductStatus status) {
        this.status = status;
    }

    public void transitionTo(ProductStatus newStatus) {
        Set<ProductStatus> allowed = allowedTransitions();
        if (!allowed.contains(newStatus)) {
            throw new IllegalStateException(
                    "Invalid status transition from " + this.status + " to " + newStatus
            );
        }
        this.status = newStatus;
    }

    private Set<ProductStatus> allowedTransitions() {
        return switch (this.status) {
            case SUBMITTED, RESUBMITTED -> Set.of(
                    ProductStatus.VALIDATED,
                    ProductStatus.VALIDATION_FAILED,
                    ProductStatus.NEEDS_REVIEW
            );
            case VALIDATION_FAILED -> Set.of(ProductStatus.RESUBMITTED);
            case NEEDS_REVIEW -> Set.of(
                    ProductStatus.VALIDATED,
                    ProductStatus.VALIDATION_FAILED
            );
            case VALIDATED -> Set.of(ProductStatus.PUBLISHED);
            case PUBLISHED -> Set.of(ProductStatus.TAKEN_DOWN);
            case TAKEN_DOWN -> Set.of(ProductStatus.PUBLISHED, ProductStatus.RETIRED);
            case RETIRED -> Set.of();
        };
    }
}