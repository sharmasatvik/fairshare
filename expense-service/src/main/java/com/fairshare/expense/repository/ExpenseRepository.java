package com.fairshare.expense.repository;

import com.fairshare.expense.domain.Expense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ExpenseRepository extends JpaRepository<Expense, UUID> {

    List<Expense> findByGroupIdOrderByCreatedAtDesc(UUID groupId);
}
