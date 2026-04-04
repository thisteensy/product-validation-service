package com.productcatalog.domain.ports;

import com.productcatalog.domain.model.ChangedByType;
import com.productcatalog.domain.model.Track;
import com.productcatalog.domain.model.TrackStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrackRepository {
    Optional<Track> findById(UUID id);
    List<Track> findByProductId(UUID productId);
    void updateStatus(UUID id, TrackStatus status, String notes, ChangedByType changedByType, String changedById);
}