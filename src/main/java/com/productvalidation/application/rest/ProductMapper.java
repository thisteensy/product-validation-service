package com.productvalidation.application.rest;

import com.productvalidation.domain.model.Product;
import com.productvalidation.domain.model.ProductContributor;
import com.productvalidation.domain.model.OwnershipSplit;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public Product toProductFromProductParams(ProductParams params) {
        return Product.builder()
                .upc(params.getUpc() == null ? null : params.getUpc().strip().replace("-", "").replace(" ", ""))
                .isrc(params.getIsrc() == null ? null : params.getIsrc().strip().toUpperCase())
                .title(params.getTitle() == null ? null : params.getTitle().strip())
                .genre(params.getGenre() == null ? null : params.getGenre().strip())
                .language(params.getLanguage() == null ? null : params.getLanguage().strip().toLowerCase())
                .audioFileUri(params.getAudioFileUri() == null ? null : params.getAudioFileUri().strip())
                .artworkUri(params.getArtworkUri() == null ? null : params.getArtworkUri().strip())
                .explicit(params.isExplicit())
                .releaseDate(params.getReleaseDate())
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
                .dspTargets(params.getDspTargets() == null ? null : params.getDspTargets().stream()
                        .map(t -> t == null ? null : t.strip().toLowerCase())
                        .toList())
                .build();
    }
}