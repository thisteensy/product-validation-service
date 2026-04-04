package com.productcatalog.application.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.productcatalog.domain.model.Product;
import com.productcatalog.domain.model.ProductStatus;
import com.productcatalog.infrastructure.rules.RuleEngine;
import com.productcatalog.infrastructure.rules.ValidationResult;
import com.productcatalog.domain.ports.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class ProductEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ProductEventConsumer.class);

    private final ObjectMapper objectMapper;
    private final ProductEventMapper mapper;
    private final RuleEngine ruleEngine;
    private final ProductRepository productRepository;

    public ProductEventConsumer(ObjectMapper objectMapper,
                                ProductEventMapper mapper,
                                RuleEngine ruleEngine,
                                ProductRepository productRepository) {
        this.objectMapper = objectMapper;
        this.mapper = mapper;
        this.ruleEngine = ruleEngine;
        this.productRepository = productRepository;
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

            Product product = mapper.toDomain(event.getPayload().getAfter());
            ValidationResult result = new ValidationResult(product, ruleEngine.evaluate(product));
            productRepository.updateStatus(product.getId(), result.getStatus(), null);

            log.info("Validated product {} -- status: {}", product.getId(), result.getStatus());

        } catch (Exception e) {
            log.error("Failed to process product event -- routing to DLQ", e);
            throw new RuntimeException(e);
        }
    }
}