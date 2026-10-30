package com.abdelali.test_adria;

import com.abdelali.test_adria.messaging.TransactionCreatedEvent;
import com.abdelali.test_adria.messaging.TransactionKafkaPublisher;
import com.abdelali.test_adria.service.dto.TransactionResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionKafkaPublisherTests {

    @Mock
    KafkaTemplate<String, TransactionCreatedEvent> kafkaTemplate;

    @Test
    void sendsJsonEventToExpectedTopicUsingAccountIdAsKey() {
        UUID accountId = UUID.randomUUID();
        TransactionResponse transaction = new TransactionResponse(
                UUID.randomUUID(), accountId, new BigDecimal("150.00"),
                "EUR", "SHOPPING", "MA", Instant.parse("2026-10-30T09:00:00Z"));

        TransactionCreatedEvent event = TransactionCreatedEvent.from(transaction);
        when(kafkaTemplate.send(eq("bank.transactions.created"), eq(accountId.toString()), any(TransactionCreatedEvent.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        new TransactionKafkaPublisher(kafkaTemplate, "bank.transactions.created")
                .onTransactionCreated(event);

        ArgumentCaptor<TransactionCreatedEvent> captured =
                ArgumentCaptor.forClass(TransactionCreatedEvent.class);
        verify(kafkaTemplate).send(eq("bank.transactions.created"),
                eq(accountId.toString()), captured.capture());

        assertThat(captured.getValue()).isEqualTo(event);
        assertThat(event.type()).isEqualTo("TransactionCreated");
        assertThat(event.transactionId()).isEqualTo(transaction.id());
        assertThat(event.amount()).isEqualByComparingTo("150.00");
        assertThat(event.occurredAt()).isEqualTo(transaction.createdAt());
    }

    @Test
    void brokerFailureDoesNotThrowAfterTransactionHasCommitted() {
        UUID accountId = UUID.randomUUID();
        TransactionResponse transaction = new TransactionResponse(
                UUID.randomUUID(), accountId, new BigDecimal("20.00"),
                "EUR", "SHOPPING", "MA", Instant.now());
        TransactionCreatedEvent event = TransactionCreatedEvent.from(transaction);
        when(kafkaTemplate.send(eq("bank.transactions.created"), eq(accountId.toString()), any(TransactionCreatedEvent.class)))
                .thenThrow(new IllegalStateException("broker unavailable"));

        org.assertj.core.api.Assertions.assertThatCode(() ->
                new TransactionKafkaPublisher(kafkaTemplate, "bank.transactions.created")
                        .onTransactionCreated(event)
        ).doesNotThrowAnyException();
    }

    @Test
    void listenerIsConfiguredToPublishAfterDatabaseCommit() throws Exception {
        var method = TransactionKafkaPublisher.class.getMethod(
                "onTransactionCreated", TransactionCreatedEvent.class);
        TransactionalEventListener annotation =
                method.getAnnotation(TransactionalEventListener.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.phase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
        assertThat(annotation.fallbackExecution()).isFalse();
    }
}
