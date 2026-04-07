package com.productcatalog.infrastructure.rules;

import com.productcatalog.domain.model.ValidationOutcome;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RuleResultTest {

    @Test
    void shouldReturnFailedWhenAnyResultIsBlocking() {
        List<RuleResult> results = List.of(
                new RuleResult("TitleRule", RuleSeverity.BLOCKING, "Title is missing"),
                new RuleResult("ArtworkRule", RuleSeverity.WARNING, "Artwork missing"),
                new RuleResult("IsrcRule", RuleSeverity.PASS, "ISRC valid")
        );

        assertThat(RuleResult.resolve(results).outcome()).isEqualTo(ValidationOutcome.FAILED);
    }

    @Test
    void shouldReturnFailedWhhenBlockingAndWarningBothPresent() {
        List<RuleResult> results = List.of(
                new RuleResult("TitleRule", RuleSeverity.BLOCKING, "Title is missing"),
                new RuleResult("ArtworkRule", RuleSeverity.WARNING, "Artwork missing")
        );

        assertThat(RuleResult.resolve(results).outcome()).isEqualTo(ValidationOutcome.FAILED);
    }

    @Test
    void shouldReturnNeedsReviewWhenWarningPresentAndNoBlocking() {
        List<RuleResult> results = List.of(
                new RuleResult("ArtworkRule", RuleSeverity.WARNING, "Artwork missing"),
                new RuleResult("IsrcRule", RuleSeverity.PASS, "ISRC valid")
        );

        assertThat(RuleResult.resolve(results).outcome()).isEqualTo(ValidationOutcome.NEEDS_REVIEW);
    }

    @Test
    void shouldReturnPassedWhenAllResultsPass() {
        List<RuleResult> results = List.of(
                new RuleResult("TitleRule", RuleSeverity.PASS, "Title present"),
                new RuleResult("IsrcRule", RuleSeverity.PASS, "ISRC valid")
        );

        assertThat(RuleResult.resolve(results).outcome()).isEqualTo(ValidationOutcome.PASSED);
    }

    @Test
    void shouldReturnPassedWhenResultsListIsEmpty() {
        assertThat(RuleResult.resolve(List.of()).outcome()).isEqualTo(ValidationOutcome.PASSED);
    }
}