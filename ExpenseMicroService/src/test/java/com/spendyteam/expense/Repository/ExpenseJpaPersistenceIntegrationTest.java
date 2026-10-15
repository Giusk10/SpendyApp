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

    @Test
    @DisplayName("Should aggregate monthly expenses in database skipping positive amounts and other users")
    void testAggregateMonthlyExpenses() {
        // Month 1: two negative expenses and one positive income (which must be skipped)
        Expense jan1 = new Expense();
        jan1.setStartedDate(LocalDateTime.of(2026, 1, 10, 10, 0));
        jan1.setCompletedDate(LocalDateTime.of(2026, 1, 10, 10, 30));
        jan1.setAmount(new BigDecimal("-50.00"));
        jan1.setUsername("agg_user");

        Expense jan2 = new Expense();
        jan2.setStartedDate(LocalDateTime.of(2026, 1, 20, 15, 0));
        jan2.setCompletedDate(LocalDateTime.of(2026, 1, 20, 15, 30));
        jan2.setAmount(new BigDecimal("-25.50"));
        jan2.setUsername("agg_user");

        Expense janIncome = new Expense();
        janIncome.setStartedDate(LocalDateTime.of(2026, 1, 5, 8, 0));
        janIncome.setCompletedDate(LocalDateTime.of(2026, 1, 5, 8, 30));
        janIncome.setAmount(new BigDecimal("500.00")); // Positive income should be filtered out
        janIncome.setUsername("agg_user");

        // Month 2: one negative expense
        Expense feb1 = new Expense();
        feb1.setStartedDate(LocalDateTime.of(2026, 2, 14, 19, 0));
        feb1.setCompletedDate(LocalDateTime.of(2026, 2, 14, 21, 0));
        feb1.setAmount(new BigDecimal("-100.00"));
        feb1.setUsername("agg_user");

        // Different user in Jan: must NOT be included in agg_user totals
        Expense otherUser = new Expense();
        otherUser.setStartedDate(LocalDateTime.of(2026, 1, 15, 12, 0));
        otherUser.setCompletedDate(LocalDateTime.of(2026, 1, 15, 12, 30));
        otherUser.setAmount(new BigDecimal("-80.00"));
        otherUser.setUsername("other_user");

        // Different year (2025): must NOT be included in 2026 totals
        Expense prevYear = new Expense();
        prevYear.setStartedDate(LocalDateTime.of(2025, 12, 20, 12, 0));
        prevYear.setCompletedDate(LocalDateTime.of(2025, 12, 20, 12, 30));
        prevYear.setAmount(new BigDecimal("-45.00"));
        prevYear.setUsername("agg_user");

        expenseRepository.saveAll(List.of(jan1, jan2, janIncome, feb1, otherUser, prevYear));

        LocalDateTime start = LocalDateTime.of(2026, 1, 1, 0, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 12, 31, 23, 59, 59);

        List<com.spendyteam.expense.DTO.MonthlyExpenseTotal> totals =
                expenseRepository.aggregateMonthlyExpenses("agg_user", start, end);

        assertNotNull(totals);
        assertEquals(2, totals.size());

        assertEquals("2026-01", totals.get(0).getMonthKey());
        assertEquals(new BigDecimal("-75.50"), totals.get(0).getTotalAmount());

        assertEquals("2026-02", totals.get(1).getMonthKey());
        assertEquals(new BigDecimal("-100.00"), totals.get(1).getTotalAmount());
    }

    @Test
    @DisplayName("Should paginate user expenses using Spring Data Pageable")
    void testPaginationByUsername() {
        for (int i = 1; i <= 5; i++) {
            Expense e = new Expense();
            e.setStartedDate(LocalDateTime.of(2026, 3, i, 10, 0));
            e.setCompletedDate(LocalDateTime.of(2026, 3, i, 10, 30));
            e.setAmount(new BigDecimal("-" + i + "0.00"));
            e.setUsername("page_user");
            expenseRepository.save(e);
        }

        org.springframework.data.domain.Page<Expense> firstPage =
                expenseRepository.findByUsername("page_user", org.springframework.data.domain.PageRequest.of(0, 2));

        assertEquals(5, firstPage.getTotalElements());
        assertEquals(3, firstPage.getTotalPages());
        assertEquals(2, firstPage.getContent().size());
    }
}
