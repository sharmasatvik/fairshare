package com.fairshare.ledger.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Mirrors com.fairshare.expense.event.ExpenseCreatedEvent in expense-service.
 * See that class's Javadoc for why these two copies exist instead of a
 * shared module, and what the natural next step (schema registry) looks
 * like once this needs to scale past two services maintained by one person.
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
