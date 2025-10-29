package com.spendyteam.expense.Utility;

public enum ExpenseStatus
{
    SUCCESS("Success"),
    ERROR("Error"),
    NOT_FOUND("Not Found"),
    INVALID_INPUT("Invalid Input"),
    UNAUTHORIZED("Unauthorized"),
    FORBIDDEN("Forbidden"),
    NO_CONTENT("No Content"),;

    private final String status;

    ExpenseStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}