package com.spendyteam.expense.Utility;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ExpenseStatusTest {

    @Test
    void testExpenseStatusExists() {
        assertNotNull(ExpenseStatus.SUCCESS);
        assertNotNull(ExpenseStatus.ERROR);
        assertNotNull(ExpenseStatus.NOT_FOUND);
        assertNotNull(ExpenseStatus.INVALID_INPUT);
        assertNotNull(ExpenseStatus.UNAUTHORIZED);
        assertNotNull(ExpenseStatus.FORBIDDEN);
        assertNotNull(ExpenseStatus.NO_CONTENT);
    }

    @Test
    void testGetStatus() {
        assertEquals("Success", ExpenseStatus.SUCCESS.getStatus());
        assertEquals("Error", ExpenseStatus.ERROR.getStatus());
        assertEquals("Not Found", ExpenseStatus.NOT_FOUND.getStatus());
        assertEquals("Invalid Input", ExpenseStatus.INVALID_INPUT.getStatus());
        assertEquals("Unauthorized", ExpenseStatus.UNAUTHORIZED.getStatus());
        assertEquals("Forbidden", ExpenseStatus.FORBIDDEN.getStatus());
        assertEquals("No Content", ExpenseStatus.NO_CONTENT.getStatus());
    }
}

