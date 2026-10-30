package com.abdelali.test_adria.messaging;

import com.abdelali.test_adria.service.dto.TransactionResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Kafka contract for bank.transactions.created.
 * The schema is intentionally small, stable and independent of JPA.
 */
public record TransactionCreatedEvent(
        UUID eventId,
        String type,
        UUID transactionId,
        UUID accountId,
        BigDecimal amount,
        String currency,
        Instant occurredAt
) {
    public static TransactionCreatedEvent from(TransactionResponse transaction) {
        return new TransactionCreatedEvent(
                UUID.randomUUID(),
                "TransactionCreated",
                transaction.id(),
                transaction.accountId(),
                transaction.amount(),
                transaction.currency(),
                transaction.createdAt()
        );
    }
}
