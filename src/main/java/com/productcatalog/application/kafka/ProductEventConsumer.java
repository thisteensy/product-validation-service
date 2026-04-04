package com.productcatalog.application.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.productcatalog.domain.model.Product;
import com.productcatalog.domain.model.ProductStatus;
import com.productcatalog.domain.model.ChangedByType;
import com.productcatalog.domain.ports.ProductRepository;
import com.productcatalog.infrastructure.rules.DspOrchestrator;
import com.productcatalog.infrastructure.rules.ProductRules;
import com.productcatalog.infrastructure.rules.RuleResult;
import com.productcatalog.infrastructure.rules.RuleSeverity;
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
public class ProductEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ProductEventConsumer.class);

    private final ObjectMapper objectMapper;
    private final ProductEventMapper mapper;
    private final ProductRules productRules;
    private final ProductRepository productRepository;
    private final DspOrchestrator dspOrchestrator;

    public ProductEventConsumer(ObjectMapper objectMapper,
                                ProductEventMapper mapper,
                                ProductRules productRules,
                                ProductRepository productRepository,
                                DspOrchestrator dspOrchestrator) {
        this.objectMapper = objectMapper;
        this.mapper = mapper;
        this.productRules = productRules;
        this.productRepository = productRepository;
        this.dspOrchestrator = dspOrchestrator;
    }

    @KafkaListener(topics = "catalog.music_catalog.products", groupId = "product-validation")
    public void consume(@Payload String message,
                        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        try {
            ProductEventDto event = objectMapper.readValue(message, ProductEventDto.class);

            if (event.getPayload() == null || event.getPayload().getAfter() == null) {
                log.debug("Skipping event with no after state -- likely a delete");
                return;
            }

            String op = event.getPayload().getOp();
            if (!"c".equals(op) && !"u".equals(op)) {
                log.debug("Skipping event with op={} -- only processing creates and updates", op);
                return;
            }

            String status = event.getPayload().getAfter().getStatus();
            if (!ProductStatus.SUBMITTED.name().equals(status)
                    && !ProductStatus.RESUBMITTED.name().equals(status)) {
                log.debug("Skipping event with status={} -- only processing SUBMITTED and RESUBMITTED", status);
                return;
            }

            Product product = mapper.toProductFromProductRow(event.getPayload().getAfter());

            List<RuleResult> results = productRules.evaluate(product);
            boolean hasBlockingFailure = results.stream()
                    .anyMatch(r -> r.getSeverity() == RuleSeverity.BLOCKING);

            if (hasBlockingFailure) {
                productRepository.updateStatus(product.getId(), ProductStatus.VALIDATION_FAILED,
                        "Product-level validation failed", ChangedByType.SYSTEM, null);
                log.info("Product {} failed product-level validation", product.getId());
            } else {
                productRepository.updateStatus(product.getId(), ProductStatus.AWAITING_TRACK_VALIDATION,
                        null, ChangedByType.SYSTEM, null);
                log.info("Product {} passed product-level validation -- awaiting track validation", product.getId());
            }

        } catch (Exception e) {
            log.error("Failed to process product event -- routing to DLQ", e);
            throw new RuntimeException(e);
        }
    }
}