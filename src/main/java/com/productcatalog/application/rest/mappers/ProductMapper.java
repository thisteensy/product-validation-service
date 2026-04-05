package com.productcatalog.application.rest.mappers;

import com.productcatalog.application.rest.params.ProductParams;
import com.productcatalog.domain.model.*;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ProductMapper {

    public Product toProductFromProductParams(ProductParams params) {
        return Product.builder()
                .upc(params.getUpc() == null ? null : params.getUpc().strip().replace("-", "").replace(" ", ""))
                .title(params.getTitle() == null ? null : params.getTitle().strip())
                .releaseDate(params.getReleaseDate())
                .genre(params.getGenre() == null ? null : params.getGenre().strip())
                .language(params.getLanguage() == null ? null : params.getLanguage().strip().toLowerCase())
                .artworkUri(params.getArtworkUri() == null ? null : params.getArtworkUri().strip())
                .ownershipSplits(params.getOwnershipSplits() == null ? null : params.getOwnershipSplits().stream()
                        .map(o -> new OwnershipSplit(
                                o.getRightsHolder() == null ? null : o.getRightsHolder().strip(),
                                o.getPercentage()))
                        .toList())
                .dspTargets(params.getDspTargets() == null ? null : params.getDspTargets().stream()
                        .map(t -> t == null ? null : t.strip().toLowerCase())
                        .toList())
                .tracks(params.getTracks() == null ? null : params.getTracks().stream()
                        .map(this::toTrackFromTrackParams)
                        .toList())
                .build();
    }

    private Track toTrackFromTrackParams(ProductParams.TrackParams params) {
        return Track.builder()
                .id(UUID.randomUUID())
                .isrc(params.getIsrc() == null ? null : params.getIsrc().strip().toUpperCase())
                .title(params.getTitle() == null ? null : params.getTitle().strip())
                .trackNumber(params.getTrackNumber())
                .audioFileUri(params.getAudioFileUri() == null ? null : params.getAudioFileUri().strip())
                .duration(params.getDuration())
                .explicit(params.isExplicit())
                .contributors(params.getContributors() == null ? null : params.getContributors().stream()
                        .map(c -> new ProductContributor(
                                c.getName() == null ? null : c.getName().strip(),
                                c.getRole()))
                        .toList())
                .ownershipSplits(params.getOwnershipSplits() == null ? null : params.getOwnershipSplits().stream()
                        .map(o -> new OwnershipSplit(
                                o.getRightsHolder() == null ? null : o.getRightsHolder().strip(),
                                o.getPercentage()))
                        .toList())
                .status(TrackStatus.PENDING)
                .build();
    }
}