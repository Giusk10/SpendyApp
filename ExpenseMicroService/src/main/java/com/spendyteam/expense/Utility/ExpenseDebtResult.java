package com.spendyteam.expense.Utility;

public class ExpenseDebtResult {

    private ExpenseStatus status;
    private String message;
    private Object debts;

    public ExpenseDebtResult(ExpenseStatus status, String message, Object debts) {
        this.status = status;
        this.message = message;
        this.debts = debts;
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

    public Object getDebts() {
        return debts;
    }

    public void setDebts(Object debts) {
        this.debts = debts;
    }
}