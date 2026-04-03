package com.productvalidation.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RuleResult {

    private final String ruleName;
    private final RuleSeverity severity;
    private final String message;

}