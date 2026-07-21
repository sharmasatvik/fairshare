package com.fairshare.ledger.kafka;

import com.fairshare.ledger.event.ExpenseCreatedEvent;
import com.fairshare.ledger.service.BalanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExpenseEventConsumer {

    private final BalanceService balanceService;

    /**
     * No manual partition assignment here, the ledger-service consumer
     * group relies on Kafka's default group-coordinated partition
     * assignment, and correctness only depends on same-group events
     * landing on the same partition (guaranteed by the producer's
     * groupId-based key), not on which consumer instance handles them.
     * That's what makes this horizontally scalable: adding more
     * ledger-service instances just spreads groups across them.
     */
    @KafkaListener(topics = "${app.kafka.topic.expense-events}")
    public void onExpenseCreated(ExpenseCreatedEvent event) {
        log.info("Received expense-created event {} for group {}", event.eventId(), event.groupId());
        balanceService.applyExpenseEvent(event);
    }
}
