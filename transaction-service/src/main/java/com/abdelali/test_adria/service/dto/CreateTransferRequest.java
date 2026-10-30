package com.abdelali.test_adria.service.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateTransferRequest(
        UUID sourceAccountId,
        UUID destinationAccountId,
        BigDecimal amount,
        String currency,
        String country
) { }
