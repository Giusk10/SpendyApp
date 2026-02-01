package com.spendyteam.expense.Utility;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ExpenseDebtResultTest {

    @Test
    void testExpenseDebtResultCreation() {
        ExpenseDebtResult result = new ExpenseDebtResult(ExpenseStatus.SUCCESS, "Operation successful", null);
        assertNotNull(result);
        assertEquals(ExpenseStatus.SUCCESS, result.getStatus());
        assertEquals("Operation successful", result.getMessage());
        assertNull(result.getDebts());
    }

    @Test
    void testSetStatus() {
        ExpenseDebtResult result = new ExpenseDebtResult(ExpenseStatus.SUCCESS, "Success", null);
        result.setStatus(ExpenseStatus.ERROR);
        assertEquals(ExpenseStatus.ERROR, result.getStatus());
    }

    @Test
    void testSetMessage() {
        ExpenseDebtResult result = new ExpenseDebtResult(ExpenseStatus.SUCCESS, "Initial message", null);
        result.setMessage("Updated message");
        assertEquals("Updated message", result.getMessage());
    }

    @Test
    void testSetDebts() {
        ExpenseDebtResult result = new ExpenseDebtResult(ExpenseStatus.SUCCESS, "Success", null);
        String newDebts = "New debts";
        result.setDebts(newDebts);
        assertEquals(newDebts, result.getDebts());
    }
}

