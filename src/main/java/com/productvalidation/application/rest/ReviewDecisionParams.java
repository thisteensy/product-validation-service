package com.productvalidation.application.rest;

import com.productvalidation.domain.model.ProductStatus;
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