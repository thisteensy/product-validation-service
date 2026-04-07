package com.productcatalog.infrastructure.rules;

import com.productcatalog.domain.model.ValidationOutcome;
import com.productcatalog.domain.model.ValidationResult;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class RuleResult {

    private final String ruleName;
    private final RuleSeverity severity;
    private final String message;

    public static ValidationResult resolve(List<RuleResult> results) {
        List<String> violations = results.stream()
                .filter(r -> r.getSeverity() != RuleSeverity.PASS)
                .map(RuleResult::getMessage)
                .toList();

        if (results.stream().anyMatch(r -> r.getSeverity() == RuleSeverity.BLOCKING)) {
            return new ValidationResult(ValidationOutcome.FAILED, violations);
        }
        if (results.stream().anyMatch(r -> r.getSeverity() == RuleSeverity.WARNING)) {
            return new ValidationResult(ValidationOutcome.NEEDS_REVIEW, violations);
        }
        return new ValidationResult(ValidationOutcome.PASSED, violations);
    }
}