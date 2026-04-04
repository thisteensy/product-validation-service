package com.productcatalog.infrastructure.rules;

import com.productcatalog.domain.model.Product;

import java.util.ArrayList;
import java.util.List;

public class SpotifyRules implements ValidationRules {

    @Override
    public List<RuleResult> evaluate(Product product) {
        List<RuleResult> results = new ArrayList<>();

        if (product.getLanguage() == null || product.getLanguage().isBlank()) {
            results.add(new RuleResult("SpotifyLanguageRule", RuleSeverity.BLOCKING,
                    "Spotify requires language to be specified"));
        } else {
            results.add(new RuleResult("SpotifyLanguageRule", RuleSeverity.PASS,
                    "Language is set for Spotify"));
        }

        return results;
    }
}