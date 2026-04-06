package com.productcatalog.infrastructure.persistence;

import com.productcatalog.infrastructure.persistence.entities.ProductEntity;
import org.springframework.data.jpa.domain.Specification;

public class ProductSpecifications {

    public static Specification<ProductEntity> hasArtist(String artist) {
        return (root, query, cb) -> artist == null ? null :
                cb.like(cb.lower(root.get("artist")), "%" + artist.toLowerCase() + "%");
    }

    public static Specification<ProductEntity> hasLabel(String label) {
        return (root, query, cb) -> label == null ? null :
                cb.like(cb.lower(root.get("label")), "%" + label.toLowerCase() + "%");
    }

    public static Specification<ProductEntity> hasGenre(String genre) {
        return (root, query, cb) -> genre == null ? null :
                cb.like(cb.lower(root.get("genre")), "%" + genre.toLowerCase() + "%");
    }

    public static Specification<ProductEntity> hasStatus(String status) {
        return (root, query, cb) -> status == null ? null :
                cb.equal(root.get("status"), status);
    }
}