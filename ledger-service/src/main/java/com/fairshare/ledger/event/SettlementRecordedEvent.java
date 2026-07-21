package com.fairshare.ledger.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SettlementRecordedEvent(
        UUID eventId,
        UUID settlementId,
        UUID groupId,
        String paidByUserId,
        String paidToUserId,
        BigDecimal amount,
        String currency,
        Instant occurredAt
) {
}