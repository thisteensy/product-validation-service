package com.productvalidation.application.kafka;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.productvalidation.domain.model.*;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Component
public class ProductEventMapper {

    private final ObjectMapper objectMapper;

    public ProductEventMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Product toDomain(ProductEventDto.ProductRow row) {
        try {
            return new Product(
                    UUID.fromString(row.getId()),
                    row.getUpc() == null ? null : row.getUpc().strip().replace("-", "").replace(" ", ""),
                    row.getIsrc() == null ? null : row.getIsrc().strip().toUpperCase(),
                    row.getTitle() == null ? null : row.getTitle().strip(),
                    objectMapper.readValue(row.getContributors(),
                                    new TypeReference<List<ProductContributor>>() {}).stream()
                            .map(c -> new ProductContributor(
                                    c.getName() == null ? null : c.getName().strip(),
                                    c.getRole()))
                            .toList(),
                    LocalDate.ofEpochDay(row.getReleaseDate()),
                    row.getGenre() == null ? null : row.getGenre().strip(),
                    row.getExplicit() != null && row.getExplicit() == 1,
                    row.getLanguage() == null ? null : row.getLanguage().strip().toLowerCase(),
                    objectMapper.readValue(row.getOwnershipSplits(),
                                    new TypeReference<List<OwnershipSplit>>() {}).stream()
                            .map(o -> new OwnershipSplit(
                                    o.getRightsHolder() == null ? null : o.getRightsHolder().strip(),
                                    o.getPercentage()))
                            .toList(),
                    row.getAudioFileUri() == null ? null : row.getAudioFileUri().strip(),
                    row.getArtworkUri() == null ? null : row.getArtworkUri().strip(),
                    objectMapper.readValue(row.getDspTargets(),
                                    new TypeReference<List<String>>() {}).stream()
                            .map(t -> t == null ? null : t.strip().toLowerCase())
                            .toList(),
                    ProductStatus.valueOf(row.getStatus())
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to map product event: " + row.getId(), e);
        }
    }
}