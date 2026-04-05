package com.productcatalog.infrastructure.rules.dsp;

import com.productcatalog.domain.model.Product;
import com.productcatalog.domain.model.Track;
import com.productcatalog.infrastructure.rules.RuleResult;
import com.productcatalog.infrastructure.rules.RuleSeverity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class SpotifyRules implements DspRuleSet {

    @Override
    public String dspName() {
        return "spotify";
    }

    @Override
    public List<RuleResult> evaluateProduct(Product product) {
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

    @Override
    public List<RuleResult> evaluateTrack(Track track) {
        // No Spotify-specific track rules at this time
        return List.of();
    }
}