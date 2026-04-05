package com.productcatalog.infrastructure.rules;

import com.productcatalog.infrastructure.rules.dsp.SpotifyRules;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.productcatalog.ValidationBuilders.validProduct;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SpotifyRulesTest {

    private final SpotifyRules spotifyRules = new SpotifyRules();

    @Test
    void shouldPassWhenProductIsValid() {
        List<RuleResult> results = spotifyRules.evaluateProduct(validProduct());

        assertTrue(results.stream().allMatch(r -> r.getSeverity() == RuleSeverity.PASS));
    }

    @Test
    void shouldFailWhenLanguageIsMissing() {
        List<RuleResult> results = spotifyRules.evaluateProduct(validProduct().toBuilder()
                .language(null)
                .build());

        assertTrue(results.stream()
                .filter(r -> r.getRuleName().equals("SpotifyLanguageRule"))
                .allMatch(r -> r.getSeverity() == RuleSeverity.BLOCKING));
    }
}