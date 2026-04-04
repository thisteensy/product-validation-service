package com.productcatalog;

import com.productcatalog.application.rest.ProductParams;
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

    public static Product invalidProduct() {
        return Product.builder()
                .id(UUID.randomUUID())
                .upc("")
                .title("")
                .tracks(List.of())
                .releaseDate(null)
                .genre(null)
                .language(null)
                .ownershipSplits(List.of())
                .artworkUri(null)
                .dspTargets(List.of())
                .status(ProductStatus.SUBMITTED)
                .build();
    }

    public static List<RuleResult> passingRuleResults() {
        return List.of(
                new RuleResult("IsrcFormatRule", RuleSeverity.PASS, "ISRC format is valid"),
                new RuleResult("TitleRule", RuleSeverity.PASS, "Title is present"),
                new RuleResult("ContributorRule", RuleSeverity.PASS, "At least one contributor present")
        );
    }

    public static List<RuleResult> warningRuleResults() {
        return List.of(
                new RuleResult("IsrcFormatRule", RuleSeverity.PASS, "ISRC format is valid"),
                new RuleResult("ArtworkRule", RuleSeverity.WARNING, "Artwork resolution below recommended 3000x3000"),
                new RuleResult("ContributorRule", RuleSeverity.PASS, "At least one contributor present")
        );
    }

    public static List<RuleResult> failingRuleResults() {
        return List.of(
                new RuleResult("IsrcFormatRule", RuleSeverity.BLOCKING, "ISRC format is invalid"),
                new RuleResult("TitleRule", RuleSeverity.BLOCKING, "Title is missing"),
                new RuleResult("ContributorRule", RuleSeverity.PASS, "At least one contributor present")
        );
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
        params.setReleaseDate(LocalDate.now().plusMonths(1));
        params.setGenre("pop");
        params.setLanguage("en");
        params.setOwnershipSplits(List.of(ownershipSplit));
        params.setArtworkUri("s3://artwork/thriller.jpg");
        params.setDspTargets(List.of("spotify", "apple_music"));
        params.setTracks(List.of(track));
        return params;
    }
}