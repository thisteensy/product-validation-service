package com.productcatalog.infrastructure.rules;

import com.productcatalog.domain.model.Product;
import com.productcatalog.domain.model.ProductStatus;

import java.util.List;

public class ValidationResult {

    private final Product product;
    private final List<RuleResult> ruleResults;
    private final ProductStatus status;

    public ValidationResult(Product product, List<RuleResult> ruleResults) {
        this.product = product;
        this.ruleResults = ruleResults;
        this.status = deriveStatus(ruleResults);
    }

    private ProductStatus deriveStatus(List<RuleResult> ruleResults) {
        if (ruleResults.stream().anyMatch(r -> r.getSeverity() == RuleSeverity.BLOCKING)) {
            return ProductStatus.VALIDATION_FAILED;
        }
        if (ruleResults.stream().anyMatch(r -> r.getSeverity() == RuleSeverity.WARNING)) {
            return ProductStatus.NEEDS_REVIEW;
        }
        return ProductStatus.VALIDATED;
    }

    public boolean isValid() { return status == ProductStatus.VALIDATED; }
    public boolean needsReview() { return status == ProductStatus.NEEDS_REVIEW; }

    public Product getProduct() { return product; }
    public List<RuleResult> getRuleResults() { return ruleResults; }
    public ProductStatus getStatus() { return status; }
}