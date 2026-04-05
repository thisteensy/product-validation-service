package com.productcatalog.application.kafka;

import com.productcatalog.application.kafka.mappers.ProductEventMapper;
import com.productcatalog.domain.model.ValidationOutcome;
import com.productcatalog.domain.ports.out.RuleEngine;
import com.productcatalog.domain.ports.in.ValidationOrchestrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductEventConsumerTest {

    @Mock
    private RuleEngine ruleEngine;

    @Mock
    private ValidationOrchestrationService orchestrationService;

    private ProductEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new ProductEventConsumer(
                new ProductEventMapper(new com.fasterxml.jackson.databind.ObjectMapper()),
                ruleEngine,
                orchestrationService);
    }

    private String productEvent(String status, String op) {
        return """
                {"payload":{"op":"%s","after":{"id":"00000000-0000-0000-0000-000000000001",
                "status":"%s","release_date":20000,"ownership_splits":"[]",
                "dsp_targets":"[\\"spotify\\"]","tracks":"[]"}}}
                """.formatted(op, status);
    }

    @Test
    void shouldCallOrchestrationServiceWhenStatusIsSubmittedAndOpIsCreate() {
        when(ruleEngine.evaluateProduct(any())).thenReturn(ValidationOutcome.PASSED);

        consumer.consume(productEvent("SUBMITTED", "c"), "catalog.music_catalog.products");

        verify(orchestrationService).onProductEvaluated(
                any(UUID.class), any(ValidationOutcome.class));
    }

    @Test
    void shouldCallOrchestrationServiceWhenStatusIsResubmitted() {
        when(ruleEngine.evaluateProduct(any())).thenReturn(ValidationOutcome.PASSED);

        consumer.consume(productEvent("RESUBMITTED", "u"), "catalog.music_catalog.products");

        verify(orchestrationService).onProductEvaluated(
                any(UUID.class), any(ValidationOutcome.class));
    }

    @Test
    void shouldSkipEventWhenOpIsDelete() {
        consumer.consume(productEvent("SUBMITTED", "d"), "catalog.music_catalog.products");

        verify(orchestrationService, never()).onProductEvaluated(any(), any());
    }

    @Test
    void shouldSkipEventWhenStatusIsNotSubmittedOrResubmitted() {
        consumer.consume(productEvent("AWAITING_TRACK_VALIDATION", "u"), "catalog.music_catalog.products");

        verify(orchestrationService, never()).onProductEvaluated(any(), any());
    }

    @Test
    void shouldSkipEventWhenPayloadAfterIsNull() {
        String message = """
                {"payload":{"op":"c","after":null}}
                """;

        consumer.consume(message, "catalog.music_catalog.products");

        verify(orchestrationService, never()).onProductEvaluated(any(), any());
    }

    @Test
    void shouldSkipEventWhenPayloadIsNull() {
        String message = """
                {"payload":null}
                """;

        consumer.consume(message, "catalog.music_catalog.products");

        verify(orchestrationService, never()).onProductEvaluated(any(), any());
    }
}