package com.productcatalog.application.kafka;

import com.productcatalog.application.kafka.dtos.TrackEventDto;
import com.productcatalog.application.kafka.mappers.TrackEventMapper;
import com.productcatalog.domain.model.Track;
import com.productcatalog.domain.model.TrackStatus;
import com.productcatalog.domain.model.ValidationOutcome;
import com.productcatalog.domain.ports.out.ProductRepository;
import com.productcatalog.domain.ports.out.RuleEngine;
import com.productcatalog.domain.ports.in.ValidationOrchestrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class TrackValidationConsumer {

    private static final Logger log = LoggerFactory.getLogger(TrackValidationConsumer.class);

    private final TrackEventMapper trackEventMapper;
    private final ProductRepository productRepository;
    private final RuleEngine ruleEngine;
    private final ValidationOrchestrationService orchestrationService;

    public TrackValidationConsumer(TrackEventMapper trackEventMapper,
                                   ProductRepository productRepository,
                                   RuleEngine ruleEngine,
                                   ValidationOrchestrationService orchestrationService) {
        this.trackEventMapper = trackEventMapper;
        this.productRepository = productRepository;
        this.ruleEngine = ruleEngine;
        this.orchestrationService = orchestrationService;
    }

    @KafkaListener(topics = "catalog.music_catalog.tracks", groupId = "track-validation")
    public void consume(@Payload String message,
                        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) throws Exception {
        TrackEventDto event = trackEventMapper.toDto(message);

        if (event.getPayload() == null || event.getPayload().getAfter() == null) {
            log.debug("Skipping track event with no after state -- likely a delete");
            return;
        }

        String op = event.getPayload().getOp();
        if (!"c".equals(op) && !"u".equals(op)) {
            log.debug("Skipping track event with op={}", op);
            return;
        }

        String status = event.getPayload().getAfter().getStatus();
        if (!TrackStatus.PENDING.name().equals(status)) {
            log.debug("Skipping track event with status={} -- only processing PENDING", status);
            return;
        }

        UUID trackId = UUID.fromString(event.getPayload().getAfter().getId());
        UUID productId = UUID.fromString(event.getPayload().getAfter().getProductId());

        List<String> dspTargets = productRepository.findById(productId)
                .map(p -> p.getDspTargets())
                .orElse(List.of());

        Track track = trackEventMapper.toTrackFromTrackRow(event.getPayload().getAfter());
        ValidationOutcome outcome = ruleEngine.evaluateTrack(track, dspTargets);
        orchestrationService.onTrackEvaluated(trackId, productId, outcome);
    }
}