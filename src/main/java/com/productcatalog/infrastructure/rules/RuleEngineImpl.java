package com.productcatalog.infrastructure.rules;

import com.productcatalog.domain.model.Product;
import com.productcatalog.domain.model.Track;
import com.productcatalog.domain.model.ValidationResult;
import com.productcatalog.domain.ports.out.RuleEngine;
import com.productcatalog.infrastructure.rules.dsp.DspOrchestrator;
import com.productcatalog.infrastructure.rules.universal.ProductRules;
import com.productcatalog.infrastructure.rules.universal.TrackRules;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class RuleEngineImpl implements RuleEngine {

    private final ProductRules productRules;
    private final TrackRules trackRules;
    private final DspOrchestrator dspOrchestrator;

    public RuleEngineImpl(ProductRules productRules, TrackRules trackRules, DspOrchestrator dspOrchestrator) {
        this.productRules = productRules;
        this.trackRules = trackRules;
        this.dspOrchestrator = dspOrchestrator;
    }

    @Override
    public ValidationResult evaluateProduct(Product product) {
        List<RuleResult> results = new ArrayList<>();
        results.addAll(productRules.evaluate(product));
        results.addAll(dspOrchestrator.evaluateProduct(product));
        return RuleResult.resolve(results);
    }

    @Override
    public ValidationResult evaluateTrack(Track track, List<String> dspTargets) {
        List<RuleResult> results = new ArrayList<>();
        results.addAll(trackRules.evaluate(track, dspTargets));
        results.addAll(dspOrchestrator.evaluateTrack(track, dspTargets));
        return RuleResult.resolve(results);
    }
}