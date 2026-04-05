package com.productcatalog.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.productcatalog.domain.model.*;
import com.productcatalog.domain.ports.out.ProductRepository;
import com.productcatalog.domain.ports.out.ProductStatusHistoryRepository;
import com.productcatalog.infrastructure.persistence.entities.ProductEntity;
import com.productcatalog.infrastructure.persistence.entities.TrackEntity;
import com.productcatalog.infrastructure.persistence.ports.ProductJpaRepository;
import com.productcatalog.infrastructure.persistence.ports.TrackJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class ProductRepositoryImpl implements ProductRepository {

    private final ProductJpaRepository jpaRepository;
    private final TrackJpaRepository trackJpaRepository;
    private final ObjectMapper objectMapper;
    private final ProductStatusHistoryRepository historyRepository;

    public ProductRepositoryImpl(ProductJpaRepository jpaRepository,
                                 TrackJpaRepository trackJpaRepository,
                                 ObjectMapper objectMapper,
                                 ProductStatusHistoryRepository historyRepository) {
        this.jpaRepository = jpaRepository;
        this.trackJpaRepository = trackJpaRepository;
        this.objectMapper = objectMapper;
        this.historyRepository = historyRepository;
    }

    @Override
    public void updateStatus(UUID id, ProductStatus status, String notes, ChangedByType changedByType, String changedById) {
        jpaRepository.findById(id.toString()).ifPresent(entity -> {
            ProductStatus previousStatus = ProductStatus.valueOf(entity.getStatus());
            entity.setStatus(status.name());
            entity.setReviewerNotes(notes);
            jpaRepository.save(entity);
            historyRepository.record(id, previousStatus, status, changedByType, changedById, notes);
        });
    }

    @Override
    public List<Product> findByStatus(ProductStatus status) {
        return jpaRepository.findByStatus(status.name()).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id.toString());
    }

    @Override
    public Product save(Product product) {
        try {
            ProductEntity entity = new ProductEntity(
                    product.getId().toString(),
                    product.getUpc(),
                    product.getTitle(),
                    product.getReleaseDate(),
                    product.getGenre(),
                    product.getLanguage(),
                    objectMapper.writeValueAsString(product.getOwnershipSplits()),
                    product.getArtworkUri(),
                    objectMapper.writeValueAsString(product.getDspTargets()),
                    product.getStatus().name(),
                    null,
                    null
            );
            ProductEntity saved = jpaRepository.save(entity);
            saveTracks(product.getTracks(), saved);
            return toDomain(saved);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize product: " + product.getId(), e);
        }
    }

    @Override
    public Optional<Product> findById(UUID id) {
        return jpaRepository.findById(id.toString())
                .map(this::toDomain);
    }

    @Override
    public List<Product> findAll() {
        return jpaRepository.findAll().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void update(Product product) {
        jpaRepository.findById(product.getId().toString()).ifPresent(entity -> {
            entity.setUpc(product.getUpc());
            entity.setTitle(product.getTitle());
            entity.setReleaseDate(product.getReleaseDate());
            entity.setGenre(product.getGenre());
            entity.setLanguage(product.getLanguage());
            entity.setArtworkUri(product.getArtworkUri());
            try {
                entity.setOwnershipSplits(objectMapper.writeValueAsString(product.getOwnershipSplits()));
                entity.setDspTargets(objectMapper.writeValueAsString(product.getDspTargets()));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize product: " + product.getId(), e);
            }
            jpaRepository.save(entity);
            trackJpaRepository.deleteAll(trackJpaRepository.findByProductId(product.getId().toString()));
            saveTracks(product.getTracks(), entity);
        });
    }

    @Override
    public void resubmit(UUID id) {
        jpaRepository.findById(id.toString()).ifPresent(entity -> {
            Product product = toDomain(entity);
            ProductStatus previousStatus = product.getStatus();
            product.transitionTo(ProductStatus.RESUBMITTED, product.getTracks());
            entity.setStatus(product.getStatus().name());
            jpaRepository.save(entity);
            historyRepository.record(id, previousStatus, ProductStatus.RESUBMITTED, ChangedByType.LABEL, null, null);
        });
    }

    private void saveTracks(List<Track> tracks, ProductEntity productEntity) {
        if (tracks == null) return;
        try {
            for (Track track : tracks) {
                TrackEntity trackEntity = new TrackEntity(
                        track.getId() != null ? track.getId().toString() : UUID.randomUUID().toString(),
                        productEntity,
                        track.getIsrc(),
                        track.getTitle(),
                        track.getTrackNumber(),
                        track.getAudioFileUri(),
                        track.getDuration(),
                        track.isExplicit(),
                        objectMapper.writeValueAsString(track.getContributors()),
                        objectMapper.writeValueAsString(track.getOwnershipSplits()),
                        track.getStatus().name()
                );
                trackJpaRepository.save(trackEntity);
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize track", e);
        }
    }

    private Product toDomain(ProductEntity entity) {
        try {
            List<Track> tracks = trackJpaRepository.findByProductId(entity.getId()).stream()
                    .map(this::trackToDomain)
                    .toList();
            return new Product(
                    UUID.fromString(entity.getId()),
                    entity.getUpc(),
                    entity.getTitle(),
                    tracks,
                    entity.getReleaseDate(),
                    entity.getGenre(),
                    entity.getLanguage(),
                    objectMapper.readValue(entity.getOwnershipSplits(),
                            new TypeReference<List<OwnershipSplit>>() {}),
                    entity.getArtworkUri(),
                    objectMapper.readValue(entity.getDspTargets(),
                            new TypeReference<List<String>>() {}),
                    ProductStatus.valueOf(entity.getStatus())
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize product entity: " + entity.getId(), e);
        }
    }

    private Track trackToDomain(TrackEntity entity) {
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
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize track entity: " + entity.getId(), e);
        }
    }
}