package com.abdelali.test_adria.service.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

public record TransferRequest(
        @NotNull UUID sourceAccountId,
        @NotNull UUID destinationAccountId,
        @NotNull
        @DecimalMin(value = "0.0", inclusive = false)
        BigDecimal amount
) {
}