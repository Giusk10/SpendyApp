package com.spendyteam.expense.Utility;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ExpenseResultTest {

    @Test
    void testExpenseResultCreation() {
        ExpenseResult result = new ExpenseResult(ExpenseStatus.SUCCESS, "Operation successful", null);
        assertNotNull(result);
        assertEquals(ExpenseStatus.SUCCESS, result.getStatus());
        assertEquals("Operation successful", result.getMessage());
        assertNull(result.getExpenses());
    }

    @Test
    void testSetStatus() {
        ExpenseResult result = new ExpenseResult(ExpenseStatus.SUCCESS, "Success", null);
        result.setStatus(ExpenseStatus.ERROR);
        assertEquals(ExpenseStatus.ERROR, result.getStatus());
    }

    @Test
    void testSetMessage() {
        ExpenseResult result = new ExpenseResult(ExpenseStatus.SUCCESS, "Initial message", null);
        result.setMessage("Updated message");
        assertEquals("Updated message", result.getMessage());
    }

    @Test
    void testSetExpenses() {
        ExpenseResult result = new ExpenseResult(ExpenseStatus.SUCCESS, "Success", null);
        String newExpenses = "New expenses";
        result.setExpenses(newExpenses);
        assertEquals(newExpenses, result.getExpenses());
    }
}

