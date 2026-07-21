package com.fairshare.expense.service;

import com.fairshare.expense.domain.Expense;
import com.fairshare.expense.domain.Group;
import com.fairshare.expense.dto.AddExpenseRequest;
import com.fairshare.expense.event.ExpenseCreatedEvent;
import com.fairshare.expense.kafka.ExpenseEventProducer;
import com.fairshare.expense.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final GroupService groupService;
    private final ExpenseEventProducer eventProducer;

    /**
     * NOTE on consistency: this persists the Expense and publishes the Kafka
     * event in the same method, but NOT atomically, a crash between the DB
     * commit and the publish call would leave an expense with no downstream
     * balance update. The correct production fix is the transactional
     * outbox pattern (write the event to an outbox table in the same DB
     * transaction, then a separate poller/CDC process publishes it).
     * Skipped here to keep the demo's moving parts focused on the
     * consumer-side idempotency/ordering story, which is the more
     * interesting half of this problem.
     */
    @Transactional
    public Expense addExpense(final UUID groupId, final AddExpenseRequest request) {
        final var group = groupService.getGroup(groupId);
        validateParticipants(group, request);

        final var shares = SplitCalculator.calculateShares(
                request.splitType(),
                request.amount(),
                request.participantUserIds(),
                request.explicitValues()
        );

        final var expense = Expense.create(
                groupId,
                request.description(),
                request.paidByUserId(),
                request.amount(),
                request.currencyOrDefault(),
                request.splitType()
        );
        shares.forEach(expense::addSplit);

        final var saved = expenseRepository.save(expense);
        eventProducer.publish(toEvent(saved));

        return saved;
    }

    @Transactional(readOnly = true)
    public List<Expense> getExpensesForGroup(final UUID groupId) {
        groupService.getGroup(groupId);

        return expenseRepository.findByGroupIdOrderByCreatedAtDesc(groupId);
    }

    private void validateParticipants(final Group group, final AddExpenseRequest request) {
        if (!group.hasMember(request.paidByUserId())) {
            throw new IllegalArgumentException(
                    "paidByUserId '%s' is not a member of this group".formatted(request.paidByUserId()));
        }
        for (String participant : request.participantUserIds()) {
            if (!group.hasMember(participant)) {
                throw new IllegalArgumentException(
                        "Participant '%s' is not a member of this group".formatted(participant));
            }
        }
    }

    private ExpenseCreatedEvent toEvent(final Expense expense) {
        final var splitShares = expense.getSplits()
                .stream()
                .map(split -> new ExpenseCreatedEvent.SplitShare(split.getUserId()
                        , split.getShareAmount()))
                .toList();

        return new ExpenseCreatedEvent(
                UUID.randomUUID(),
                expense.getId(),
                expense.getGroupId(),
                expense.getDescription(),
                expense.getPaidByUserId(),
                expense.getAmount(),
                expense.getCurrency(),
                splitShares,
                Instant.now()
        );
    }
}
