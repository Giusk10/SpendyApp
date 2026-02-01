package com.spendyteam.expense.Data;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ExpenseTest {

    private Expense expense;

    @BeforeEach
    void setUp() {
        expense = new Expense();
    }

    @Test
    void testExpenseCreation() {
        assertNotNull(expense);
    }

    @Test
    void testSetAndGetType() {
        String type = "Payment";
        expense.setType(type);
        assertEquals(type, expense.getType());
    }

    @Test
    void testSetAndGetProduct() {
        String product = "Netflix Subscription";
        expense.setProduct(product);
        assertEquals(product, expense.getProduct());
    }

    @Test
    void testSetAndGetStartedDate() {
        LocalDateTime startedDate = LocalDateTime.of(2024, 1, 15, 10, 30);
        expense.setStartedDate(startedDate);
        assertEquals(startedDate, expense.getStartedDate());
    }

    @Test
    void testSetAndGetCompletedDate() {
        LocalDateTime completedDate = LocalDateTime.of(2024, 1, 15, 10, 35);
        expense.setCompletedDate(completedDate);
        assertEquals(completedDate, expense.getCompletedDate());
    }

    @Test
    void testSetAndGetDescription() {
        String description = "Monthly Netflix subscription";
        expense.setDescription(description);
        assertEquals(description, expense.getDescription());
    }

    @Test
    void testSetAndGetAmount() {
        BigDecimal amount = new BigDecimal("99.99");
        expense.setAmount(amount);
        assertEquals(amount, expense.getAmount());
    }

    @Test
    void testSetAndGetFee() {
        BigDecimal fee = new BigDecimal("2.50");
        expense.setFee(fee);
        assertEquals(fee, expense.getFee());
    }

    @Test
    void testSetAndGetCurrency() {
        String currency = "EUR";
        expense.setCurrency(currency);
        assertEquals(currency, expense.getCurrency());
    }

    @Test
    void testSetAndGetState() {
        String state = "COMPLETED";
        expense.setState(state);
        assertEquals(state, expense.getState());
    }

    @Test
    void testSetAndGetCategory() {
        String category = "Abbonamenti e Servizi Digitali";
        expense.setCategory(category);
        assertEquals(category, expense.getCategory());
    }

    @Test
    void testSetAndGetUsername() {
        String username = "testuser";
        expense.setUsername(username);
        assertEquals(username, expense.getUsername());
    }

    @Test
    void testToString() {
        expense.setType("Payment");
        expense.setProduct("Netflix");
        expense.setDescription("Monthly subscription");
        expense.setAmount(new BigDecimal("15.99"));
        expense.setFee(new BigDecimal("0.50"));
        expense.setCurrency("EUR");
        expense.setState("COMPLETED");
        expense.setCategory("Abbonamenti e Servizi Digitali");
        expense.setUsername("testuser");

        String result = expense.toString();

        assertNotNull(result);
        assertTrue(result.contains("Payment"));
        assertTrue(result.contains("Netflix"));
        assertTrue(result.contains("Monthly subscription"));
        assertTrue(result.contains("testuser"));
    }

    @Test
    void testExpenseWithAllFields() {
        LocalDateTime startedDate = LocalDateTime.of(2024, 1, 15, 10, 30);
        LocalDateTime completedDate = LocalDateTime.of(2024, 1, 15, 10, 35);

        expense.setType("Payment");
        expense.setProduct("Grocery Shopping");
        expense.setStartedDate(startedDate);
        expense.setCompletedDate(completedDate);
        expense.setDescription("Weekly groceries at Carrefour");
        expense.setAmount(new BigDecimal("85.50"));
        expense.setFee(new BigDecimal("1.20"));
        expense.setCurrency("EUR");
        expense.setState("COMPLETED");
        expense.setCategory("Supermercati e Alimentari");
        expense.setUsername("user123");

        assertEquals("Payment", expense.getType());
        assertEquals("Grocery Shopping", expense.getProduct());
        assertEquals(startedDate, expense.getStartedDate());
        assertEquals(completedDate, expense.getCompletedDate());
        assertEquals("Weekly groceries at Carrefour", expense.getDescription());
        assertEquals(new BigDecimal("85.50"), expense.getAmount());
        assertEquals(new BigDecimal("1.20"), expense.getFee());
        assertEquals("EUR", expense.getCurrency());
        assertEquals("COMPLETED", expense.getState());
        assertEquals("Supermercati e Alimentari", expense.getCategory());
        assertEquals("user123", expense.getUsername());
    }

    @Test
    void testNullValues() {
        expense.setType(null);
        expense.setProduct(null);
        expense.setDescription(null);
        expense.setCurrency(null);
        expense.setState(null);
        expense.setCategory(null);
        expense.setUsername(null);

        assertNull(expense.getType());
        assertNull(expense.getProduct());
        assertNull(expense.getDescription());
        assertNull(expense.getCurrency());
        assertNull(expense.getState());
        assertNull(expense.getCategory());
        assertNull(expense.getUsername());
    }
}

