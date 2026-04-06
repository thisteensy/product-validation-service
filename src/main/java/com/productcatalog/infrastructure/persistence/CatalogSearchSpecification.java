package com.productcatalog.infrastructure.persistence;

import com.productcatalog.infrastructure.persistence.entities.TrackEntity;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

public class CatalogSearchSpecification {

    public static Specification<TrackEntity> hasIsrc(String isrc) {
        return (root, query, cb) -> isrc == null ? null :
                cb.like(cb.lower(root.get("isrc")), "%" + isrc.toLowerCase(Locale.ROOT) + "%");
    }

    public static Specification<TrackEntity> hasTrackTitle(String title) {
        return (root, query, cb) -> title == null ? null :
                cb.like(cb.lower(root.get("title")), "%" + title.toLowerCase(Locale.ROOT) + "%");
    }

    public static Specification<TrackEntity> hasTrackStatus(String status) {
        return (root, query, cb) -> status == null ? null :
                cb.equal(root.get("status"), status);
    }

    public static Specification<TrackEntity> hasArtist(String artist) {
        return (root, query, cb) -> artist == null ? null :
                cb.like(cb.lower(root.join("product").get("artist")), "%" + artist.toLowerCase(Locale.ROOT) + "%");
    }

    public static Specification<TrackEntity> hasLabel(String label) {
        return (root, query, cb) -> label == null ? null :
                cb.like(cb.lower(root.join("product").get("label")), "%" + label.toLowerCase(Locale.ROOT) + "%");
    }

    public static Specification<TrackEntity> hasGenre(String genre) {
        return (root, query, cb) -> genre == null ? null :
                cb.like(cb.lower(root.join("product").get("genre")), "%" + genre.toLowerCase(Locale.ROOT) + "%");
    }
}