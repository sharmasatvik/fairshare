package com.fairshare.ledger.service;

import com.fairshare.ledger.domain.Balance;
import com.fairshare.ledger.domain.DebtSimplifier;
import com.fairshare.ledger.repository.BalanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LedgerQueryService {

    private final BalanceRepository balanceRepository;

    @Transactional(readOnly = true)
    public List<Balance> getRawBalances(UUID groupId) {
        return balanceRepository.findByGroupId(groupId);
    }

    @Transactional(readOnly = true)
    public List<DebtSimplifier.Transaction> getSimplifiedSettlement(UUID groupId) {
        Map<String, BigDecimal> netBalances = computeNetBalances(groupId);
        return DebtSimplifier.simplify(netBalances);
    }

    /**
     * Collapses this group's pairwise balances into one net figure per
     * user. Each Balance row's positive/negative sign (see Balance's
     * Javadoc for the convention) is folded into both users' running
     * totals, this is where the pairwise ledger becomes the per-user
     * view that debt simplification actually operates on.
     */
    private Map<String, BigDecimal> computeNetBalances(UUID groupId) {
        Map<String, BigDecimal> net = new HashMap<>();
        for (Balance balance : balanceRepository.findByGroupId(groupId)) {
            // positive netAmount: userB owes userA -> userA is owed (credit), userB owes (debit)
            net.merge(balance.getUserA(), balance.getNetAmount(), BigDecimal::add);
            net.merge(balance.getUserB(), balance.getNetAmount().negate(), BigDecimal::add);
        }
        return net;
    }
}
