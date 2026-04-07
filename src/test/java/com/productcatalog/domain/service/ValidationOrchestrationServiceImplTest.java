package com.productcatalog.domain.service;

import com.productcatalog.ValidationBuilders;
import com.productcatalog.domain.model.*;
import com.productcatalog.domain.ports.out.RuleEngine;
import com.productcatalog.domain.ports.out.StatusUpdatePublisher;
import com.productcatalog.domain.ports.out.ValidationStateStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ValidationOrchestrationServiceImplTest {

    @Mock
    private StatusUpdatePublisher statusUpdatePublisher;

    @Mock
    private RuleEngine ruleEngine;

    @Mock
    private ValidationStateStore validationStateStore;

    private ValidationOrchestrationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ValidationOrchestrationServiceImpl(ruleEngine, statusUpdatePublisher, validationStateStore);
    }

    @Test
    void shouldSubmitProductAndEvaluate() {
        Product product = ValidationBuilders.validProduct();
        when(ruleEngine.evaluateProduct(product)).thenReturn(ValidationBuilders.validationPassed());

        service.submitProduct(product);

        verify(ruleEngine).evaluateProduct(product);
        verify(statusUpdatePublisher).publish(argThat(event ->
                event.getStatus().equals(ProductStatus.AWAITING_TRACK_VALIDATION.name())
        ));
    }

    @Test
    void shouldSubmitTrackAndEvaluate() {
        Track track = ValidationBuilders.validTrack();
        UUID productId = UUID.randomUUID();
        when(validationStateStore.getDspTargets(productId)).thenReturn(List.of("SPOTIFY"));
        when(ruleEngine.evaluateTrack(track, List.of("SPOTIFY"))).thenReturn(ValidationBuilders.validationPassed());

        service.submitTrack(track, productId);

        verify(ruleEngine).evaluateTrack(track, List.of("SPOTIFY"));
        verify(statusUpdatePublisher).publish(argThat(event ->
                event.getStatus().equals(TrackStatus.VALIDATED.name())
        ));
    }

    @Test
    void shouldPublishValidationFailedWhenOutcomeIsFailed() {
        UUID productId = UUID.randomUUID();

        service.onProductEvaluated(productId, ValidationOutcome.FAILED, List.of("Artwork is missing"));

        ArgumentCaptor<StatusUpdateEvent> captor = ArgumentCaptor.forClass(StatusUpdateEvent.class);
        verify(statusUpdatePublisher).publish(captor.capture());
        assertThat(captor.getValue().getEntityType()).isEqualTo(StatusUpdateEvent.EntityType.PRODUCT);
        assertThat(captor.getValue().getEntityId()).isEqualTo(productId.toString());
        assertThat(captor.getValue().getStatus()).isEqualTo(ProductStatus.VALIDATION_FAILED.name());
        assertThat(captor.getValue().getNotes()).isEqualTo("Artwork is missing");
    }

    @Test
    void shouldPublishNeedsReviewWhenOutcomeIsNeedsReview() {
        UUID productId = UUID.randomUUID();

        service.onProductEvaluated(productId, ValidationOutcome.NEEDS_REVIEW, List.of("Release date is in the past."));

        ArgumentCaptor<StatusUpdateEvent> captor = ArgumentCaptor.forClass(StatusUpdateEvent.class);
        verify(statusUpdatePublisher).publish(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ProductStatus.NEEDS_REVIEW.name());
        assertThat(captor.getValue().getNotes()).isEqualTo("Release date is in the past.");
    }

    @Test
    void shouldPublishAwaitingTrackValidationWhenOutcomeIsPassed() {
        UUID productId = UUID.randomUUID();

        service.onProductEvaluated(productId, ValidationOutcome.PASSED, List.of());

        ArgumentCaptor<StatusUpdateEvent> captor = ArgumentCaptor.forClass(StatusUpdateEvent.class);
        verify(statusUpdatePublisher).publish(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ProductStatus.AWAITING_TRACK_VALIDATION.name());
    }

    @Test
    void shouldPublishTrackValidationFailedWhenTrackOutcomeIsFailed() {
        UUID trackId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        service.onTrackEvaluated(trackId, productId, ValidationOutcome.FAILED, List.of());

        ArgumentCaptor<StatusUpdateEvent> captor = ArgumentCaptor.forClass(StatusUpdateEvent.class);
        verify(statusUpdatePublisher).publish(captor.capture());
        assertThat(captor.getValue().getEntityType()).isEqualTo(StatusUpdateEvent.EntityType.TRACK);
        assertThat(captor.getValue().getEntityId()).isEqualTo(trackId.toString());
        assertThat(captor.getValue().getStatus()).isEqualTo(TrackStatus.VALIDATION_FAILED.name());
    }

    @Test
    void shouldPublishNeedsReviewWhenTrackOutcomeIsNeedsReview() {
        UUID trackId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        service.onTrackEvaluated(trackId, productId, ValidationOutcome.NEEDS_REVIEW, List.of());

        ArgumentCaptor<StatusUpdateEvent> captor = ArgumentCaptor.forClass(StatusUpdateEvent.class);
        verify(statusUpdatePublisher).publish(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(TrackStatus.NEEDS_REVIEW.name());
    }

    @Test
    void shouldPublishValidatedWhenAllTracksValidated() {
        UUID productId = UUID.randomUUID();

        service.onAllTracksValidated(productId);

        ArgumentCaptor<StatusUpdateEvent> captor = ArgumentCaptor.forClass(StatusUpdateEvent.class);
        verify(statusUpdatePublisher).publish(captor.capture());
        assertThat(captor.getValue().getEntityType()).isEqualTo(StatusUpdateEvent.EntityType.PRODUCT);
        assertThat(captor.getValue().getStatus()).isEqualTo(ProductStatus.VALIDATED.name());
    }

    @Test
    void shouldTriggerRollupWhenProductAwaitingAndAllTracksValidated() {
        UUID productId = UUID.randomUUID();
        UUID trackId = UUID.randomUUID();

        Map<UUID, String> trackStatuses = Map.of(trackId, TrackStatus.VALIDATED.name());

        boolean result = service.onValidationStateUpdated(
                productId, ProductStatus.AWAITING_TRACK_VALIDATION.name(), trackStatuses);

        assertTrue(result);
        verify(statusUpdatePublisher).publish(argThat(event ->
                event.getEntityId().equals(productId.toString()) &&
                        event.getStatus().equals(ProductStatus.VALIDATED.name())
        ));
    }

    @Test
    void shouldNotTriggerRollupWhenProductAwaitingButTrackStillPending() {
        UUID productId = UUID.randomUUID();
        UUID trackId = UUID.randomUUID();

        Map<UUID, String> trackStatuses = Map.of(trackId, TrackStatus.PENDING.name());

        boolean result = service.onValidationStateUpdated(
                productId, ProductStatus.AWAITING_TRACK_VALIDATION.name(), trackStatuses);

        assertFalse(result);
        verify(statusUpdatePublisher, never()).publish(any());
    }

    @Test
    void shouldNotTriggerRollupWhenProductNotAwaitingTrackValidation() {
        UUID productId = UUID.randomUUID();
        UUID trackId = UUID.randomUUID();

        Map<UUID, String> trackStatuses = Map.of(trackId, TrackStatus.VALIDATED.name());

        boolean result = service.onValidationStateUpdated(
                productId, ProductStatus.SUBMITTED.name(), trackStatuses);

        assertFalse(result);
        verify(statusUpdatePublisher, never()).publish(any());
    }
}
