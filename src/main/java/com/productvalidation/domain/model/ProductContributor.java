package com.productvalidation.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProductContributor {
    private final String name;
    private final ContributorRole role;
}