package com.fairshare.settlement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CreateSettlementRequest(
        @NotBlank String paidByUserId,
        @NotBlank String paidToUserId,
        @NotNull @Positive BigDecimal amount,
        String currency
) {
    public String currencyOrDefault() {
        return currency == null || currency.isBlank() ? "INR" : currency;
    }
}
