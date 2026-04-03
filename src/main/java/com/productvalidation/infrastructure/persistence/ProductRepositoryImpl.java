package com.productvalidation.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.productvalidation.domain.model.*;
import com.productvalidation.domain.ports.ProductRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class ProductRepositoryImpl implements ProductRepository {

    private final ProductJpaRepository jpaRepository;
    private final ObjectMapper objectMapper;

    public ProductRepositoryImpl(ProductJpaRepository jpaRepository, ObjectMapper objectMapper) {
        this.jpaRepository = jpaRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void updateStatus(UUID id, ProductStatus status, String reviewerNotes) {
        jpaRepository.findById(id.toString()).ifPresent(entity -> {
            entity.setStatus(status.name());
            entity.setReviewerNotes(reviewerNotes);
            jpaRepository.save(entity);
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
                    product.getIsrc(),
                    product.getTitle(),
                    objectMapper.writeValueAsString(product.getContributors()),
                    product.getReleaseDate(),
                    product.getGenre(),
                    product.isExplicit(),
                    product.getLanguage(),
                    objectMapper.writeValueAsString(product.getOwnershipSplits()),
                    product.getAudioFileUri(),
                    product.getArtworkUri(),
                    objectMapper.writeValueAsString(product.getDspTargets()),
                    product.getStatus().name(),
                    null
            );
            return toDomain(jpaRepository.save(entity));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize product: " + product.getId(), e);
        }
    }

    @Override
    public Optional<Product> findById(UUID id) {
        return jpaRepository.findById(id.toString())
                .map(this::toDomain);
    }

    private Product toDomain(ProductEntity entity) {
        try {
            return new Product(
                    UUID.fromString(entity.getId()),
                    entity.getUpc(),
                    entity.getIsrc(),
                    entity.getTitle(),
                    objectMapper.readValue(entity.getContributors(),
                            new TypeReference<List<ProductContributor>>() {}),
                    entity.getReleaseDate(),
                    entity.getGenre(),
                    entity.isExplicit(),
                    entity.getLanguage(),
                    objectMapper.readValue(entity.getOwnershipSplits(),
                            new TypeReference<List<OwnershipSplit>>() {}),
                    entity.getAudioFileUri(),
                    entity.getArtworkUri(),
                    objectMapper.readValue(entity.getDspTargets(),
                            new TypeReference<List<String>>() {}),
                    ProductStatus.valueOf(entity.getStatus())
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize product entity: " + entity.getId(), e);
        }
    }
}