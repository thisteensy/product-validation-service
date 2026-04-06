package com.productcatalog.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@AllArgsConstructor
@Builder(toBuilder = true)
public class Track {
    private final UUID id;
    private final String isrc;
    private final String title;
    private final int trackNumber;
    private final String audioFileUri;
    private final int duration;
    private final boolean explicit;
    private final List<ProductContributor> contributors;
    private final List<OwnershipSplit> ownershipSplits;
    private TrackStatus status;
}