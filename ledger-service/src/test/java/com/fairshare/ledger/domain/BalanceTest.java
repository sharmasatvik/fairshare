package com.fairshare.ledger.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BalanceTest {

    private final UUID groupId = UUID.randomUUID();

    @Test
    void applyDebt_debtorIsUserB_increasesPositiveNetAmount() {
        // alice < bob lexicographically, so alice=userA, bob=userB
        Balance balance = Balance.newPair(groupId, "alice", "bob");

        // bob owes alice 50 (alice paid, bob is a participant)
        balance.applyDebt("bob", "alice", new BigDecimal("50.00"));

        assertThat(balance.getNetAmount()).isEqualByComparingTo("50.00");
    }

    @Test
    void applyDebt_debtorIsUserA_decreasesNetAmount() {
        Balance balance = Balance.newPair(groupId, "alice", "bob");

        // alice owes bob 30 (bob paid, alice is a participant)
        balance.applyDebt("alice", "bob", new BigDecimal("30.00"));

        assertThat(balance.getNetAmount()).isEqualByComparingTo("-30.00");
    }

    @Test
    void applyDebt_oppositeDirectionDebtsNetOutOnSamePair() {
        Balance balance = Balance.newPair(groupId, "alice", "bob");

        balance.applyDebt("bob", "alice", new BigDecimal("50.00"));  // bob owes alice 50
        balance.applyDebt("alice", "bob", new BigDecimal("20.00"));  // alice owes bob 20

        // net: bob still owes alice 30
        assertThat(balance.getNetAmount()).isEqualByComparingTo("30.00");
    }

    @Test
    void applyDebt_rejectsUserNotInPair() {
        Balance balance = Balance.newPair(groupId, "alice", "bob");

        assertThatThrownBy(() -> balance.applyDebt("carol", "alice", new BigDecimal("10.00")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void canonicalOrder_isLexicographicRegardlessOfInputOrder() {
        assertThat(Balance.canonicalOrder("bob", "alice")).containsExactly("alice", "bob");
        assertThat(Balance.canonicalOrder("alice", "bob")).containsExactly("alice", "bob");
    }
}
