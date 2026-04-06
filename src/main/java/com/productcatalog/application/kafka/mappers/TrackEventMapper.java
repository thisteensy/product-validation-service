package com.productcatalog.application.kafka.mappers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.productcatalog.application.kafka.dtos.TrackEventDto;
import com.productcatalog.domain.model.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Component
public class TrackEventMapper {

    private final ObjectMapper objectMapper;

    public TrackEventMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public TrackEventDto toDto(String message) {
        try {
            return objectMapper.readValue(message, TrackEventDto.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize track event", e);
        }
    }

    public Track toTrackFromTrackRow(TrackEventDto.TrackRow row) {
        try {
            return Track.builder()
                    .id(UUID.fromString(row.getId()))
                    .isrc(row.getIsrc() == null ? null : row.getIsrc().strip().toUpperCase(Locale.ROOT))
                    .title(row.getTitle() == null ? null : row.getTitle().strip())
                    .trackNumber(row.getTrackNumber() == null ? 0 : row.getTrackNumber())
                    .audioFileUri(row.getAudioFileUri() == null ? null : row.getAudioFileUri().strip())
                    .duration(row.getDuration() == null ? 0 : row.getDuration())
                    .explicit(row.getExplicit() != null && row.getExplicit() == 1)
                    .contributors(row.getContributors() == null ? null :
                            objectMapper.readValue(row.getContributors(),
                                            new TypeReference<List<ProductContributor>>() {}).stream()
                                    .map(c -> new ProductContributor(
                                            c.getName() == null ? null : c.getName().strip(),
                                            c.getRole()))
                                    .toList())
                    .ownershipSplits(row.getOwnershipSplits() == null ? null :
                            objectMapper.readValue(row.getOwnershipSplits(),
                                            new TypeReference<List<OwnershipSplit>>() {}).stream()
                                    .map(o -> new OwnershipSplit(
                                            o.getRightsHolder() == null ? null : o.getRightsHolder().strip(),
                                            o.getPercentage()))
                                    .toList())
                    .status(TrackStatus.valueOf(row.getStatus()))
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to map track event: " + row.getId(), e);
        }
    }
}