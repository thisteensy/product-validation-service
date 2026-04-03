package com.productvalidation.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@AllArgsConstructor
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
}