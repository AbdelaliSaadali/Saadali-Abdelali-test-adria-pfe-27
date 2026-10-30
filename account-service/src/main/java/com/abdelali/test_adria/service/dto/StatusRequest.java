package com.abdelali.test_adria.service.dto;

import com.abdelali.test_adria.domain.AccountStatus;
import jakarta.validation.constraints.NotNull;

public record StatusRequest(
        @NotNull AccountStatus status
) {
}