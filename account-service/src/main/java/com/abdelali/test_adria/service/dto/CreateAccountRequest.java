package com.abdelali.test_adria.service.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record CreateAccountRequest(
        @NotBlank String customerReference,
        @NotBlank String currency,
        @NotNull @DecimalMin("0.0") BigDecimal initialBalance
) {
}