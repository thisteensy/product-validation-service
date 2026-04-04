package com.productcatalog.infrastructure.rules;

import com.productcatalog.domain.model.Product;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class RuleEngineImpl implements RuleEngine {

    private final List<ValidationRules> universalRules;
    private final Map<String, ValidationRules> dspRules;

    public RuleEngineImpl(List<ValidationRules> universalRules,
                          Map<String, ValidationRules> dspRules) {
        this.universalRules = universalRules;
        this.dspRules = dspRules;
    }

    @Override
    public List<RuleResult> evaluate(Product product) {
        List<RuleResult> results = new ArrayList<>();

        // run universal rules
        universalRules.forEach(rules -> results.addAll(rules.evaluate(product)));

        // run DSP-specific rules for each target
        product.getDspTargets().forEach(dsp -> {
            ValidationRules rules = dspRules.get(dsp.toLowerCase());
            if (rules != null) {
                results.addAll(rules.evaluate(product));
            }
        });

        return results;
    }
}