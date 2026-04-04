package com.productcatalog.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OwnershipSplit {

    private final String rightsHolder;
    private final double percentage;

}