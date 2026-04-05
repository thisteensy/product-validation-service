package com.productcatalog.infrastructure.rules;

import com.productcatalog.domain.model.Product;
import com.productcatalog.domain.model.Track;
import com.productcatalog.domain.model.ValidationOutcome;
import com.productcatalog.domain.ports.out.RuleEngine;
import com.productcatalog.infrastructure.rules.universal.ProductRules;
import com.productcatalog.infrastructure.rules.universal.TrackRules;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RuleEngineImpl implements RuleEngine {

    private final ProductRules productRules;
    private final TrackRules trackRules;

    public RuleEngineImpl(ProductRules productRules, TrackRules trackRules) {
        this.productRules = productRules;
        this.trackRules = trackRules;
    }

    @Override
    public ValidationOutcome evaluateProduct(Product product) {
        return RuleResult.resolve(productRules.evaluate(product));
    }

    @Override
    public ValidationOutcome evaluateTrack(Track track, List<String> dspTargets) {
        return RuleResult.resolve(trackRules.evaluate(track, dspTargets));
    }
}