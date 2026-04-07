package com.productcatalog.domain.model;

import java.util.List;

public record ValidationResult(ValidationOutcome outcome, List<String> violations) {}