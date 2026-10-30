package com.abdelali.test_adria.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import static org.springframework.kafka.support.KafkaHeaders.RECEIVED_KEY;

/**
 * Independent audit consumer. Kafka JSON payload is logged as a String:
 * no tight Java-class dependency on transaction-service's producer.
 * No account mutations or external side effects.
 */
@Component
public class TransactionCreatedListener {

    private static final Logger log = LoggerFactory.getLogger(TransactionCreatedListener.class);

    @KafkaListener(
            topics = "${bank.kafka.topics.transaction-created:bank.transactions.created}",
            groupId = "${spring.kafka.consumer.group-id:adria-transaction-audit}"
    )
    public void onTransactionCreated(String payload, @Header(RECEIVED_KEY) String accountId) {
        log.info("CONSUMED TransactionCreated accountId={} payload={}", accountId, payload);
    }
}
