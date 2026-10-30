package com.abdelali.test_adria.service.dto;

import java.math.BigDecimal;

public record TransferResponse(
        AccountResponse source,
        AccountResponse destination,
        BigDecimal amount
) {
}