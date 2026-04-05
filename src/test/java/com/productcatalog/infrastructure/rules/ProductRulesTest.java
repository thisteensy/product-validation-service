package com.productcatalog.infrastructure.rules;

import com.productcatalog.domain.model.Product;
import com.productcatalog.infrastructure.rules.dsp.DspOrchestrator;
import com.productcatalog.infrastructure.rules.universal.ProductRules;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static com.productcatalog.ValidationBuilders.validProduct;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductRulesTest {

    @Mock
    private DspOrchestrator dspOrchestrator;

    private ProductRules productRules;

    @BeforeEach
    void setUp() {
        when(dspOrchestrator.evaluateProduct(any())).thenReturn(List.of());
        productRules = new ProductRules(dspOrchestrator);
    }

    @Test
    void shouldPassWhenProductIsValid() {
        List<RuleResult> results = productRules.evaluate(validProduct());

        assertThat(results).noneMatch(r -> r.getSeverity() == RuleSeverity.BLOCKING);
    }

    @Test
    void shouldBlockWhenReleaseDateIsNull() {
        Product product = validProduct().toBuilder().releaseDate(null).build();

        List<RuleResult> results = productRules.evaluate(product);

        assertThat(results).anyMatch(r ->
                r.getRuleName().equals("ReleaseDateRule") &&
                        r.getSeverity() == RuleSeverity.BLOCKING);
    }

    @Test
    void shouldWarnWhenReleaseDateIsInThePast() {
        Product product = validProduct().toBuilder()
                .releaseDate(LocalDate.now().minusDays(1))
                .build();

        List<RuleResult> results = productRules.evaluate(product);

        assertThat(results).anyMatch(r ->
                r.getRuleName().equals("ReleaseDateRule") &&
                        r.getSeverity() == RuleSeverity.WARNING);
    }

    @Test
    void shouldWarnWhenGenreIsNotAccepted() {
        Product product = validProduct().toBuilder().genre("dubstep").build();

        List<RuleResult> results = productRules.evaluate(product);

        assertThat(results).anyMatch(r ->
                r.getRuleName().equals("GenreRule") &&
                        r.getSeverity() == RuleSeverity.WARNING);
    }

    @Test
    void shouldWarnWhenGenreIsNull() {
        Product product = validProduct().toBuilder().genre(null).build();

        List<RuleResult> results = productRules.evaluate(product);

        assertThat(results).anyMatch(r ->
                r.getRuleName().equals("GenreRule") &&
                        r.getSeverity() == RuleSeverity.WARNING);
    }

    @Test
    void shouldWarnWhenArtworkIsNull() {
        Product product = validProduct().toBuilder().artworkUri(null).build();

        List<RuleResult> results = productRules.evaluate(product);

        assertThat(results).anyMatch(r ->
                r.getRuleName().equals("ArtworkRule") &&
                        r.getSeverity() == RuleSeverity.WARNING);
    }

    @Test
    void shouldBlockWhenOwnershipSplitsAreEmpty() {
        Product product = validProduct().toBuilder().ownershipSplits(List.of()).build();

        List<RuleResult> results = productRules.evaluate(product);

        assertThat(results).anyMatch(r ->
                r.getRuleName().equals("OwnershipSplitRule") &&
                        r.getSeverity() == RuleSeverity.BLOCKING);
    }

    @Test
    void shouldBlockWhenOwnershipSplitsDoNotSumToHundred() {
        Product product = validProduct().toBuilder()
                .ownershipSplits(List.of(
                        new com.productcatalog.domain.model.OwnershipSplit("MJ Estate", 60.0),
                        new com.productcatalog.domain.model.OwnershipSplit("Sony", 30.0)
                ))
                .build();

        List<RuleResult> results = productRules.evaluate(product);

        assertThat(results).anyMatch(r ->
                r.getRuleName().equals("OwnershipSplitRule") &&
                        r.getSeverity() == RuleSeverity.BLOCKING);
    }
}