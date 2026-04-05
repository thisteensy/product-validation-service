package com.productcatalog.domain.service;

import com.productcatalog.domain.model.*;
import com.productcatalog.domain.ports.out.ProductRepository;
import com.productcatalog.domain.ports.out.TrackRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static com.productcatalog.ValidationBuilders.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ValidationOrchestrationServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private TrackRepository trackRepository;

    private ValidationOrchestrationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ValidationOrchestrationServiceImpl(productRepository, trackRepository);
    }

    // onProductEvaluated tests

    @Test
    void shouldTransitionToValidationFailedWhenOutcomeIsFailed() {
        UUID productId = UUID.randomUUID();

        service.onProductEvaluated(productId, ValidationOutcome.FAILED);

        verify(productRepository).updateStatus(
                eq(productId),
                eq(ProductStatus.VALIDATION_FAILED),
                eq("Product-level validation failed"),
                eq(ChangedByType.SYSTEM),
                isNull()
        );
    }

    @Test
    void shouldTransitionToNeedsReviewWhenOutcomeIsNeedsReview() {
        UUID productId = UUID.randomUUID();

        service.onProductEvaluated(productId, ValidationOutcome.NEEDS_REVIEW);

        verify(productRepository).updateStatus(
                eq(productId),
                eq(ProductStatus.NEEDS_REVIEW),
                eq("Needs manual review"),
                eq(ChangedByType.SYSTEM),
                isNull()
        );
    }

    @Test
    void shouldTransitionToAwaitingTrackValidationWhenOutcomeIsPassed() {
        UUID productId = UUID.randomUUID();
        Product product = validProduct().toBuilder()
                .id(productId)
                .status(ProductStatus.AWAITING_TRACK_VALIDATION)
                .build();

        service.onProductEvaluated(productId, ValidationOutcome.PASSED);

        ArgumentCaptor<ProductStatus> statusCaptor = ArgumentCaptor.forClass(ProductStatus.class);
        verify(productRepository, atLeastOnce()).updateStatus(
                eq(productId), statusCaptor.capture(), any(), any(), any());

        assertThat(statusCaptor.getAllValues())
                .contains(ProductStatus.AWAITING_TRACK_VALIDATION);
    }

    // onTrackEvaluated tests

    @Test
    void shouldTransitionTrackToValidationFailedWhenTrackOutcomeIsFailed() {
        UUID trackId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Product product = validProduct().toBuilder()
                .id(productId)
                .status(ProductStatus.AWAITING_TRACK_VALIDATION)
                .build();

        service.onTrackEvaluated(trackId, productId, ValidationOutcome.FAILED);

        verify(trackRepository).updateStatus(
                eq(trackId),
                eq(TrackStatus.VALIDATION_FAILED),
                isNull(),
                eq(ChangedByType.SYSTEM),
                isNull()
        );
    }

    @Test
    void shouldTransitionTrackToNeedsReviewWhenTrackOutcomeIsNeedsReview() {
        UUID trackId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Product product = validProduct().toBuilder()
                .id(productId)
                .status(ProductStatus.AWAITING_TRACK_VALIDATION)
                .build();

        service.onTrackEvaluated(trackId, productId, ValidationOutcome.NEEDS_REVIEW);

        verify(trackRepository).updateStatus(
                eq(trackId),
                eq(TrackStatus.NEEDS_REVIEW),
                isNull(),
                eq(ChangedByType.SYSTEM),
                isNull()
        );
    }

    // onAllTracksValidated tests

    @Test
    void shouldTransitionProductToValidatedWhenOnAllTracksValidatedCalled() {
        UUID productId = UUID.randomUUID();

        service.onAllTracksValidated(productId);

        verify(productRepository).updateStatus(
                eq(productId),
                eq(ProductStatus.VALIDATED),
                isNull(),
                eq(ChangedByType.SYSTEM),
                isNull()
        );
    }
}