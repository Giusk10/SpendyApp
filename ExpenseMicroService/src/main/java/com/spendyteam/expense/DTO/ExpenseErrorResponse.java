package com.spendyteam.expense.DTO;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Standard error response for expense operations")
public class ExpenseErrorResponse {

    @Schema(description = "Detailed error message describing the failure", example = "Failed to retrieve expenses: Invalid date format")
    private String error;

    public ExpenseErrorResponse() {}

    public ExpenseErrorResponse(String error) {
        this.error = error;
    }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}
