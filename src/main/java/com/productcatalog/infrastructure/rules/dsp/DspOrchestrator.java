package com.productcatalog.infrastructure.rules.dsp;

import com.productcatalog.domain.model.Product;
import com.productcatalog.domain.model.Track;
import com.productcatalog.infrastructure.rules.RuleResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DspOrchestrator {

    private final List<DspRuleSet> dspRuleSets;

    public DspOrchestrator(List<DspRuleSet> dspRuleSets) {
        this.dspRuleSets = dspRuleSets;
    }

    public List<RuleResult> evaluateProduct(Product product) {
        List<RuleResult> results = new ArrayList<>();
        if (product.getDspTargets() == null) return results;

        dspRuleSets.stream()
                .filter(rs -> product.getDspTargets().contains(rs.dspName()))
                .forEach(rs -> results.addAll(rs.evaluateProduct(product)));

        return results;
    }

    public List<RuleResult> evaluateTrack(Track track, List<String> dspTargets) {
        List<RuleResult> results = new ArrayList<>();
        if (dspTargets == null) return results;

        dspRuleSets.stream()
                .filter(rs -> dspTargets.contains(rs.dspName()))
                .forEach(rs -> results.addAll(rs.evaluateTrack(track)));

        return results;
    }
}