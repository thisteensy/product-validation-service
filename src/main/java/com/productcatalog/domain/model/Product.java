package com.productcatalog.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@AllArgsConstructor
@Builder(toBuilder = true)
public class Product {

    private final UUID id;
    private final String upc;
    private final String title;
    private final String artist;
    private final String label;
    private final List<Track> tracks;
    private final LocalDate releaseDate;
    private final String genre;
    private final String language;
    private final List<OwnershipSplit> ownershipSplits;
    private final String artworkUri;
    private final List<String> dspTargets;
    private ProductStatus status;

    public boolean isExplicit() {
        return tracks != null && tracks.stream().anyMatch(Track::isExplicit);
    }
}