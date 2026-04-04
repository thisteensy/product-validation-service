package com.productcatalog.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Set;
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

    public void transitionTo(TrackStatus newStatus) {
        Set<TrackStatus> allowed = allowedTransitions();
        if (!allowed.contains(newStatus)) {
            throw new IllegalStateException(
                    "Invalid track status transition from " + this.status + " to " + newStatus
            );
        }
        this.status = newStatus;
    }

    private Set<TrackStatus> allowedTransitions() {
        return switch (this.status) {
            case PENDING -> Set.of(
                    TrackStatus.VALIDATED,
                    TrackStatus.VALIDATION_FAILED,
                    TrackStatus.NEEDS_REVIEW
            );
            case VALIDATION_FAILED, NEEDS_REVIEW -> Set.of(TrackStatus.PENDING);
            case VALIDATED -> Set.of(TrackStatus.PUBLISHED);
            case PUBLISHED -> Set.of(TrackStatus.TAKEN_DOWN);
            case TAKEN_DOWN -> Set.of(TrackStatus.PUBLISHED, TrackStatus.RETIRED);
            case RETIRED -> Set.of();
        };
    }
}