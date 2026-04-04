package com.productcatalog.infrastructure.rules;

import com.productcatalog.domain.model.Product;

import java.util.List;

public interface RuleEngine {
    List<RuleResult> evaluate(Product product);
}
