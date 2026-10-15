package com.spendyteam.expense.DTO;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Payload to delete a single expense record")
public class DeleteExpenseRequest {

    @Schema(description = "MongoDB identifier of the expense to delete", requiredMode = Schema.RequiredMode.REQUIRED, example = "65f2c4b8e1d4a23b12345678")
    private String expenseId;

    public DeleteExpenseRequest() {}

    public DeleteExpenseRequest(String expenseId) {
        this.expenseId = expenseId;
    }

    public String getExpenseId() { return expenseId; }
    public void setExpenseId(String expenseId) { this.expenseId = expenseId; }
}
