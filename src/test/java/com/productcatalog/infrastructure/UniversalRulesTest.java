package com.productcatalog.infrastructure;

import com.productcatalog.infrastructure.rules.RuleResult;
import com.productcatalog.infrastructure.rules.RuleSeverity;
import com.productcatalog.infrastructure.rules.UniversalRules;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static com.productcatalog.ValidationBuilders.validProduct;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UniversalRulesTest {

    private final UniversalRules universalRules = new UniversalRules();

    @Test
    void shouldPassWhenProductIsValid() {
        List<RuleResult> results = universalRules.evaluate(validProduct());

        assertTrue(results.stream().allMatch(r -> r.getSeverity() == RuleSeverity.PASS));
    }

    @Test
    void shouldWarnForReleaseDateGenreAndArtwork() {
        List<RuleResult> results = universalRules.evaluate(validProduct().toBuilder()
                .releaseDate(LocalDate.now().minusMonths(1))
                .genre("ambient")
                .artworkUri(null)
                .build());

        assertTrue(results.stream()
                .filter(r -> r.getRuleName().equals("ReleaseDateRule"))
                .allMatch(r -> r.getSeverity() == RuleSeverity.WARNING));

        assertTrue(results.stream()
                .filter(r -> r.getRuleName().equals("GenreRule"))
                .allMatch(r -> r.getSeverity() == RuleSeverity.WARNING));

        assertTrue(results.stream()
                .filter(r -> r.getRuleName().equals("ArtworkRule"))
                .allMatch(r -> r.getSeverity() == RuleSeverity.WARNING));
    }

    @Test
    void shouldFailWhenReleaseDateIsMissingAndAudioFormatIsInvalid() {
        List<RuleResult> results = universalRules.evaluate(validProduct().toBuilder()
                .releaseDate(null)
                .audioFileUri("s3://audio/thriller.aac")
                .build());

        assertTrue(results.stream()
                .filter(r -> r.getRuleName().equals("ReleaseDateRule"))
                .allMatch(r -> r.getSeverity() == RuleSeverity.BLOCKING));

        assertTrue(results.stream()
                .filter(r -> r.getRuleName().equals("AudioFormatRule"))
                .allMatch(r -> r.getSeverity() == RuleSeverity.BLOCKING));
    }
}