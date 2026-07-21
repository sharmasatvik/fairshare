package com.fairshare.expense.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The event contract published to the <b>expense-events</b> topic.
 * <p>
 * NOTE: since expense-service and ledger-service are intentionally kept as
 * independent Maven projects (no shared parent/library module), this class
 * is duplicated in ledger-service's `event` package. That's a real tradeoff:
 * no build-time coupling between services, at the cost of the two copies
 * needing to be kept in sync by hand. In a larger system this is exactly
 * where a schema registry (Avro/Protobuf) earns its keep, noted as a
 * natural next step rather than built here, to keep the demo's dependency
 * footprint small.
 */
public record ExpenseCreatedEvent(
        UUID eventId,
        UUID expenseId,
        UUID groupId,
        String description,
        String paidByUserId,
        BigDecimal amount,
        String currency,
        List<SplitShare> splits,
        Instant occurredAt
) {
    public record SplitShare(String userId, BigDecimal shareAmount) {
    }
}
