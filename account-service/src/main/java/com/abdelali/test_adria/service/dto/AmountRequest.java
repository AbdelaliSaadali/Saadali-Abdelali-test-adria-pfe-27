package com.abdelali.test_adria.service.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record AmountRequest(
        @NotNull
        @DecimalMin(value = "0.0", inclusive = false)
        BigDecimal amount
) {
}