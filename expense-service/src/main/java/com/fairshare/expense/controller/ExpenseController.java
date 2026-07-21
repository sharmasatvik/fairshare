package com.fairshare.expense.controller;

import com.fairshare.expense.domain.Expense;
import com.fairshare.expense.dto.AddExpenseRequest;
import com.fairshare.expense.service.ExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Manages expenses for a group.
 */
@RestController
@RequestMapping("/groups/{groupId}/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    /**
     * Adds an expense to a specified group.
     *
     * @param groupId The group identifier.
     * @param request The details of the expense.
     *
     * @return The details of the expense.
     */
    @PostMapping
    public ResponseEntity<Expense> addExpense(final @PathVariable UUID groupId
            , final @Valid @RequestBody AddExpenseRequest request) {
        final var expense = expenseService.addExpense(groupId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(expense);
    }

    /**
     * Gets the expenses for a specified group id.
     *
     * @param groupId The group identifier.
     *
     * @return The expense details.
     */
    @GetMapping
    public List<Expense> getExpenses(final @PathVariable UUID groupId) {
        return expenseService.getExpensesForGroup(groupId);
    }
}
