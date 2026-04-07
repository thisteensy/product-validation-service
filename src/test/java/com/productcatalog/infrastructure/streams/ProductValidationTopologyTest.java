package com.productcatalog.infrastructure.streams;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.productcatalog.application.kafka.mappers.ProductEventMapper;
import com.productcatalog.application.kafka.mappers.TrackEventMapper;
import com.productcatalog.domain.ports.in.ValidationOrchestrationService;
import com.productcatalog.domain.ports.in.ValidationStatePort;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TopologyTestDriver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Properties;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductValidationTopologyTest {

    @Mock
    private ValidationStatePort validationStatePort;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private TopologyTestDriver testDriver;
    private TestInputTopic<String, String> productTopic;
    private TestInputTopic<String, String> trackTopic;

    private static final String PRODUCT_ID = "00000000-0000-0000-0000-000000000001";
    private static final String TRACK_ID = "00000000-0000-0000-0000-000000000002";

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        ProductEventMapper productEventMapper = new ProductEventMapper(objectMapper);
        TrackEventMapper trackEventMapper = new TrackEventMapper(objectMapper);
        ProductValidationStateSerde stateSerde = new ProductValidationStateSerde(objectMapper);
        ValidationEventSerde eventSerde = new ValidationEventSerde(objectMapper);

        ProductValidationTopology topology = new ProductValidationTopology(
                productEventMapper, trackEventMapper, validationStatePort, stateSerde, eventSerde, kafkaTemplate);

        StreamsBuilder builder = new StreamsBuilder();
        topology.productValidationStateTable(builder);

        Properties props = new Properties();
        props.put("application.id", "test");
        props.put("bootstrap.servers", "dummy:9092");
        props.put("default.key.serde", "org.apache.kafka.common.serialization.Serdes$StringSerde");
        props.put("default.value.serde", "org.apache.kafka.common.serialization.Serdes$StringSerde");
        props.put("state.dir", System.getProperty("java.io.tmpdir") + "/kafka-streams-test-" + UUID.randomUUID());

        testDriver = new TopologyTestDriver(builder.build(), props);

        productTopic = testDriver.createInputTopic(
                "catalog.music_catalog.products",
                new StringSerializer(), new StringSerializer());

        trackTopic = testDriver.createInputTopic(
                "catalog.music_catalog.tracks",
                new StringSerializer(), new StringSerializer());
    }

    @AfterEach
    void tearDown() {
        testDriver.close();
    }

    private String productEvent(String productId, String status) {
        return """
                {"payload":{"op":"c","after":{"id":"%s","status":"%s"}}}
                """.formatted(productId, status);
    }

    private String trackEvent(String trackId, String productId, String status) {
        return """
                {"payload":{"op":"u","after":{"id":"%s","product_id":"%s","status":"%s"}}}
                """.formatted(trackId, productId, status);
    }

    @Test
    void shouldTriggerRollupWhenProductAwaitingAndAllTracksValidated() {
        productTopic.pipeInput(PRODUCT_ID, productEvent(PRODUCT_ID, "AWAITING_TRACK_VALIDATION"));
        trackTopic.pipeInput(PRODUCT_ID, trackEvent(TRACK_ID, PRODUCT_ID, "VALIDATED"));

        verify(validationStatePort).onValidationStateUpdated(
                eq(UUID.fromString(PRODUCT_ID)),
                eq("AWAITING_TRACK_VALIDATION"),
                argThat(map -> map.containsValue("VALIDATED"))
        );    }

    @Test
    void shouldNotTriggerRollupWhenProductAwaitingButTrackStillPending() {
        productTopic.pipeInput(PRODUCT_ID, productEvent(PRODUCT_ID, "AWAITING_TRACK_VALIDATION"));
        trackTopic.pipeInput(PRODUCT_ID, trackEvent(TRACK_ID, PRODUCT_ID, "PENDING"));

        verify(validationStatePort, atLeastOnce()).onValidationStateUpdated(any(), any(), any());
    }

    @Test
    void shouldNotTriggerRollupWhenProductNotYetAwaitingTrackValidation() {
        productTopic.pipeInput(PRODUCT_ID, productEvent(PRODUCT_ID, "SUBMITTED"));
        trackTopic.pipeInput(PRODUCT_ID, trackEvent(TRACK_ID, PRODUCT_ID, "VALIDATED"));

        verify(validationStatePort, atLeastOnce()).onValidationStateUpdated(any(), any(), any());    }

    @Test
    void shouldNotTriggerRollupWhenNoProductEventReceived() {
        trackTopic.pipeInput(PRODUCT_ID, trackEvent(TRACK_ID, PRODUCT_ID, "VALIDATED"));

        verify(validationStatePort, atLeastOnce()).onValidationStateUpdated(any(), any(), any());
    }

    @Test
    void shouldSkipUnparseableProductEventWhenPayloadIsMalformed() {
        productTopic.pipeInput(PRODUCT_ID, "not valid json");
        trackTopic.pipeInput(PRODUCT_ID, trackEvent(TRACK_ID, PRODUCT_ID, "VALIDATED"));

        verify(validationStatePort, atLeastOnce()).onValidationStateUpdated(any(), any(), any());
    }
    @Test
    void shouldNotTriggerRollupWhenProductValidationFailed () {
        productTopic.pipeInput(PRODUCT_ID, productEvent(PRODUCT_ID, "VALIDATION_FAILED"));
        trackTopic.pipeInput(PRODUCT_ID, trackEvent(TRACK_ID, PRODUCT_ID, "VALIDATED"));

        verify(validationStatePort, atLeastOnce()).onValidationStateUpdated(any(), any(), any());
    }
}