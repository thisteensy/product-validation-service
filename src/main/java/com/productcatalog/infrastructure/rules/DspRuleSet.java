package com.productcatalog.infrastructure.rules;

import com.productcatalog.domain.model.Product;
import com.productcatalog.domain.model.Track;

import java.util.List;

public interface DspRuleSet {
    String dspName();
    List<RuleResult> evaluateProduct(Product product);
    List<RuleResult> evaluateTrack(Track track);
}