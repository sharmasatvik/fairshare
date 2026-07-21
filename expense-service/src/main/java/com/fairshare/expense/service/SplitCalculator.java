package com.fairshare.expense.service;

import com.fairshare.expense.domain.SplitType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Computes each participant's share of an expense.
 * <p>
 * The one part of this that's easy to get subtly wrong is EQUAL splitting:
 * dividing an amount like 100.00 across 3 people gives 33.33 each, which
 * only sums to 99.99. The missing paisa has to go somewhere, or the ledger
 * will never fully balance. We assign the remainder to the first N
 * participants (by iteration order) rather than leaving it unaccounted for.
 */
public final class SplitCalculator {

    private static final int CURRENCY_SCALE = 2;

    private SplitCalculator() {
    }

    /**
     * @param splitType      how the expense should be divided
     * @param totalAmount    the full expense amount
     * @param participantIds who owes a share (order matters for EQUAL remainder distribution)
     * @param explicitValues for EXACT: userId -> amount owed. for PERCENTAGE: userId -> percentage (0-100).
     *                       ignored for EQUAL.
     *
     * @return userId -> share amount, guaranteed to sum exactly to totalAmount
     */
    public static Map<String, BigDecimal> calculateShares(final SplitType splitType
            , final BigDecimal totalAmount
            , final List<String> participantIds
            , final Map<String, BigDecimal> explicitValues) {

        if (participantIds == null || participantIds.isEmpty()) {
            throw new IllegalArgumentException("An expense must have at least one participant");
        }

        return switch (splitType) {
            case EQUAL -> splitEqually(totalAmount, participantIds);
            case EXACT -> splitExact(totalAmount, participantIds, explicitValues);
            case PERCENTAGE -> splitByPercentage(totalAmount, participantIds, explicitValues);
        };
    }

    private static Map<String, BigDecimal> splitEqually(final BigDecimal totalAmount
            , final List<String> participantIds) {
        int n = participantIds.size();
        final var baseShare = totalAmount
                .divide(BigDecimal.valueOf(n), CURRENCY_SCALE, RoundingMode.DOWN);

        final var distributed = baseShare.multiply(BigDecimal.valueOf(n));
        final var remainder = totalAmount.subtract(distributed);
        // remainder is a small number of currency's smallest units (e.g. paise),
        // hand them out one-per-person, in order, until exhausted.
        final var unit = BigDecimal.ONE.movePointLeft(CURRENCY_SCALE);
        int extraUnits = remainder.divide(unit, 0, RoundingMode.HALF_UP).intValue();

        Map<String, BigDecimal> shares = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            BigDecimal share = baseShare;
            if (i < extraUnits) {
                share = share.add(unit);
            }
            shares.put(participantIds.get(i), share);
        }
        return shares;
    }

    private static Map<String, BigDecimal> splitExact(final BigDecimal totalAmount
            , final List<String> participantIds
            , final Map<String, BigDecimal> exactValues) {

        requireAllParticipantsCovered(participantIds, exactValues, "EXACT split");

        final var sum = exactValues.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (sum.setScale(CURRENCY_SCALE, RoundingMode.HALF_UP)
                .compareTo(totalAmount.setScale(CURRENCY_SCALE, RoundingMode.HALF_UP)) != 0) {
            throw new IllegalArgumentException(
                    "EXACT split amounts (%s) must sum to the expense total (%s)".formatted(sum, totalAmount));
        }

        return new LinkedHashMap<>(exactValues);
    }

    private static Map<String, BigDecimal> splitByPercentage(final BigDecimal totalAmount
            , final List<String> participantIds
            , final Map<String, BigDecimal> percentages) {

        requireAllParticipantsCovered(participantIds, percentages, "PERCENTAGE split");

        final var totalPercent = percentages.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalPercent.compareTo(BigDecimal.valueOf(100)) != 0) {
            throw new IllegalArgumentException(
                    "PERCENTAGE split must sum to 100, got " + totalPercent);
        }

        Map<String, BigDecimal> shares = new LinkedHashMap<>();
        var runningTotal = BigDecimal.ZERO;
        int n = participantIds.size();

        for (int i = 0; i < n; i++) {
            final var userId = participantIds.get(i);
            BigDecimal share;
            if (i == n - 1) {
                // last participant absorbs any rounding drift so the total is exact,
                // same reasoning as the EQUAL-split remainder handling above.
                share = totalAmount.subtract(runningTotal).setScale(CURRENCY_SCALE, RoundingMode.HALF_UP);
            } else {
                BigDecimal pct = percentages.get(userId);
                share = totalAmount.multiply(pct)
                        .divide(BigDecimal.valueOf(100), CURRENCY_SCALE, RoundingMode.HALF_UP);
                runningTotal = runningTotal.add(share);
            }

            shares.put(userId, share);
        }

        return shares;
    }

    private static void requireAllParticipantsCovered(final List<String> participantIds
            , final Map<String, BigDecimal> values
            , final String context) {
        if (values == null || !values.keySet().containsAll(participantIds) || values.size() != participantIds.size()) {
            throw new IllegalArgumentException(
                    context + " requires exactly one value per participant, no more, no less");
        }
    }
}
