package com.productcatalog.infrastructure.rules.dsp;

import com.productcatalog.domain.model.Product;
import com.productcatalog.domain.model.Track;
import com.productcatalog.infrastructure.rules.RuleResult;

import java.util.List;

public interface DspRuleSet {
    String dspName();
    List<RuleResult> evaluateProduct(Product product);
    List<RuleResult> evaluateTrack(Track track);
}