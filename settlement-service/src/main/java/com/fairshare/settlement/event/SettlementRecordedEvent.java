package com.fairshare.settlement.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Published to `settlement-events`. Mirrored in ledger-service under
 * com.fairshare.ledger.event, same rationale as ExpenseCreatedEvent's
 * duplication (see that class's Javadoc in expense-service).
 */
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
