package com.productvalidation.domain.service;

import com.productvalidation.ValidationBuilders;
import com.productvalidation.domain.model.Product;
import com.productvalidation.domain.model.RuleSeverity;
import com.productvalidation.domain.model.ValidationResult;
import com.productvalidation.domain.ports.RuleEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidationServiceImplTest {

    @Mock
    private RuleEngine ruleEngine;

    private ValidationServiceImpl validationService;

    @BeforeEach
    void setUp() {
        validationService = new ValidationServiceImpl(ruleEngine);
    }

    @Test
    void shouldReturnValidResultWhenAllRulesPass() {
        Product product = ValidationBuilders.validProduct();
        when(ruleEngine.evaluate(any())).thenReturn(ValidationBuilders.passingRuleResults());

        ValidationResult result = validationService.validate(product);

        assertTrue(result.isValid());
    }

    @Test
    void shouldRequireReviewWhenRulesProduceWarnings() {
        Product product = ValidationBuilders.validProduct();
        when(ruleEngine.evaluate(any())).thenReturn(ValidationBuilders.warningRuleResults());

        ValidationResult result = validationService.validate(product);

        assertTrue(result.needsReview());
    }

    @Test
    void shouldReturnFailureWhenRulesProduceBlockingResults() {
        Product product = ValidationBuilders.validProduct();
        when(ruleEngine.evaluate(any())).thenReturn(ValidationBuilders.failingRuleResults());

        ValidationResult result = validationService.validate(product);

        assertFalse(result.isValid());
    }

    @Test
    void shouldReturnBlockingResultsWithoutCallingRuleEngineWhenInputsAreInvalid() {
        Product product = ValidationBuilders.invalidProduct();

        ValidationResult result = validationService.validate(product);

        assertFalse(result.isValid());
        assertTrue(result.getRuleResults().stream()
                .allMatch(r -> r.getSeverity() == RuleSeverity.BLOCKING));
        verify(ruleEngine, never()).evaluate(any());
    }

}