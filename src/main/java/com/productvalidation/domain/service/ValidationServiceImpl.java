package com.productvalidation.domain.service;

import com.productvalidation.domain.model.ContributorRole;
import com.productvalidation.domain.model.OwnershipSplit;
import com.productvalidation.domain.model.Product;
import com.productvalidation.domain.model.ProductContributor;
import com.productvalidation.domain.model.RuleResult;
import com.productvalidation.domain.model.RuleSeverity;
import com.productvalidation.domain.model.ValidationResult;
import com.productvalidation.domain.ports.RuleEngine;
import com.productvalidation.domain.ports.ValidationService;

import java.util.ArrayList;
import java.util.List;

public class ValidationServiceImpl implements ValidationService {

    private final RuleEngine ruleEngine;

    public ValidationServiceImpl(RuleEngine ruleEngine) {
        this.ruleEngine = ruleEngine;
    }

    @Override
    public ValidationResult validate(Product product) {

        List<RuleResult> inspectionResults = inspect(product);
        if (inspectionResults.stream().anyMatch(r -> r.getSeverity() == RuleSeverity.BLOCKING)) {
            return new ValidationResult(product, inspectionResults);
        }

        return new ValidationResult(product, ruleEngine.evaluate(product));
    }

    private List<RuleResult> inspect(Product product) {
        List<RuleResult> results = new ArrayList<>();

        // Title not blank
        if (product.getTitle() == null || product.getTitle().isBlank()) {
            results.add(new RuleResult("TitleRule", RuleSeverity.BLOCKING, "Title must not be blank"));
        }

        // ISRC format -- two uppercase letters, three alphanumeric, seven digits
        if (product.getIsrc() == null || !product.getIsrc().matches("[A-Z]{2}[A-Z0-9]{3}[0-9]{7}")) {
            results.add(new RuleResult("IsrcFormatRule", RuleSeverity.BLOCKING, "ISRC must match format: two uppercase letters, three alphanumeric, seven digits"));
        }

        // UPC not blank
        if (product.getUpc() == null || product.getUpc().isBlank()) {
            results.add(new RuleResult("UpcRule", RuleSeverity.BLOCKING, "UPC must not be blank"));
        }

        // At least one contributor with MAIN_ARTIST role
        boolean hasMainArtist = product.getContributors() != null && product.getContributors().stream()
                .anyMatch(c -> c.getRole() == ContributorRole.MAIN_ARTIST);
        if (!hasMainArtist) {
            results.add(new RuleResult("MainArtistRule", RuleSeverity.BLOCKING, "At least one contributor must have the MAIN_ARTIST role"));
        }

        // Ownership splits present and sum to 100%
        boolean splitsValid = product.getOwnershipSplits() != null
                && !product.getOwnershipSplits().isEmpty()
                && Math.abs(product.getOwnershipSplits().stream()
                .mapToDouble(OwnershipSplit::getPercentage)
                .sum() - 100.0) < 0.001;
        if (!splitsValid) {
            results.add(new RuleResult("OwnershipSplitRule", RuleSeverity.BLOCKING, "Ownership splits must be present and sum to 100%"));
        }

        // At least one DSP target
        if (product.getDspTargets() == null || product.getDspTargets().isEmpty()) {
            results.add(new RuleResult("DspTargetRule", RuleSeverity.BLOCKING, "At least one DSP target must be specified"));
        }

        // Audio file URI present
        if (product.getAudioFileUri() == null || product.getAudioFileUri().isBlank()) {
            results.add(new RuleResult("AudioFileRule", RuleSeverity.BLOCKING, "Audio file URI must be present"));
        }

        return results;
    }
}