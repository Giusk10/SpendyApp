package com.spendyteam.expense.Utility;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response wrapper for expense operations")
public class ExpenseResult {

    @Schema(description = "Status of the expense operation", example = "SUCCESS")
    private ExpenseStatus status;

    @Schema(description = "Result explanation or status message", example = "Expenses retrieved successfully")
    private String message;

    @Schema(description = "Payload containing expense list, single expense, or monthly totals")
    private Object expenses;

    public ExpenseResult(ExpenseStatus status, String message, Object expenses) {
        this.status = status;
        this.message = message;
        this.expenses = expenses;
    }

    public ExpenseStatus getStatus() {
        return status;
    }

    public void setStatus(ExpenseStatus status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getExpenses() {
        return expenses;
    }

    public void setExpenses(Object expenses) {
        this.expenses = expenses;
    }
}