package com.fairshare.ledger.repository;

import com.fairshare.ledger.domain.Balance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BalanceRepository extends JpaRepository<Balance, UUID> {

    Optional<Balance> findByGroupIdAndUserAAndUserB(UUID groupId, String userA, String userB);

    List<Balance> findByGroupId(UUID groupId);
}
