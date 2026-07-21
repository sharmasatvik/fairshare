package com.fairshare.ledger.kafka;

import com.fairshare.ledger.event.SettlementRecordedEvent;
import com.fairshare.ledger.service.BalanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SettlementEventConsumer {

    private final BalanceService balanceService;

    @KafkaListener(topics = "${app.kafka.topic.settlement-events}")
    public void onSettlementRecorded(SettlementRecordedEvent event) {
        log.info("Received settlement-recorded event {} for group {}", event.eventId(), event.groupId());
        balanceService.applySettlementEvent(event);
    }
}