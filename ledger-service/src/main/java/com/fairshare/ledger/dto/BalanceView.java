package com.fairshare.ledger.dto;

import com.fairshare.ledger.domain.Balance;

import java.math.BigDecimal;

/**
 * Flattens a Balance row's sign convention into an explicit
 * "who owes whom" statement, so API consumers don't need to know the
 * userA/userB canonical-ordering convention to read the response.
 */
public record BalanceView(String owedByUserId, String owedToUserId, BigDecimal amount) {

    public static BalanceView from(Balance balance) {
        BigDecimal net = balance.getNetAmount();
        if (net.signum() >= 0) {
            // userB owes userA
            return new BalanceView(balance.getUserB(), balance.getUserA(), net.abs());
        }
        // userA owes userB
        return new BalanceView(balance.getUserA(), balance.getUserB(), net.abs());
    }
}
