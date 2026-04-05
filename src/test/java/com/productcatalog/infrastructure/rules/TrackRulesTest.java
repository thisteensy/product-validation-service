package com.productcatalog.infrastructure.rules;

import com.productcatalog.domain.model.ContributorRole;
import com.productcatalog.domain.model.OwnershipSplit;
import com.productcatalog.domain.model.ProductContributor;
import com.productcatalog.domain.model.Track;
import com.productcatalog.infrastructure.rules.dsp.DspOrchestrator;
import com.productcatalog.infrastructure.rules.universal.TrackRules;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.productcatalog.ValidationBuilders.validTrack;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrackRulesTest {

    @Mock
    private DspOrchestrator dspOrchestrator;

    private TrackRules trackRules;

    private static final List<String> DSP_TARGETS = List.of("spotify", "apple_music");

    @BeforeEach
    void setUp() {
        when(dspOrchestrator.evaluateTrack(any(), any())).thenReturn(List.of());
        trackRules = new TrackRules(dspOrchestrator);
    }

    @Test
    void shouldPassWhenTrackIsValid() {
        List<RuleResult> results = trackRules.evaluate(validTrack(), DSP_TARGETS);

        assertThat(results).noneMatch(r -> r.getSeverity() == RuleSeverity.BLOCKING);
    }

    @Test
    void shouldBlockWhenAudioFormatIsNotAccepted() {
        Track track = validTrack().toBuilder()
                .audioFileUri("s3://audio/thriller.ogg")
                .build();

        List<RuleResult> results = trackRules.evaluate(track, DSP_TARGETS);

        assertThat(results).anyMatch(r ->
                r.getRuleName().equals("AudioFormatRule") &&
                        r.getSeverity() == RuleSeverity.BLOCKING);
    }

    @Test
    void shouldPassWhenAudioFormatIsWav() {
        Track track = validTrack().toBuilder()
                .audioFileUri("s3://audio/thriller.wav")
                .build();

        List<RuleResult> results = trackRules.evaluate(track, DSP_TARGETS);

        assertThat(results).anyMatch(r ->
                r.getRuleName().equals("AudioFormatRule") &&
                        r.getSeverity() == RuleSeverity.PASS);
    }

    @Test
    void shouldPassWhenAudioFormatIsFlac() {
        Track track = validTrack().toBuilder()
                .audioFileUri("s3://audio/thriller.flac")
                .build();

        List<RuleResult> results = trackRules.evaluate(track, DSP_TARGETS);

        assertThat(results).anyMatch(r ->
                r.getRuleName().equals("AudioFormatRule") &&
                        r.getSeverity() == RuleSeverity.PASS);
    }

    @Test
    void shouldBlockWhenNoMainArtistPresent() {
        Track track = validTrack().toBuilder()
                .contributors(List.of(
                        new ProductContributor("Someone", ContributorRole.FEATURED_ARTIST)
                ))
                .build();

        List<RuleResult> results = trackRules.evaluate(track, DSP_TARGETS);

        assertThat(results).anyMatch(r ->
                r.getRuleName().equals("MainArtistRule") &&
                        r.getSeverity() == RuleSeverity.BLOCKING);
    }

    @Test
    void shouldBlockWhenContributorsAreNull() {
        Track track = validTrack().toBuilder()
                .contributors(null)
                .build();

        List<RuleResult> results = trackRules.evaluate(track, DSP_TARGETS);

        assertThat(results).anyMatch(r ->
                r.getRuleName().equals("MainArtistRule") &&
                        r.getSeverity() == RuleSeverity.BLOCKING);
    }

    @Test
    void shouldBlockWhenOwnershipSplitsAreEmpty() {
        Track track = validTrack().toBuilder()
                .ownershipSplits(List.of())
                .build();

        List<RuleResult> results = trackRules.evaluate(track, DSP_TARGETS);

        assertThat(results).anyMatch(r ->
                r.getRuleName().equals("TrackOwnershipSplitRule") &&
                        r.getSeverity() == RuleSeverity.BLOCKING);
    }

    @Test
    void shouldBlockWhenOwnershipSplitsDoNotSumToHundred() {
        Track track = validTrack().toBuilder()
                .ownershipSplits(List.of(
                        new OwnershipSplit("MJ Estate", 60.0),
                        new OwnershipSplit("Sony", 30.0)
                ))
                .build();

        List<RuleResult> results = trackRules.evaluate(track, DSP_TARGETS);

        assertThat(results).anyMatch(r ->
                r.getRuleName().equals("TrackOwnershipSplitRule") &&
                        r.getSeverity() == RuleSeverity.BLOCKING);
    }

    @Test
    void shouldBlockWhenOwnershipSplitsAreNull() {
        Track track = validTrack().toBuilder()
                .ownershipSplits(null)
                .build();

        List<RuleResult> results = trackRules.evaluate(track, DSP_TARGETS);

        assertThat(results).anyMatch(r ->
                r.getRuleName().equals("TrackOwnershipSplitRule") &&
                        r.getSeverity() == RuleSeverity.BLOCKING);
    }
}