package com.fairshare.settlement.kafka;

import com.fairshare.settlement.event.SettlementRecordedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SettlementEventProducer {

    private final KafkaTemplate<String, SettlementRecordedEvent> kafkaTemplate;

    @Value("${app.kafka.topic.settlement-events}")
    private String topic;

    /** Keyed by groupId, same rationale as ExpenseEventProducer. */
    public void publish(SettlementRecordedEvent event) {
        String partitionKey = event.groupId().toString();

        kafkaTemplate.send(topic, partitionKey, event).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish settlement-recorded event {} for group {}",
                        event.eventId(), event.groupId(), ex);
            } else {
                log.info("Published settlement-recorded event {} for group {} to partition {}",
                        event.eventId(), event.groupId(), result.getRecordMetadata().partition());
            }
        });
    }
}
