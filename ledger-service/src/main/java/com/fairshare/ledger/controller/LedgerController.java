package com.fairshare.ledger.controller;

import com.fairshare.ledger.domain.DebtSimplifier;
import com.fairshare.ledger.dto.BalanceView;
import com.fairshare.ledger.service.LedgerQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/groups/{groupId}/")
@RequiredArgsConstructor
public class LedgerController {

    private final LedgerQueryService ledgerQueryService;

    /**
     * Raw pairwise balances, e.g. "bob owes alice 250.00".
     */
    @GetMapping("balances")
    public List<BalanceView> getBalances(final @PathVariable UUID groupId) {
        return ledgerQueryService.getRawBalances(groupId).stream()
                .map(BalanceView::from)
                .filter(view -> view.amount().signum() != 0) // fully settled pairs aren't worth showing
                .toList();
    }

    /**
     * Minimal-ish set of settle-up transactions for the whole group.
     */
    @GetMapping("simplify")
    public List<DebtSimplifier.Transaction> getSimplifiedSettlement(final @PathVariable UUID groupId) {
        return ledgerQueryService.getSimplifiedSettlement(groupId);
    }
}
