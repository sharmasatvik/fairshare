package com.fairshare.ledger.domain;

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

/**
 * One row per (group, unordered user pair). Users are stored in
 * lexicographic order (userA < userB) so a debt in either direction between
 * the same two people always updates the same row instead of scattering
 * across two rows that would each need reconciling separately.
 * <p>
 * Sign convention: positive netAmount means userB owes userA.
 * Negative netAmount means userA owes userB (by the absolute value).
 */
@Entity
@Table(name = "balances")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Balance {

    @Id
    private UUID id;

    @Column(name = "group_id", nullable = false)
    private UUID groupId;

    @Column(name = "user_a", nullable = false)
    private String userA;

    @Column(name = "user_b", nullable = false)
    private String userB;

    @Column(name = "net_amount", nullable = false)
    private BigDecimal netAmount = BigDecimal.ZERO;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static Balance newPair(final UUID groupId
            , final String userA
            , final String userB) {
        if (userA.compareTo(userB) >= 0) {
            throw new IllegalArgumentException("userA must be lexicographically before userB (canonical order)");
        }
        final var balance = new Balance();

        balance.id = UUID.randomUUID();
        balance.groupId = groupId;
        balance.userA = userA;
        balance.userB = userB;
        balance.netAmount = BigDecimal.ZERO;
        balance.updatedAt = Instant.now();

        return balance;
    }

    /**
     * Records that `debtorId` owes `creditorId` (i.e. `creditorId` paid,
     * `debtorId` owes their share). Both IDs must be one of this balance's
     * two users. Adjusts netAmount according to the sign convention above.
     */
    public void applyDebt(final String debtorId
            , final String creditorId
            , final BigDecimal amount) {
        boolean debtorIsUserA = userA.equals(debtorId);
        boolean debtorIsUserB = userB.equals(debtorId);
        if (!debtorIsUserA && !debtorIsUserB) {
            throw new IllegalArgumentException("debtorId " + debtorId + " is not part of this balance pair");
        }
        boolean creditorMatches = debtorIsUserA ? userB.equals(creditorId) : userA.equals(creditorId);
        if (!creditorMatches) {
            throw new IllegalArgumentException("creditorId " + creditorId + " is not the counterpart in this pair");
        }

        if (debtorIsUserB) {
            // userB owes userA -> increases the positive convention
            netAmount = netAmount.add(amount);
        } else {
            // userA owes userB -> increases the negative convention
            netAmount = netAmount.subtract(amount);
        }
        updatedAt = Instant.now();
    }

    /**
     * Canonical (userA, userB) ordering for a given raw pair of user IDs.
     */
    public static String[] canonicalOrder(final String userId1, final String userId2) {
        return userId1.compareTo(userId2) < 0
                ? new String[]{userId1, userId2}
                : new String[]{userId2, userId1};
    }
}
