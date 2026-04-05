package com.productcatalog.domain.ports.out;

import com.productcatalog.domain.model.Product;
import com.productcatalog.domain.model.Track;
import com.productcatalog.domain.model.ValidationOutcome;

import java.util.List;

public interface RuleEngine {
    ValidationOutcome evaluateProduct(Product product);
    ValidationOutcome evaluateTrack(Track track, List<String> dspTargets);
}