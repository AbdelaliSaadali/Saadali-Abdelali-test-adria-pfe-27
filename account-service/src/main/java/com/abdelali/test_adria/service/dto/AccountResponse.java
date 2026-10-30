package com.abdelali.test_adria.service.dto;

import com.abdelali.test_adria.domain.AccountStatus;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        String customerReference,
        AccountStatus status,
        String currency,
        double balance
) {
}