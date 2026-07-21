package com.fairshare.settlement.service;

import com.fairshare.settlement.domain.Settlement;
import com.fairshare.settlement.dto.CreateSettlementRequest;
import com.fairshare.settlement.event.SettlementRecordedEvent;
import com.fairshare.settlement.kafka.SettlementEventProducer;
import com.fairshare.settlement.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * NOTE on a gap this service doesn't close: unlike expense-service, this
 * has no way to verify paidByUserId/paidToUserId are actually members of
 * the group, that membership data lives in expense-service's database,
 * and this service doesn't call across to check it (no synchronous
 * inter-service HTTP call, to avoid coupling settlement-service's
 * availability to expense-service's). In a system that needed to close
 * this gap, the standard move is event-carried state transfer: have
 * settlement-service consume expense-service's group-membership events
 * and keep a local read-only copy to validate against, the same pattern
 * ledger-service already uses for balances. Left as a known gap here
 * rather than built, since it'd mostly duplicate a pattern already
 * demonstrated elsewhere in this project.
 */
@Service
@RequiredArgsConstructor
public class SettlementService {

    private final SettlementRepository settlementRepository;
    private final SettlementEventProducer eventProducer;

    @Transactional
    public Settlement recordSettlement(UUID groupId, CreateSettlementRequest request) {
        Settlement settlement = Settlement.create(
                groupId,
                request.paidByUserId(),
                request.paidToUserId(),
                request.amount(),
                request.currencyOrDefault()
        );

        Settlement saved = settlementRepository.save(settlement);
        eventProducer.publish(toEvent(saved));
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Settlement> getSettlementsForGroup(UUID groupId) {
        return settlementRepository.findByGroupIdOrderByCreatedAtDesc(groupId);
    }

    private SettlementRecordedEvent toEvent(Settlement settlement) {
        return new SettlementRecordedEvent(
                UUID.randomUUID(),
                settlement.getId(),
                settlement.getGroupId(),
                settlement.getPaidByUserId(),
                settlement.getPaidToUserId(),
                settlement.getAmount(),
                settlement.getCurrency(),
                Instant.now()
        );
    }
}
