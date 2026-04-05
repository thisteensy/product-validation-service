package com.productcatalog.application.kafka;

import com.productcatalog.application.kafka.dtos.ProductEventDto;
import com.productcatalog.application.kafka.mappers.ProductEventMapper;
import com.productcatalog.domain.model.Product;
import com.productcatalog.domain.model.ProductStatus;
import com.productcatalog.domain.model.ValidationOutcome;
import com.productcatalog.domain.ports.out.RuleEngine;
import com.productcatalog.domain.ports.in.ValidationOrchestrationService;
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

    private final ProductEventMapper mapper;
    private final RuleEngine ruleEngine;
    private final ValidationOrchestrationService orchestrationService;

    public ProductEventConsumer(ProductEventMapper mapper,
                                RuleEngine ruleEngine,
                                ValidationOrchestrationService orchestrationService) {
        this.mapper = mapper;
        this.ruleEngine = ruleEngine;
        this.orchestrationService = orchestrationService;
    }

    @KafkaListener(topics = "catalog.music_catalog.products", groupId = "product-validation")
    public void consume(@Payload String message,
                        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        try {
            ProductEventDto event = mapper.toDto(message);

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
            ValidationOutcome outcome = ruleEngine.evaluateProduct(product);
            orchestrationService.onProductEvaluated(product.getId(), outcome);

        } catch (Exception e) {
            log.error("Failed to process product event -- routing to DLQ", e);
            throw new RuntimeException(e);
        }
    }
}