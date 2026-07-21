package com.fairshare.expense.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Entity for expense splits.
 */
@Entity
@Table(name = "expense_splits")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExpenseSplit {

    @Id
    private UUID id;

    /**
     * The expense.
     */
    @ManyToOne
    @JoinColumn(name = "expense_id", nullable = false)
    @JsonIgnore
    private Expense expense;

    /**
     * The user id.
     */
    @Column(name = "user_id", nullable = false)
    private String userId;

    /**
     * The share amount.
     */
    @Column(name = "share_amount", nullable = false)
    private BigDecimal shareAmount;

    /**
     * Creates a new expense split.
     *
     * @param expense     The expense.
     * @param userId      The user id.
     * @param shareAmount The share amount.
     */
    public ExpenseSplit(final Expense expense
            , final String userId
            , final BigDecimal shareAmount) {
        this.id = UUID.randomUUID();
        this.expense = expense;
        this.userId = userId;
        this.shareAmount = shareAmount;
    }
}
