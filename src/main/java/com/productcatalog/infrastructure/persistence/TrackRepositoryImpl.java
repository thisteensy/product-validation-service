package com.productcatalog.infrastructure.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.productcatalog.domain.model.*;
import com.productcatalog.domain.ports.out.TrackRepository;
import com.productcatalog.infrastructure.persistence.entities.TrackEntity;
import com.productcatalog.infrastructure.persistence.ports.TrackJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class TrackRepositoryImpl implements TrackRepository {

    private final TrackJpaRepository jpaRepository;
    private final ObjectMapper objectMapper;

    public TrackRepositoryImpl(TrackJpaRepository jpaRepository, ObjectMapper objectMapper) {
        this.jpaRepository = jpaRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<Track> findById(UUID id) {
        return jpaRepository.findById(id.toString())
                .map(this::toDomain);
    }

    @Override
    public List<Track> findByProductId(UUID productId) {
        return jpaRepository.findByProductId(productId.toString()).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void updateStatus(UUID id, TrackStatus status, String notes,
                             ChangedByType changedByType, String changedById) {
        TrackEntity entity = jpaRepository.findById(id.toString())
                .orElseThrow(() -> new IllegalStateException("Track not found: " + id));
        Track track = toDomain(entity);
        track.transitionTo(status);
        entity.setStatus(track.getStatus().name());
        jpaRepository.save(entity);
    }

    private Track toDomain(TrackEntity entity) {
        try {
            return new Track(
                    UUID.fromString(entity.getId()),
                    entity.getIsrc(),
                    entity.getTitle(),
                    entity.getTrackNumber(),
                    entity.getAudioFileUri(),
                    entity.getDuration(),
                    entity.isExplicit(),
                    objectMapper.readValue(entity.getContributors(),
                            new TypeReference<List<ProductContributor>>() {}),
                    objectMapper.readValue(entity.getOwnershipSplits(),
                            new TypeReference<List<OwnershipSplit>>() {}),
                    TrackStatus.valueOf(entity.getStatus())
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize track entity: " + entity.getId(), e);
        }
    }
}