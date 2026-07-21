package com.fairshare.expense.domain;

import com.fairshare.expense.service.SplitCalculator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SplitCalculatorTest {

    @Test
    void equalSplit_distributesRemainderSoTotalMatchesExactly() {
        // 100.00 / 3 = 33.33 each, with 0.01 left over that must go somewhere
        Map<String, BigDecimal> shares = SplitCalculator.calculateShares(
                SplitType.EQUAL,
                new BigDecimal("100.00"),
                List.of("alice", "bob", "carol"),
                null
        );

        BigDecimal total = shares.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(total).isEqualByComparingTo("100.00");
        assertThat(shares.get("alice")).isEqualByComparingTo("33.34"); // gets the remainder paisa
        assertThat(shares.get("bob")).isEqualByComparingTo("33.33");
        assertThat(shares.get("carol")).isEqualByComparingTo("33.33");
    }

    @Test
    void equalSplit_evenDivision_noRemainder() {
        Map<String, BigDecimal> shares = SplitCalculator.calculateShares(
                SplitType.EQUAL,
                new BigDecimal("100.00"),
                List.of("alice", "bob"),
                null
        );

        assertThat(shares.get("alice")).isEqualByComparingTo("50.00");
        assertThat(shares.get("bob")).isEqualByComparingTo("50.00");
    }

    @Test
    void exactSplit_acceptsValuesThatSumToTotal() {
        Map<String, BigDecimal> values = Map.of(
                "alice", new BigDecimal("60.00"),
                "bob", new BigDecimal("40.00")
        );

        Map<String, BigDecimal> shares = SplitCalculator.calculateShares(
                SplitType.EXACT, new BigDecimal("100.00"), List.of("alice", "bob"), values);

        assertThat(shares).isEqualTo(values);
    }

    @Test
    void exactSplit_rejectsValuesThatDontSumToTotal() {
        Map<String, BigDecimal> values = Map.of(
                "alice", new BigDecimal("60.00"),
                "bob", new BigDecimal("30.00")
        );

        assertThatThrownBy(() -> SplitCalculator.calculateShares(
                SplitType.EXACT, new BigDecimal("100.00"), List.of("alice", "bob"), values))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must sum to");
    }

    @Test
    void percentageSplit_lastParticipantAbsorbsRoundingDrift() {
        // 33.33% x3 = 99.99%, doesn't cleanly divide 100.00, third person
        // absorbs the extra paisa so the total still matches exactly.
        Map<String, BigDecimal> values = Map.of(
                "alice", new BigDecimal("33.33"),
                "bob", new BigDecimal("33.33"),
                "carol", new BigDecimal("33.34")
        );

        Map<String, BigDecimal> shares = SplitCalculator.calculateShares(
                SplitType.PERCENTAGE, new BigDecimal("100.00"), List.of("alice", "bob", "carol"), values);

        BigDecimal total = shares.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(total).isEqualByComparingTo("100.00");
    }

    @Test
    void percentageSplit_rejectsValuesNotSummingTo100() {
        Map<String, BigDecimal> values = Map.of(
                "alice", new BigDecimal("50.00"),
                "bob", new BigDecimal("40.00")
        );

        assertThatThrownBy(() -> SplitCalculator.calculateShares(
                SplitType.PERCENTAGE, new BigDecimal("100.00"), List.of("alice", "bob"), values))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must sum to 100");
    }

    @Test
    void rejectsEmptyParticipantList() {
        assertThatThrownBy(() -> SplitCalculator.calculateShares(
                SplitType.EQUAL, new BigDecimal("100.00"), List.of(), null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
