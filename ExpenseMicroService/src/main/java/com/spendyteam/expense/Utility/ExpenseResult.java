package com.spendyteam.expense.Utility;

public class ExpenseResult {

    private ExpenseStatus status;
    private String message;
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