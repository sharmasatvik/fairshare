package com.fairshare.ledger.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.AbstractMap.SimpleEntry;

/**
 * Reduces a group's pairwise balances to a minimal-ish set of settle-up
 * transactions.
 * <p>
 * Honest framing: finding the *provably minimum* number of transactions to
 * settle a set of net balances is equivalent to a min-cash-flow problem
 * and is NP-hard in general (it reduces to set partition). What's
 * implemented here is the standard greedy heuristic, repeatedly settle
 * the largest creditor against the largest debtor, which is what
 * production apps like SplitWise itself actually use in practice, because
 * it's O(n log n), always terminates in at most n-1 transactions for n
 * participants, and is optimal or near-optimal for the vast majority of
 * real group shapes. It is not optimal for every adversarially constructed
 * input. That tradeoff (heuristic vs. exact) is worth being explicit about
 * rather than overclaiming "minimum" transactions.
 */
public final class DebtSimplifier {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private DebtSimplifier() {
    }

    public record Transaction(String fromUserId, String toUserId, BigDecimal amount) {
    }

    /**
     * @param netBalances userId -> net position. Positive means the group
     *                    owes this user money (net creditor); negative
     *                    means this user owes the group money (net debtor).
     */
    public static List<Transaction> simplify(final Map<String, BigDecimal> netBalances) {
        PriorityQueue<SimpleEntry<String, BigDecimal>> creditors =
                new PriorityQueue<>((a, b) -> b.getValue().compareTo(a.getValue())); // largest credit first

        PriorityQueue<SimpleEntry<String, BigDecimal>> debtors =
                new PriorityQueue<>(Map.Entry.comparingByValue()); // most negative (largest debt) first

        for (var entry : netBalances.entrySet()) {
            final var amount = entry.getValue().setScale(2, RoundingMode.HALF_UP);
            int cmp = amount.compareTo(ZERO);
            if (cmp > 0) {
                creditors.add(new SimpleEntry<>(entry.getKey(), amount));
            } else if (cmp < 0) {
                debtors.add(new SimpleEntry<>(entry.getKey(), amount));
            }
            // exactly zero -> already settled with the group, nothing to do
        }

        List<Transaction> transactions = new ArrayList<>();

        while (!creditors.isEmpty() && !debtors.isEmpty()) {
            SimpleEntry<String, BigDecimal> creditor = creditors.poll();
            SimpleEntry<String, BigDecimal> debtor = debtors.poll();

            final var owed = debtor.getValue().negate(); // debtor's magnitude, as a positive number
            final var settleAmount = creditor.getValue().min(owed);

            transactions.add(new Transaction(debtor.getKey(), creditor.getKey(), settleAmount));

            final var creditorRemaining = creditor.getValue().subtract(settleAmount);
            final var debtorRemaining = owed.subtract(settleAmount).negate();

            if (creditorRemaining.compareTo(ZERO) > 0) {
                creditors.add(new SimpleEntry<>(creditor.getKey(), creditorRemaining));
            }
            if (debtorRemaining.compareTo(ZERO) < 0) {
                debtors.add(new SimpleEntry<>(debtor.getKey(), debtorRemaining));
            }
        }

        return transactions;
    }
}
