package com.productcatalog.infrastructure.rules;

import com.productcatalog.domain.model.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class UniversalRules implements ValidationRules {

    private static final Set<String> ACCEPTED_AUDIO_FORMATS = Set.of("wav", "flac", "mp3", "aiff");
    private static final Set<String> ACCEPTED_GENRES = Set.of(
            "pop", "rock", "hip-hop", "jazz", "classical", "electronic",
            "r&b", "country", "folk", "metal", "reggae", "blues"
    );

    @Override
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

        // Track-level rules
        if (product.getTracks() == null || product.getTracks().isEmpty()) {
            results.add(new RuleResult("TracksRule", RuleSeverity.BLOCKING,
                    "At least one track is required"));
        } else {
            for (Track track : product.getTracks()) {
                // Audio format per track
                if (track.getAudioFileUri() != null) {
                    String extension = track.getAudioFileUri()
                            .substring(track.getAudioFileUri().lastIndexOf('.') + 1)
                            .toLowerCase();
                    if (!ACCEPTED_AUDIO_FORMATS.contains(extension)) {
                        results.add(new RuleResult("AudioFormatRule", RuleSeverity.BLOCKING,
                                "Track " + track.getTrackNumber() + ": audio format '." + extension + "' is not accepted. Accepted formats: " + ACCEPTED_AUDIO_FORMATS));
                    } else {
                        results.add(new RuleResult("AudioFormatRule", RuleSeverity.PASS,
                                "Track " + track.getTrackNumber() + ": audio format is valid"));
                    }
                }

                // At least one MAIN_ARTIST per track
                boolean hasMainArtist = track.getContributors() != null && track.getContributors().stream()
                        .anyMatch(c -> c.getRole() == ContributorRole.MAIN_ARTIST);
                if (!hasMainArtist) {
                    results.add(new RuleResult("MainArtistRule", RuleSeverity.BLOCKING,
                            "Track " + track.getTrackNumber() + ": at least one contributor must have the MAIN_ARTIST role"));
                } else {
                    results.add(new RuleResult("MainArtistRule", RuleSeverity.PASS,
                            "Track " + track.getTrackNumber() + ": main artist is present"));
                }

                // Track ownership splits sum to 100%
                boolean trackSplitsValid = track.getOwnershipSplits() != null
                        && !track.getOwnershipSplits().isEmpty()
                        && Math.abs(track.getOwnershipSplits().stream()
                        .mapToDouble(OwnershipSplit::getPercentage)
                        .sum() - 100.0) < 0.001;
                if (!trackSplitsValid) {
                    results.add(new RuleResult("TrackOwnershipSplitRule", RuleSeverity.BLOCKING,
                            "Track " + track.getTrackNumber() + ": ownership splits must be present and sum to 100%"));
                } else {
                    results.add(new RuleResult("TrackOwnershipSplitRule", RuleSeverity.PASS,
                            "Track " + track.getTrackNumber() + ": ownership splits are valid"));
                }
            }
        }

        return results;
    }
}