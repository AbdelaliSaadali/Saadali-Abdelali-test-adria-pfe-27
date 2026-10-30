package com.abdelali.test_adria.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Publishing only AFTER_COMMIT prevents events for rolled-back local writes.
 * This is a best-effort publisher: a Kafka outage after commit can lose an
 * event; a transactional outbox is required for guaranteed delivery.
 */
@Component
public class TransactionKafkaPublisher {

    private static final Logger log = LoggerFactory.getLogger(TransactionKafkaPublisher.class);

    private final KafkaTemplate<String, TransactionCreatedEvent> kafkaTemplate;
    private final String topic;

    public TransactionKafkaPublisher(
            KafkaTemplate<String, TransactionCreatedEvent> kafkaTemplate,
            @Value("${bank.kafka.topics.transaction-created:bank.transactions.created}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTransactionCreated(TransactionCreatedEvent event) {
        try {
            kafkaTemplate.send(topic, event.accountId().toString(), event)
                    .whenComplete((result, error) -> {
                        if (error == null) {
                            log.info("Published {} eventId={} transactionId={} topic={}",
                                    event.type(), event.eventId(), event.transactionId(), topic);
                        } else {
                            log.error("Kafka publish failed eventId={} transactionId={} topic={}",
                                    event.eventId(), event.transactionId(), topic, error);
                        }
                    });
        } catch (RuntimeException error) {
            // The database has ALREADY committed. Do not report the payment as failed.
            log.error("Kafka send could not start for committed transactionId={} eventId={}",
                    event.transactionId(), event.eventId(), error);
        }
    }
}
