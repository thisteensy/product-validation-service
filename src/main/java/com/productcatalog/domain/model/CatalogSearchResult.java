package com.productcatalog.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class CatalogSearchResult {
    private final UUID trackId;
    private final String isrc;
    private final String trackTitle;
    private final TrackStatus trackStatus;
    private final UUID productId;
    private final String productTitle;
    private final String artist;
    private final String label;
    private final String genre;
    private final ProductStatus productStatus;
}