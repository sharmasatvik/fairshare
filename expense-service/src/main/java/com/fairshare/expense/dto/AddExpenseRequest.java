package com.fairshare.expense.dto;

import com.fairshare.expense.domain.SplitType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * <b>participantUserIds</b> defines who owes a share and, for EQUAL splits, the
 * order remainder units are handed out in.
 * <p>
 * <b>explicitValues</b> is required for EXACT (userId -> amount) and PERCENTAGE
 * (userId -> percentage) splits, and ignored for EQUAL.
 */
public record AddExpenseRequest(
        @NotBlank String description,
        @NotBlank String paidByUserId,
        @NotNull @Positive BigDecimal amount,
        String currency,
        @NotNull SplitType splitType,
        @NotEmpty List<String> participantUserIds,
        Map<String, BigDecimal> explicitValues
) {
    public String currencyOrDefault() {
        return currency == null || currency.isBlank() ? "INR" : currency;
    }
}
