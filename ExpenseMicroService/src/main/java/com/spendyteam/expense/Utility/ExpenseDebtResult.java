package com.spendyteam.expense.Utility;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response wrapper for expense debt operations")
public class ExpenseDebtResult {

    @Schema(description = "Status of the debt operation", example = "SUCCESS")
    private ExpenseStatus status;

    @Schema(description = "Result explanation or status message", example = "Debts retrieved successfully")
    private String message;

    @Schema(description = "Payload containing debt details")
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