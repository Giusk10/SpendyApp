package com.spendyteam.expense.DTO;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Payload to create a new expense entry")
public class ExpenseCreateRequest {

    @Schema(description = "Transaction type (e.g. CARD_PAYMENT, TRANSFER, CASH)", example = "CARD_PAYMENT")
    private String type;

    @Schema(description = "Product or merchant name", example = "Carrefour Supermarket")
    private String product;

    @Schema(description = "Transaction initiation timestamp (ISO-8601 or yyyy-MM-dd HH:mm:ss)", example = "2026-03-15 14:30:00")
    private String startedDate;

    @Schema(description = "Transaction completion timestamp (ISO-8601 or yyyy-MM-dd HH:mm:ss)", example = "2026-03-15 14:30:00")
    private String completedDate;

    @Schema(description = "Detailed description of the expense", example = "Weekly grocery shopping")
    private String description;

    @Schema(description = "Monetary amount spent", example = "45.50")
    private String amount;

    @Schema(description = "Fee charged for the transaction", example = "0.00")
    private String fee;

    @Schema(description = "Currency code in ISO 4217", example = "EUR")
    private String currency;

    @Schema(description = "Transaction state (e.g. COMPLETED, PENDING)", example = "COMPLETED")
    private String state;

    @Schema(description = "Expense category. If omitted, AI categorizer will assign automatically.", example = "Groceries")
    private String category;

    public ExpenseCreateRequest() {}

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getProduct() { return product; }
    public void setProduct(String product) { this.product = product; }

    public String getStartedDate() { return startedDate; }
    public void setStartedDate(String startedDate) { this.startedDate = startedDate; }

    public String getCompletedDate() { return completedDate; }
    public void setCompletedDate(String completedDate) { this.completedDate = completedDate; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getAmount() { return amount; }
    public void setAmount(String amount) { this.amount = amount; }

    public String getFee() { return fee; }
    public void setFee(String fee) { this.fee = fee; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}
