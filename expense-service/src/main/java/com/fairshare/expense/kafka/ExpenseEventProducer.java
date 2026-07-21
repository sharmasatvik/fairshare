package com.fairshare.expense.kafka;

import com.fairshare.expense.event.ExpenseCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExpenseEventProducer {

    private final KafkaTemplate<String, ExpenseCreatedEvent> kafkaTemplate;

    @Value("${app.kafka.topic.expense-events}")
    private String topic;

    /**
     * Keyed by groupId, not expenseId. This is the load-bearing decision in
     * this whole service: it's what guarantees every event for a given
     * group is processed in order by ledger-service, since Kafka only
     * orders messages within a partition, and same key -> same partition.
     */
    public void publish(final ExpenseCreatedEvent event) {
        final var partitionKey = event.groupId().toString();

        kafkaTemplate.send(topic, partitionKey, event).whenComplete((result
                , ex) -> {
            if (ex != null) {
                log.error("Failed to publish expense-created event {} for group {}",
                        event.eventId(), event.groupId(), ex);
            } else {
                log.info("Published expense-created event {} for group {} to partition {}",
                        event.eventId(), event.groupId(), result.getRecordMetadata().partition());
            }
        });
    }
}
