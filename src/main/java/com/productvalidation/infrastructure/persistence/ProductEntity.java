package com.productvalidation.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "products")
public class ProductEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "upc")
    private String upc;

    @Column(name = "isrc")
    private String isrc;

    @Column(name = "title")
    private String title;

    @Column(name = "contributors", columnDefinition = "JSON")
    private String contributors;

    @Column(name = "release_date")
    private LocalDate releaseDate;

    @Column(name = "genre")
    private String genre;

    @Column(name = "explicit")
    private boolean explicit;

    @Column(name = "language")
    private String language;

    @Column(name = "ownership_splits", columnDefinition = "JSON")
    private String ownershipSplits;

    @Column(name = "audio_file_uri")
    private String audioFileUri;

    @Column(name = "artwork_uri")
    private String artworkUri;

    @Column(name = "dsp_targets", columnDefinition = "JSON")
    private String dspTargets;

    @Column(name = "status")
    private String status;
}