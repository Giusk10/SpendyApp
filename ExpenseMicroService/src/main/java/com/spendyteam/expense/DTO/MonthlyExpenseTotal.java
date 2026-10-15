package com.spendyteam.expense.DTO;

import java.math.BigDecimal;

/**
 * Projection representing aggregated monthly expense totals directly from the database.
 */
public interface MonthlyExpenseTotal {
    String getMonthKey();
    BigDecimal getTotalAmount();
}
