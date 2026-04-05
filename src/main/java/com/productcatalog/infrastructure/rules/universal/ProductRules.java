package com.productcatalog.infrastructure.rules.universal;

import com.productcatalog.domain.model.OwnershipSplit;
import com.productcatalog.domain.model.Product;
import com.productcatalog.infrastructure.rules.dsp.DspOrchestrator;
import com.productcatalog.infrastructure.rules.RuleResult;
import com.productcatalog.infrastructure.rules.RuleSeverity;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class ProductRules {

    private static final Set<String> ACCEPTED_GENRES = Set.of(
            "pop", "rock", "hip-hop", "jazz", "classical", "electronic",
            "r&b", "country", "folk", "metal", "reggae", "blues"
    );

    private final DspOrchestrator dspOrchestrator;

    public ProductRules(DspOrchestrator dspOrchestrator) {
        this.dspOrchestrator = dspOrchestrator;
    }

    public List<RuleResult> evaluate(Product product) {
        List<RuleResult> results = new ArrayList<>();

        // Release date
        if (product.getReleaseDate() == null) {
            results.add(new RuleResult("ReleaseDateRule", RuleSeverity.BLOCKING,
                    "Release date is required"));
        } else if (product.getReleaseDate().isBefore(LocalDate.now())) {
            results.add(new RuleResult("ReleaseDateRule", RuleSeverity.WARNING,
                    "Release date " + product.getReleaseDate() + " is in the past -- verify this is intentional"));
        } else {
            results.add(new RuleResult("ReleaseDateRule", RuleSeverity.PASS, "Release date is valid"));
        }

        // Genre
        if (product.getGenre() == null || !ACCEPTED_GENRES.contains(product.getGenre().toLowerCase())) {
            results.add(new RuleResult("GenreRule", RuleSeverity.WARNING,
                    "Genre '" + product.getGenre() + "' is not in the accepted genre list"));
        } else {
            results.add(new RuleResult("GenreRule", RuleSeverity.PASS, "Genre is valid"));
        }

        // Artwork
        if (product.getArtworkUri() == null || product.getArtworkUri().isBlank()) {
            results.add(new RuleResult("ArtworkRule", RuleSeverity.WARNING,
                    "Artwork is missing -- recommended for all DSPs"));
        } else {
            results.add(new RuleResult("ArtworkRule", RuleSeverity.PASS, "Artwork is present"));
        }

        // Ownership splits sum to 100%
        boolean splitsValid = product.getOwnershipSplits() != null
                && !product.getOwnershipSplits().isEmpty()
                && Math.abs(product.getOwnershipSplits().stream()
                .mapToDouble(OwnershipSplit::getPercentage)
                .sum() - 100.0) < 0.001;
        if (!splitsValid) {
            results.add(new RuleResult("OwnershipSplitRule", RuleSeverity.BLOCKING,
                    "Ownership splits must be present and sum to 100%"));
        } else {
            results.add(new RuleResult("OwnershipSplitRule", RuleSeverity.PASS,
                    "Ownership splits are valid"));
        }

        // DSP-specific product rules
        results.addAll(dspOrchestrator.evaluateProduct(product));

        return results;
    }
}