package com.fairshare.ledger.service;

import com.fairshare.ledger.domain.Balance;
import com.fairshare.ledger.domain.ProcessedEvent;
import com.fairshare.ledger.event.ExpenseCreatedEvent;
import com.fairshare.ledger.event.SettlementRecordedEvent;
import com.fairshare.ledger.repository.BalanceRepository;
import com.fairshare.ledger.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BalanceService {

    private final BalanceRepository balanceRepository;
    private final ProcessedEventRepository processedEventRepository;

    /**
     * Applies an expense's splits to pairwise balances, exactly once per
     * eventId. The idempotency check and every balance mutation happen in
     * one transaction: either the whole event is applied and recorded as
     * processed, or none of it is, a partial application (e.g. crash
     * halfway through a 5-person split) would be worse than not having
     * processed the event at all, since it'd be silently invisible on
     * simple retry.
     */
    @Transactional
    public void applyExpenseEvent(ExpenseCreatedEvent event) {
        if (processedEventRepository.existsById(event.eventId())) {
            log.info("Event {} already processed, skipping (redelivery)", event.eventId());
            return;
        }

        String payer = event.paidByUserId();
        for (ExpenseCreatedEvent.SplitShare split : event.splits()) {
            String debtor = split.userId();
            if (debtor.equals(payer)) {
                // the payer's own share isn't a debt to themselves
                continue;
            }
            applyDebt(event.groupId(), debtor, payer, split.shareAmount());
        }

        processedEventRepository.save(new ProcessedEvent(event.eventId()));
        log.info("Applied expense {} ({}) to balances for group {}",
                event.expenseId(), event.eventId(), event.groupId());
    }

    private void applyDebt(UUID groupId, String debtor, String creditor, BigDecimal amount) {
        String[] canonical = Balance.canonicalOrder(debtor, creditor);
        Balance balance = balanceRepository
                .findByGroupIdAndUserAAndUserB(groupId, canonical[0], canonical[1])
                .orElseGet(() -> Balance.newPair(groupId, canonical[0], canonical[1]));

        balance.applyDebt(debtor, creditor, amount);
        balanceRepository.save(balance);
    }

    // add this method, anywhere alongside applyExpenseEvent:
    @Transactional
    public void applySettlementEvent(SettlementRecordedEvent event) {
        if (processedEventRepository.existsById(event.eventId())) {
            log.info("Settlement event {} already processed, skipping (redelivery)", event.eventId());
            return;
        }

        applyDebt(event.groupId(), event.paidToUserId(), event.paidByUserId(), event.amount());

        processedEventRepository.save(new ProcessedEvent(event.eventId()));
        log.info("Applied settlement {} ({}) to balances for group {}",
                event.settlementId(), event.eventId(), event.groupId());
    }
}
