package com.productcatalog;

import com.productcatalog.application.kafka.dtos.ProductEventDto;
import com.productcatalog.application.kafka.dtos.TrackEventDto;
import com.productcatalog.application.rest.params.ProductParams;
import com.productcatalog.domain.model.*;
import com.productcatalog.infrastructure.rules.RuleResult;
import com.productcatalog.infrastructure.rules.RuleSeverity;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class ValidationBuilders {

    public static Track validTrack() {
        return Track.builder()
                .id(UUID.randomUUID())
                .isrc("USRC17607839")
                .title("Thriller")
                .trackNumber(1)
                .audioFileUri("s3://audio/thriller.wav")
                .duration(358)
                .explicit(false)
                .contributors(List.of(new ProductContributor("Michael Jackson", ContributorRole.MAIN_ARTIST)))
                .ownershipSplits(List.of(new OwnershipSplit("MJ Estate", 100.0)))
                .status(TrackStatus.PENDING)
                .build();
    }

    public static Product validProduct() {
        return Product.builder()
                .id(UUID.randomUUID())
                .upc("012345678905")
                .title("Thriller")
                .artist("Michael Jackson")
                .label("Epic Records")
                .tracks(List.of(validTrack()))
                .releaseDate(LocalDate.now().plusMonths(1))
                .genre("pop")
                .language("en")
                .ownershipSplits(List.of(new OwnershipSplit("MJ Estate", 100.0)))
                .artworkUri("s3://artwork/thriller.jpg")
                .dspTargets(List.of("spotify", "apple_music"))
                .status(ProductStatus.SUBMITTED)
                .build();
    }

    public static ProductParams validProductParams() {
        ProductParams.ContributorParams contributor = new ProductParams.ContributorParams();
        contributor.setName("Michael Jackson");
        contributor.setRole(ContributorRole.MAIN_ARTIST);

        ProductParams.OwnershipSplitParams ownershipSplit = new ProductParams.OwnershipSplitParams();
        ownershipSplit.setRightsHolder("MJ Estate");
        ownershipSplit.setPercentage(100.0);

        ProductParams.TrackParams track = new ProductParams.TrackParams();
        track.setIsrc("USRC17607839");
        track.setTitle("Thriller");
        track.setTrackNumber(1);
        track.setAudioFileUri("s3://audio/thriller.wav");
        track.setDuration(358);
        track.setExplicit(false);
        track.setContributors(List.of(contributor));
        track.setOwnershipSplits(List.of(ownershipSplit));

        ProductParams params = new ProductParams();
        params.setUpc("012345678905");
        params.setTitle("Thriller");
        params.setArtist("Michael Jackson");
        params.setLabel("Epic Records");
        params.setReleaseDate(LocalDate.now().plusMonths(1));
        params.setGenre("pop");
        params.setLanguage("en");
        params.setOwnershipSplits(List.of(ownershipSplit));
        params.setArtworkUri("s3://artwork/thriller.jpg");
        params.setDspTargets(List.of("spotify", "apple_music"));
        params.setTracks(List.of(track));
        return params;
    }

    public static ProductEventDto.ProductRow validProductRow() {
        ProductEventDto.ProductRow row = new ProductEventDto.ProductRow();
        row.setId("00000000-0000-0000-0000-000000000001");
        row.setUpc(" 012-345-678-905 ");
        row.setTitle(" Thriller ");
        row.setReleaseDate(20000);
        row.setGenre(" Pop ");
        row.setLanguage(" EN ");
        row.setArtworkUri(" s3://artwork/thriller.jpg ");
        row.setDspTargets("[\"Spotify\",\"Apple_Music\"]");
        row.setOwnershipSplits("[{\"rightsHolder\":\" MJ Estate \",\"percentage\":100.0}]");
        row.setStatus("SUBMITTED");
        return row;
    }

    public static TrackEventDto.TrackRow validTrackRow() {
        TrackEventDto.TrackRow row = new TrackEventDto.TrackRow();
        row.setId("00000000-0000-0000-0000-000000000002");
        row.setProductId("00000000-0000-0000-0000-000000000001");
        row.setIsrc(" usrc17607839 ");
        row.setTitle(" Thriller ");
        row.setTrackNumber(1);
        row.setAudioFileUri(" s3://audio/thriller.wav ");
        row.setDuration(358);
        row.setExplicit(0);
        row.setContributors("[{\"name\":\" Michael Jackson \",\"role\":\"MAIN_ARTIST\"}]");
        row.setOwnershipSplits("[{\"rightsHolder\":\" MJ Estate \",\"percentage\":100.0}]");
        row.setStatus("PENDING");
        return row;
    }

    public static CatalogSearchResult validCatalogSearchResult() {
        return new CatalogSearchResult(
                UUID.randomUUID(),
                "USRC17607839",
                "Thriller",
                TrackStatus.VALIDATED,
                UUID.randomUUID(),
                "Thriller",
                "Michael Jackson",
                "Epic Records",
                "pop",
                ProductStatus.VALIDATED
        );
    }
}