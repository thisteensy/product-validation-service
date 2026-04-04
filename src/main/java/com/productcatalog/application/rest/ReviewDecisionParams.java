package com.productcatalog.application.rest;

import com.productcatalog.domain.model.ProductStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ReviewDecisionParams {
    @NotNull
    private ProductStatus status;
    private String notes;
}