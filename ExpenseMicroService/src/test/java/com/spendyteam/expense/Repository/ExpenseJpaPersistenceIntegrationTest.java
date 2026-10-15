package com.spendyteam.expense.Repository;

import com.spendyteam.expense.Data.Expense;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("JPA Persistence Integration Tests for ExpenseMicroService")
public class ExpenseJpaPersistenceIntegrationTest {

    @Autowired
    private IExpenseRepository expenseRepository;

    @BeforeEach
    void cleanUp() {
        expenseRepository.deleteAll();
    }

    @Test
    @DisplayName("Should persist expense and auto-generate UUID identifier and default fee")
    void testPersistExpenseAndPrePersist() {
        Expense expense = new Expense();
        expense.setType("CARD_PAYMENT");
        expense.setProduct("Grocery Mart");
        expense.setStartedDate(LocalDateTime.of(2026, 3, 10, 10, 0, 0));
        expense.setCompletedDate(LocalDateTime.of(2026, 3, 10, 10, 5, 0));
        expense.setDescription("Weekly groceries");
        expense.setAmount(new BigDecimal("85.75"));
        expense.setCurrency("EUR");
        expense.setState("COMPLETED");
        expense.setCategory("Supermercati e Alimentari");
        expense.setUsername("giuseppe_test");

        Expense saved = expenseRepository.save(expense);

        assertNotNull(saved.getId());
        assertFalse(saved.getId().isBlank());
        assertEquals(new BigDecimal("85.75"), saved.getAmount());
        assertEquals(BigDecimal.ZERO, saved.getFee()); // default via @PrePersist
        assertEquals("giuseppe_test", saved.getUsername());
    }

    @Test
    @DisplayName("Should find user-scoped expenses with sorting")
    void testFindByUsernameSorted() {
        Expense e1 = new Expense();
        e1.setStartedDate(LocalDateTime.of(2026, 1, 10, 8, 0));
        e1.setAmount(new BigDecimal("10.00"));
        e1.setUsername("userA");

        Expense e2 = new Expense();
        e2.setStartedDate(LocalDateTime.of(2026, 1, 20, 8, 0));
        e2.setAmount(new BigDecimal("20.00"));
        e2.setUsername("userA");

        Expense eOther = new Expense();
        eOther.setStartedDate(LocalDateTime.of(2026, 1, 15, 8, 0));
        eOther.setAmount(new BigDecimal("50.00"));
        eOther.setUsername("userB");

        expenseRepository.saveAll(List.of(e1, e2, eOther));

        List<Expense> userAExpenses = expenseRepository.findByUsername("userA", Sort.by(Sort.Direction.ASC, "startedDate"));
        assertEquals(2, userAExpenses.size());
        assertEquals(new BigDecimal("10.00"), userAExpenses.get(0).getAmount());
        assertEquals(new BigDecimal("20.00"), userAExpenses.get(1).getAmount());

        List<Expense> userBExpenses = expenseRepository.findByUsername("userB");
        assertEquals(1, userBExpenses.size());
    }

    @Test
    @DisplayName("Should query expenses by date range with boundary semantics")
    void testFindExpensesByDateRange() {
        Expense inRange = new Expense();
        inRange.setStartedDate(LocalDateTime.of(2026, 5, 10, 12, 0));
        inRange.setCompletedDate(LocalDateTime.of(2026, 5, 10, 12, 30));
        inRange.setAmount(new BigDecimal("30.00"));
        inRange.setUsername("date_user");

        Expense outOfRange = new Expense();
        outOfRange.setStartedDate(LocalDateTime.of(2026, 6, 15, 12, 0));
        outOfRange.setCompletedDate(LocalDateTime.of(2026, 6, 15, 12, 30));
        outOfRange.setAmount(new BigDecimal("40.00"));
        outOfRange.setUsername("date_user");

        expenseRepository.saveAll(List.of(inRange, outOfRange));

        LocalDateTime start = LocalDateTime.of(2026, 5, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 5, 31, 23, 59, 59);

        List<Expense> result = expenseRepository.findExpensesByDateRange("date_user", start, end);
        assertEquals(1, result.size());
        assertEquals(new BigDecimal("30.00"), result.get(0).getAmount());
    }

    @Test
    @DisplayName("Should check existence and delete user expenses")
    void testExistsAndDeleteByUsername() {
        LocalDateTime started = LocalDateTime.of(2026, 7, 1, 9, 0);
        LocalDateTime completed = LocalDateTime.of(2026, 7, 1, 9, 10);

        Expense e = new Expense();
        e.setStartedDate(started);
        e.setCompletedDate(completed);
        e.setDescription("Restaurant lunch");
        e.setAmount(new BigDecimal("25.50"));
        e.setUsername("delete_user");
        expenseRepository.save(e);

        assertTrue(expenseRepository.existsByStartedDateAndCompletedDate(started, completed));
        assertTrue(expenseRepository.existsByDescriptionAndAmount("Restaurant lunch", new BigDecimal("25.50")));

        long deleted = expenseRepository.deleteByUsername("delete_user");
        assertEquals(1, deleted);
        assertTrue(expenseRepository.findByUsername("delete_user").isEmpty());
    }
}
