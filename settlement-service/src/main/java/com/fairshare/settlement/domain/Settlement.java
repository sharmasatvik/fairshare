package com.fairshare.settlement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "settlements")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Settlement {

    @Id
    private UUID id;

    @Column(name = "group_id", nullable = false)
    private UUID groupId;

    @Column(name = "paid_by_user_id", nullable = false)
    private String paidByUserId;

    @Column(name = "paid_to_user_id", nullable = false)
    private String paidToUserId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public static Settlement create(final UUID groupId
            , final String paidByUserId
            , final String paidToUserId
            , final BigDecimal amount
            , final String currency) {
        if (paidByUserId.equals(paidToUserId)) {
            throw new IllegalArgumentException("A settlement must be between two different users");
        }
        final var settlement = new Settlement();

        settlement.id = UUID.randomUUID();
        settlement.groupId = groupId;
        settlement.paidByUserId = paidByUserId;
        settlement.paidToUserId = paidToUserId;
        settlement.amount = amount;
        settlement.currency = currency;
        settlement.createdAt = Instant.now();

        return settlement;
    }
}
