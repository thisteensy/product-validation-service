package com.productvalidation;

import com.productvalidation.application.rest.ProductParams;
import com.productvalidation.domain.model.ContributorRole;
import com.productvalidation.domain.model.OwnershipSplit;
import com.productvalidation.domain.model.Product;
import com.productvalidation.domain.model.ProductContributor;
import com.productvalidation.domain.model.ProductStatus;
import com.productvalidation.domain.model.RuleResult;
import com.productvalidation.domain.model.RuleSeverity;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class ValidationBuilders {

    public static Product validProduct() {
        return Product.builder()
                .id(UUID.randomUUID())
                .upc("012345678905")
                .isrc("USRC17607839")
                .title("Thriller")
                .contributors(List.of(new ProductContributor("Michael Jackson", ContributorRole.MAIN_ARTIST)))
                .releaseDate(LocalDate.now().plusMonths(1))
                .genre("pop")
                .explicit(false)
                .language("en")
                .ownershipSplits(List.of(new OwnershipSplit("MJ Estate", 100.0)))
                .audioFileUri("s3://audio/thriller.wav")
                .artworkUri("s3://artwork/thriller.jpg")
                .dspTargets(List.of("spotify", "apple_music"))
                .status(ProductStatus.SUBMITTED)
                .build();
    }

    public static Product invalidProduct() {
        return Product.builder()
                .id(UUID.randomUUID())
                .upc("")
                .isrc("FAKE12345")
                .title("")
                .contributors(List.of())
                .releaseDate(null)
                .genre(null)
                .explicit(false)
                .language(null)
                .ownershipSplits(List.of())
                .audioFileUri(null)
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

        ProductParams params = new ProductParams();
        params.setUpc("012345678905");
        params.setIsrc("USRC17607839");
        params.setTitle("Thriller");
        params.setContributors(List.of(contributor));
        params.setReleaseDate(LocalDate.now().plusMonths(1));
        params.setGenre("pop");
        params.setExplicit(false);
        params.setLanguage("en");
        params.setOwnershipSplits(List.of(ownershipSplit));
        params.setAudioFileUri("s3://audio/thriller.wav");
        params.setArtworkUri("s3://artwork/thriller.jpg");
        params.setDspTargets(List.of("spotify", "apple_music"));
        return params;
    }

}