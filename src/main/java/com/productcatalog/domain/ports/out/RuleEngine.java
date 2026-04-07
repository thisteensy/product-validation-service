package com.productcatalog.domain.ports.out;

import com.productcatalog.domain.model.Product;
import com.productcatalog.domain.model.Track;
import com.productcatalog.domain.model.ValidationResult;

import java.util.List;

public interface RuleEngine {
    ValidationResult evaluateProduct(Product product);
    ValidationResult evaluateTrack(Track track, List<String> dspTargets);
}