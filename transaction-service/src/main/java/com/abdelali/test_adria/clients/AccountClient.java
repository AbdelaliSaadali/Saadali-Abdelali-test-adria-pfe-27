package com.abdelali.test_adria.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Contract matching account-service's existing REST endpoints.
 * The account-service URL is configured externally, allowing future Eureka discovery.
 */
@FeignClient(name = "account-service", fallback = AccountClientFallback.class)
public interface AccountClient {

    @GetMapping("/accounts/{id}")
    AccountSnapshot getAccount(@PathVariable("id") UUID id);

    @PostMapping("/accounts/{id}/debit")
    AccountSnapshot debit(@PathVariable("id") UUID id, @RequestBody AmountPayload request);

    @PostMapping("/accounts/transfers")
    TransferSnapshot transfer(@RequestBody TransferPayload request);

    record AccountSnapshot(
            UUID id,
            String customerReference,
            String status,
            String currency,
            double balance
    ) { }

    record AmountPayload(BigDecimal amount) { }

    record TransferPayload(
            UUID sourceAccountId,
            UUID destinationAccountId,
            BigDecimal amount
    ) { }

    record TransferSnapshot(
            AccountSnapshot source,
            AccountSnapshot destination,
            BigDecimal amount
    ) { }
}
