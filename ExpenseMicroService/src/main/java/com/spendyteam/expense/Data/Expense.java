package com.spendyteam.expense.Data;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Document(collection = "expenses")
@Schema(description = "Expense entity representing a financial transaction record")
public class Expense {

    @Id
    @Schema(description = "Unique identifier of the expense (MongoDB ID)", accessMode = Schema.AccessMode.READ_ONLY, example = "65a1234b5c6d7e8f90123456")
    private String id;

    @Field("type")
    @Schema(description = "Transaction type (e.g. CARD_PAYMENT, TRANSFER, CASH)", example = "CARD_PAYMENT")
    private String type;

    @Field("product")
    @Schema(description = "Product or merchant name", example = "Supermarket Grocery")
    private String product;

    @Field("startedDate")
    @Schema(description = "Date and time when the expense started", example = "2026-03-15T14:30:00")
    private LocalDateTime startedDate;

    @Field("completedDate")
    @Schema(description = "Date and time when the expense completed", example = "2026-03-15T14:30:00")
    private LocalDateTime completedDate;

    @Field("description")
    @Schema(description = "Detailed description of the expense", example = "Weekly grocery shopping at supermarket")
    private String description;

    @Field("amount")
    @Schema(description = "Monetary amount of the transaction", example = "45.50")
    private BigDecimal amount;

    @Field("fee")
    @Schema(description = "Transaction fee, if any", example = "0.00")
    private BigDecimal fee;

    @Field("currency")
    @Schema(description = "Currency code in ISO 4217 format", example = "EUR")
    private String currency;

    @Field("state")
    @Schema(description = "State of transaction (e.g. COMPLETED, PENDING, REVERTED)", example = "COMPLETED")
    private String state;

    @Field("category")
    @Schema(description = "AI categorized expense category (e.g. Groceries, Entertainment, Utilities)", example = "Groceries")
    private String category;

    @Field("username")
    @Schema(description = "Username owning this expense record", example = "mario_rossi")
    private String username;

    // Getters & Setters
    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public LocalDateTime getStartedDate() {
        return startedDate;
    }

    public void setStartedDate(LocalDateTime startedDate) {
        this.startedDate = startedDate;
    }

    public LocalDateTime getCompletedDate() {
        return completedDate;
    }

    public void setCompletedDate(LocalDateTime completedDate) {
        this.completedDate = completedDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getFee() {
        return fee;
    }

    public void setFee(BigDecimal fee) {
        this.fee = fee;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getUsername() {
        return username;
    }
    public void setUsername(String userId) {
        this.username = userId;
    }

    @Override
    public String toString() {
        return "Expense{" +
                "id='" + id + '\'' +
                ", type='" + type + '\'' +
                ", product='" + product + '\'' +
                ", startedDate=" + startedDate +
                ", completedDate=" + completedDate +
                ", description='" + description + '\'' +
                ", amount=" + amount +
                ", fee=" + fee +
                ", currency='" + currency + '\'' +
                ", state='" + state + '\'' +
                ", category='" + category + '\'' +
                ", username='" + username + '\'' +
                '}';
    }
}
