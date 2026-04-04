package com.productcatalog.infrastructure.rules;

import com.productcatalog.domain.model.ContributorRole;
import com.productcatalog.domain.model.OwnershipSplit;
import com.productcatalog.domain.model.Track;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class TrackRules {

    private static final Set<String> ACCEPTED_AUDIO_FORMATS = Set.of("wav", "flac", "mp3", "aiff");

    private final DspOrchestrator dspOrchestrator;

    public TrackRules(DspOrchestrator dspOrchestrator) {
        this.dspOrchestrator = dspOrchestrator;
    }

    public List<RuleResult> evaluate(Track track, List<String> dspTargets) {
        List<RuleResult> results = new ArrayList<>();

        // Audio format
        if (track.getAudioFileUri() != null) {
            String extension = track.getAudioFileUri()
                    .substring(track.getAudioFileUri().lastIndexOf('.') + 1)
                    .toLowerCase();
            if (!ACCEPTED_AUDIO_FORMATS.contains(extension)) {
                results.add(new RuleResult("AudioFormatRule", RuleSeverity.BLOCKING,
                        "Audio format '." + extension + "' is not accepted. Accepted formats: " + ACCEPTED_AUDIO_FORMATS));
            } else {
                results.add(new RuleResult("AudioFormatRule", RuleSeverity.PASS,
                        "Audio format is valid"));
            }
        }

        // At least one MAIN_ARTIST
        boolean hasMainArtist = track.getContributors() != null && track.getContributors().stream()
                .anyMatch(c -> c.getRole() == ContributorRole.MAIN_ARTIST);
        if (!hasMainArtist) {
            results.add(new RuleResult("MainArtistRule", RuleSeverity.BLOCKING,
                    "At least one contributor must have the MAIN_ARTIST role"));
        } else {
            results.add(new RuleResult("MainArtistRule", RuleSeverity.PASS,
                    "Main artist is present"));
        }

        // Ownership splits sum to 100%
        boolean splitsValid = track.getOwnershipSplits() != null
                && !track.getOwnershipSplits().isEmpty()
                && Math.abs(track.getOwnershipSplits().stream()
                .mapToDouble(OwnershipSplit::getPercentage)
                .sum() - 100.0) < 0.001;
        if (!splitsValid) {
            results.add(new RuleResult("TrackOwnershipSplitRule", RuleSeverity.BLOCKING,
                    "Ownership splits must be present and sum to 100%"));
        } else {
            results.add(new RuleResult("TrackOwnershipSplitRule", RuleSeverity.PASS,
                    "Ownership splits are valid"));
        }

        // DSP-specific track rules
        results.addAll(dspOrchestrator.evaluateTrack(track, dspTargets));

        return results;
    }
}