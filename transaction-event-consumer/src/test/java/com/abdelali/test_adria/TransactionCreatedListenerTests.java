package com.abdelali.test_adria;

import com.abdelali.test_adria.messaging.TransactionCreatedListener;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.annotation.KafkaListener;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class TransactionCreatedListenerTests {

    @Test
    void consumesWithoutMutatingState() {
        var listener = new TransactionCreatedListener();
        String event = """
                {"eventId":"f5ba54d2-eae5-4a6c-8030-93e50cb6fa01",
                 "type":"TransactionCreated",
                 "transactionId":"f5ba54d2-eae5-4a6c-8030-93e50cb6fa02",
                 "accountId":"f5ba54d2-eae5-4a6c-8030-93e50cb6fa03",
                 "amount":120.00,"currency":"EUR",
                 "occurredAt":"2026-10-30T10:00:00Z"}
                """;
        assertThatCode(() -> listener.onTransactionCreated(event, "account-1"))
                .doesNotThrowAnyException();
    }

    @Test
    void listensToTheConfiguredTransactionTopic() throws Exception {
        Method method = TransactionCreatedListener.class.getMethod(
                "onTransactionCreated", String.class, String.class);
        KafkaListener annotation = method.getAnnotation(KafkaListener.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.topics()).containsExactly(
                "${bank.kafka.topics.transaction-created:bank.transactions.created}"
        );
    }
}
