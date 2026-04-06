package com.productcatalog.application.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.productcatalog.application.kafka.mappers.TrackEventMapper;
import com.productcatalog.domain.model.ValidationOutcome;
import com.productcatalog.domain.ports.out.ProductRepository;
import com.productcatalog.domain.ports.out.RuleEngine;
import com.productcatalog.domain.ports.in.ValidationOrchestrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static com.productcatalog.ValidationBuilders.validProduct;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrackValidationConsumerTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private RuleEngine ruleEngine;

    @Mock
    private ValidationOrchestrationService orchestrationService;

    private TrackValidationConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new TrackValidationConsumer(
                new TrackEventMapper(new ObjectMapper()),
                productRepository,
                ruleEngine,
                orchestrationService);
    }

    private String trackEvent(String status, String op) {
        return """
                {"payload":{"op":"%s","after":{
                "id":"00000000-0000-0000-0000-000000000002",
                "product_id":"00000000-0000-0000-0000-000000000001",
                "status":"%s","explicit":0,
                "contributors":"[]","ownership_splits":"[]"}}}
                """.formatted(op, status);
    }

    @Test
    void shouldCallOrchestrationServiceWhenStatusIsPendingAndOpIsCreate() throws Exception {
        when(productRepository.findById(any())).thenReturn(Optional.of(validProduct()));
        when(ruleEngine.evaluateTrack(any(), any())).thenReturn(ValidationOutcome.PASSED);

        consumer.consume(trackEvent("PENDING", "c"), "catalog.music_catalog.tracks");

        verify(orchestrationService).onTrackEvaluated(
                any(UUID.class), any(UUID.class), any(ValidationOutcome.class));
    }

    @Test
    void shouldCallOrchestrationServiceWhenStatusIsPendingAndOpIsUpdate() throws Exception {
        when(productRepository.findById(any())).thenReturn(Optional.of(validProduct()));
        when(ruleEngine.evaluateTrack(any(), any())).thenReturn(ValidationOutcome.PASSED);

        consumer.consume(trackEvent("PENDING", "u"), "catalog.music_catalog.tracks");

        verify(orchestrationService).onTrackEvaluated(
                any(UUID.class), any(UUID.class), any(ValidationOutcome.class));
    }

    @Test
    void shouldSkipEventWhenOpIsDelete() throws Exception {
        consumer.consume(trackEvent("PENDING", "d"), "catalog.music_catalog.tracks");

        verify(orchestrationService, never()).onTrackEvaluated(any(), any(), any());
    }

    @Test
    void shouldSkipEventWhenStatusIsNotPending() throws Exception {
        consumer.consume(trackEvent("VALIDATED", "u"), "catalog.music_catalog.tracks");

        verify(orchestrationService, never()).onTrackEvaluated(any(), any(), any());
    }

    @Test
    void shouldSkipEventWhenPayloadAfterIsNull() throws Exception {
        String message = """
                {"payload":{"op":"c","after":null}}
                """;

        consumer.consume(message, "catalog.music_catalog.tracks");

        verify(orchestrationService, never()).onTrackEvaluated(any(), any(), any());
    }

    @Test
    void shouldSkipEventWhenPayloadIsNull() throws Exception {
        String message = """
                {"payload":null}
                """;

        consumer.consume(message, "catalog.music_catalog.tracks");

        verify(orchestrationService, never()).onTrackEvaluated(any(), any(), any());
    }

    @Test
    void shouldUseFallbackDspTargetsWhenProductNotFound() throws Exception {
        when(productRepository.findById(any())).thenReturn(Optional.empty());
        when(ruleEngine.evaluateTrack(any(), any())).thenReturn(ValidationOutcome.PASSED);

        consumer.consume(trackEvent("PENDING", "c"), "catalog.music_catalog.tracks");

        verify(ruleEngine).evaluateTrack(any(), any());
        verify(orchestrationService).onTrackEvaluated(any(), any(), any());
    }
}