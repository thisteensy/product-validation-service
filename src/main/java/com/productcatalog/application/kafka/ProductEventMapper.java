package com.productcatalog.application.kafka;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.productcatalog.domain.model.*;
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

    public Product toProductFromProductRow(ProductEventDto.ProductRow row) {
        try {
            return Product.builder()
                    .id(UUID.fromString(row.getId()))
                    .upc(row.getUpc() == null ? null : row.getUpc().strip().replace("-", "").replace(" ", ""))
                    .title(row.getTitle() == null ? null : row.getTitle().strip())
                    .tracks(null)
                    .releaseDate(LocalDate.ofEpochDay(row.getReleaseDate()))
                    .genre(row.getGenre() == null ? null : row.getGenre().strip())
                    .language(row.getLanguage() == null ? null : row.getLanguage().strip().toLowerCase())
                    .ownershipSplits(row.getOwnershipSplits() == null ? null :
                            objectMapper.readValue(row.getOwnershipSplits(),
                                            new TypeReference<List<OwnershipSplit>>() {}).stream()
                                    .map(o -> new OwnershipSplit(
                                            o.getRightsHolder() == null ? null : o.getRightsHolder().strip(),
                                            o.getPercentage()))
                                    .toList())
                    .artworkUri(row.getArtworkUri() == null ? null : row.getArtworkUri().strip())
                    .dspTargets(row.getDspTargets() == null ? null :
                            objectMapper.readValue(row.getDspTargets(),
                                            new TypeReference<List<String>>() {}).stream()
                                    .map(t -> t == null ? null : t.strip().toLowerCase())
                                    .toList())
                    .status(ProductStatus.valueOf(row.getStatus()))
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to map product event: " + row.getId(), e);
        }
    }
}