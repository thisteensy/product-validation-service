package com.productcatalog.domain.service;

import com.productcatalog.domain.model.*;
import com.productcatalog.domain.ports.out.StatusUpdatePublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ValidationOrchestrationServiceImplTest {

    @Mock
    private StatusUpdatePublisher statusUpdatePublisher;

    private ValidationOrchestrationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ValidationOrchestrationServiceImpl(statusUpdatePublisher);
    }

    @Test
    void shouldPublishValidationFailedWhenOutcomeIsFailed() {
        UUID productId = UUID.randomUUID();

        service.onProductEvaluated(productId, ValidationOutcome.FAILED);

        ArgumentCaptor<StatusUpdateEvent> captor = ArgumentCaptor.forClass(StatusUpdateEvent.class);
        verify(statusUpdatePublisher).publish(captor.capture());
        assertThat(captor.getValue().getEntityType()).isEqualTo(StatusUpdateEvent.EntityType.PRODUCT);
        assertThat(captor.getValue().getEntityId()).isEqualTo(productId.toString());
        assertThat(captor.getValue().getStatus()).isEqualTo(ProductStatus.VALIDATION_FAILED.name());
        assertThat(captor.getValue().getNotes()).isEqualTo("Product-level validation failed");
    }

    @Test
    void shouldPublishNeedsReviewWhenOutcomeIsNeedsReview() {
        UUID productId = UUID.randomUUID();

        service.onProductEvaluated(productId, ValidationOutcome.NEEDS_REVIEW);

        ArgumentCaptor<StatusUpdateEvent> captor = ArgumentCaptor.forClass(StatusUpdateEvent.class);
        verify(statusUpdatePublisher).publish(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ProductStatus.NEEDS_REVIEW.name());
        assertThat(captor.getValue().getNotes()).isEqualTo("Needs manual review");
    }

    @Test
    void shouldPublishAwaitingTrackValidationWhenOutcomeIsPassed() {
        UUID productId = UUID.randomUUID();

        service.onProductEvaluated(productId, ValidationOutcome.PASSED);

        ArgumentCaptor<StatusUpdateEvent> captor = ArgumentCaptor.forClass(StatusUpdateEvent.class);
        verify(statusUpdatePublisher).publish(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ProductStatus.AWAITING_TRACK_VALIDATION.name());
    }

    @Test
    void shouldPublishTrackValidationFailedWhenTrackOutcomeIsFailed() {
        UUID trackId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        service.onTrackEvaluated(trackId, productId, ValidationOutcome.FAILED);

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

        service.onTrackEvaluated(trackId, productId, ValidationOutcome.NEEDS_REVIEW);

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
}
