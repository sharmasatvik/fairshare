package com.fairshare.ledger.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DebtSimplifierTest {

    @Test
    void simpleTwoPersonDebt_producesOneTransaction() {
        Map<String, BigDecimal> net = Map.of(
                "alice", new BigDecimal("50.00"),   // owed 50
                "bob", new BigDecimal("-50.00")     // owes 50
        );

        List<DebtSimplifier.Transaction> result = DebtSimplifier.simplify(net);

        assertThat(result).containsExactly(
                new DebtSimplifier.Transaction("bob", "alice", new BigDecimal("50.00"))
        );
    }

    @Test
    void chainOfDebts_collapsesIntoFewerTransactionsThanRawPairs() {
        // classic case: A owes B 10, B owes C 10 -> nets to "A owes C 10",
        // one transaction instead of two.
        Map<String, BigDecimal> net = Map.of(
                "alice", new BigDecimal("-10.00"),
                "bob", BigDecimal.ZERO,
                "carol", new BigDecimal("10.00")
        );

        List<DebtSimplifier.Transaction> result = DebtSimplifier.simplify(net);

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(
                new DebtSimplifier.Transaction("alice", "carol", new BigDecimal("10.00")));
    }

    @Test
    void threeWaySplit_neverExceedsNMinusOneTransactions() {
        // 4 participants -> at most 3 transactions to fully settle, regardless
        // of how tangled the underlying pairwise debts were.
        Map<String, BigDecimal> net = Map.of(
                "alice", new BigDecimal("30.00"),
                "bob", new BigDecimal("20.00"),
                "carol", new BigDecimal("-25.00"),
                "dave", new BigDecimal("-25.00")
        );

        List<DebtSimplifier.Transaction> result = DebtSimplifier.simplify(net);

        assertThat(result.size()).isLessThanOrEqualTo(3);

        // and the result must still balance: every creditor's total in equals
        // their net balance, same for debtors
        BigDecimal aliceReceived = sumTo(result, "alice");
        BigDecimal bobReceived = sumTo(result, "bob");
        assertThat(aliceReceived).isEqualByComparingTo("30.00");
        assertThat(bobReceived).isEqualByComparingTo("20.00");
    }

    @Test
    void allSettled_producesNoTransactions() {
        Map<String, BigDecimal> net = Map.of(
                "alice", BigDecimal.ZERO,
                "bob", BigDecimal.ZERO
        );

        assertThat(DebtSimplifier.simplify(net)).isEmpty();
    }

    @Test
    void emptyGroup_producesNoTransactions() {
        assertThat(DebtSimplifier.simplify(Map.of())).isEmpty();
    }

    private static BigDecimal sumTo(List<DebtSimplifier.Transaction> transactions, String userId) {
        return transactions.stream()
                .filter(t -> t.toUserId().equals(userId))
                .map(DebtSimplifier.Transaction::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
