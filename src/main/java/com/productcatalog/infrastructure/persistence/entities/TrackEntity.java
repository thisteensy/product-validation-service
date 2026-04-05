package com.productcatalog.infrastructure.persistence.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tracks")
public class TrackEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    @Column(name = "isrc", nullable = false)
    private String isrc;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "track_number", nullable = false)
    private int trackNumber;

    @Column(name = "audio_file_uri", nullable = false)
    private String audioFileUri;

    @Column(name = "duration", nullable = false)
    private int duration;

    @Column(name = "explicit", nullable = false)
    private boolean explicit;

    @Column(name = "contributors", columnDefinition = "JSON")
    private String contributors;

    @Column(name = "ownership_splits", columnDefinition = "JSON")
    private String ownershipSplits;

    @Column(name = "status", nullable = false)
    private String status;
}