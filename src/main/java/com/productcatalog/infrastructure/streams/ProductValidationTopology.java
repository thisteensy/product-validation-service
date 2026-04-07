package com.productcatalog.infrastructure.streams;

import com.productcatalog.application.kafka.dtos.ProductEventDto;
import com.productcatalog.application.kafka.mappers.ProductEventMapper;
import com.productcatalog.application.kafka.dtos.TrackEventDto;
import com.productcatalog.application.kafka.mappers.TrackEventMapper;
import com.productcatalog.domain.ports.in.ValidationStatePort;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;
import java.util.UUID;

@Configuration
public class ProductValidationTopology {

    private static final Logger log = LoggerFactory.getLogger(ProductValidationTopology.class);

    private static final String PRODUCT_TOPIC = "catalog.music_catalog.products";
    private static final String TRACK_TOPIC = "catalog.music_catalog.tracks";
    private static final String STATE_STORE = "product-validation-state";

    private final ProductEventMapper productEventMapper;
    private final TrackEventMapper trackEventMapper;
    private final ValidationStatePort validationStatePort;
    private final ProductValidationStateSerde stateSerde;
    private final ValidationEventSerde eventSerde;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public ProductValidationTopology(ProductEventMapper productEventMapper,
                                     TrackEventMapper trackEventMapper,
                                     ValidationStatePort validationStatePort,
                                     ProductValidationStateSerde stateSerde,
                                     ValidationEventSerde eventSerde,
                                     KafkaTemplate<String, String> kafkaTemplate) {
        this.productEventMapper = productEventMapper;
        this.trackEventMapper = trackEventMapper;
        this.validationStatePort = validationStatePort;
        this.stateSerde = stateSerde;
        this.eventSerde = eventSerde;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Bean
    public KTable<String, ProductValidationState> productValidationStateTable(StreamsBuilder builder) {

        // Product stream -- parse and re-key by product ID
        KStream<String, ValidationEvent> productStream = builder
                .stream(PRODUCT_TOPIC, Consumed.with(Serdes.String(), Serdes.String()))
                .flatMapValues(message -> {
                    try {
                        ProductEventDto event = productEventMapper.toDto(message);
                        if (event.getPayload() == null || event.getPayload().getAfter() == null) {
                            return List.of();
                        }
                        String op = event.getPayload().getOp();
                        if (!"c".equals(op) && !"u".equals(op)) {
                            return List.of();
                        }
                        ProductEventDto.ProductRow row = event.getPayload().getAfter();
                        return List.of(new ValidationEvent(
                                ValidationEvent.Type.PRODUCT,
                                row.getId(),
                                null,
                                row.getStatus(),
                                productEventMapper.parseDspTargets(row.getDspTargets()
                                )));
                    } catch (Exception e) {
                        log.warn("Skipping unparseable product event: {}", e.getMessage());
                        return List.of();
                    }
                })
                .selectKey((key, event) -> event.getProductId());

        // Track stream -- parse and re-key by product ID
        KStream<String, ValidationEvent> trackStream = builder
                .stream(TRACK_TOPIC, Consumed.with(Serdes.String(), Serdes.String()))
                .flatMapValues(message -> {
                    try {
                        TrackEventDto event = trackEventMapper.toDto(message);
                        if (event.getPayload() == null || event.getPayload().getAfter() == null) {
                            return List.of();
                        }
                        String op = event.getPayload().getOp();
                        if (!"c".equals(op) && !"u".equals(op)) {
                            return List.of();
                        }
                        TrackEventDto.TrackRow row = event.getPayload().getAfter();
                        return List.of(new ValidationEvent(
                                ValidationEvent.Type.TRACK,
                                row.getProductId(),
                                row.getId(),
                                row.getStatus(),
                                List.of()
                        ));
                    } catch (Exception e) {
                        log.warn("Skipping unparseable track event: {}", e.getMessage());
                        return List.of();
                    }
                })
                .selectKey((key, event) -> event.getProductId());

        // Merge both streams and aggregate into a single state store
        KTable<String, ProductValidationState> stateTable = productStream
                .merge(trackStream)
                .groupByKey(Grouped.with(Serdes.String(), eventSerde))
                .aggregate(
                        ProductValidationState::new,
                        (productId, event, state) -> {
                            if (event.getType() == ValidationEvent.Type.PRODUCT) {
                                state.setProductStatus(event.getStatus());
                                if (event.getDspTargets() != null && !event.getDspTargets().isEmpty()) {
                                    state.setDspTargets(event.getDspTargets());
                                }
                            } else {
                                state.getTrackStatuses().put(UUID.fromString(event.getTrackId()), event.getStatus());
                            }
                            return state;
                        },
                        Materialized.<String, ProductValidationState, org.apache.kafka.streams.state.KeyValueStore<org.apache.kafka.common.utils.Bytes, byte[]>>as(STATE_STORE)
                                .withKeySerde(Serdes.String())
                                .withValueSerde(stateSerde)
                );
        // Detect completion and trigger rollup
        stateTable.toStream().foreach((productId, state) -> {
            if (state == null) {
                return;
            }

            boolean completed = validationStatePort.onValidationStateUpdated(
                    UUID.fromString(productId),
                    state.getProductStatus(),
                    state.getTrackStatuses()
            );

            if (completed) {
                log.info("Evicting product {} from validation state store", productId);
                kafkaTemplate.send("product-validation-streams-product-validation-state-repartition", productId, null);
            }
        });

        return stateTable;
    }
}