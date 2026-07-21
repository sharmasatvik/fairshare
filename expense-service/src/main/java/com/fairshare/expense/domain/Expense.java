package com.fairshare.expense.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entity to manage expenses.
 */
@Entity
@Table(name = "expenses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Expense {

    /**
     * Unique id of the expense.
     */
    @Id
    private UUID id;

    /**
     * The unique id of the group.
     */
    @Column(name = "group_id", nullable = false)
    private UUID groupId;

    /**
     * The description of the expense.
     */
    @Column(nullable = false)
    private String description;

    /**
     * The user id of the user who paid the expense.
     */
    @Column(name = "paid_by_user_id", nullable = false)
    private String paidByUserId;

    /**
     * The expense amount.
     */
    @Column(nullable = false)
    private BigDecimal amount;

    /**
     * The currency.
     */
    @Column(nullable = false)
    private String currency;

    /**
     * The split type.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "split_type", nullable = false)
    private SplitType splitType;

    /**
     * Time when the expense was created.
     */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /**
     * The expense splits.
     */
    @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private final List<ExpenseSplit> splits = new ArrayList<>();

    /**
     * Creates a new expense for a specified group.
     *
     * @param groupId      The group identifier.
     * @param description  The description of the expense.
     * @param paidByUserId The user who paid the expense.
     * @param amount       The amount.
     * @param currency     The currency.
     * @param splitType    The split type.
     *
     * @return The details of the expense.
     */
    public static Expense create(final UUID groupId
            , final String description
            , final String paidByUserId
            , final BigDecimal amount
            , final String currency
            , final SplitType splitType) {
        final var expense = new Expense();

        expense.id = UUID.randomUUID();
        expense.groupId = groupId;
        expense.description = description;
        expense.paidByUserId = paidByUserId;
        expense.amount = amount;
        expense.currency = currency;
        expense.splitType = splitType;
        expense.createdAt = Instant.now();

        return expense;
    }

    /**
     * Adds a new expense split.
     *
     * @param userId      The user.
     * @param shareAmount The share amount.
     */
    public void addSplit(final String userId, final BigDecimal shareAmount) {
        splits.add(new ExpenseSplit(this, userId, shareAmount));
    }
}
