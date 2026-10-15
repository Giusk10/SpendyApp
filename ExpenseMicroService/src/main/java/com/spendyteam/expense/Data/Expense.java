package com.spendyteam.expense.Data;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "expenses", indexes = {
    @Index(name = "idx_expenses_username", columnList = "username"),
    @Index(name = "idx_expenses_user_started", columnList = "username, started_date"),
    @Index(name = "idx_expenses_user_completed", columnList = "username, completed_date"),
    @Index(name = "idx_expenses_user_category", columnList = "username, category")
})
@Schema(description = "Expense entity representing a financial transaction record")
public class Expense {

    @Id
    @Column(name = "id", length = 64, nullable = false, updatable = false)
    @Schema(description = "Unique identifier of the expense (UUID)", accessMode = Schema.AccessMode.READ_ONLY, example = "65a1234b-5c6d-7e8f-9012-345678abcdef")
    private String id;

    @Column(name = "type", length = 50)
    @Schema(description = "Transaction type (e.g. CARD_PAYMENT, TRANSFER, CASH)", example = "CARD_PAYMENT")
    private String type;

    @Column(name = "product", length = 255)
    @Schema(description = "Product or merchant name", example = "Supermarket Grocery")
    private String product;

    @Column(name = "started_date")
    @Schema(description = "Date and time when the expense started", example = "2026-03-15T14:30:00")
    private LocalDateTime startedDate;

    @Column(name = "completed_date")
    @Schema(description = "Date and time when the expense completed", example = "2026-03-15T14:30:00")
    private LocalDateTime completedDate;

    @Column(name = "description", length = 500)
    @Schema(description = "Detailed description of the expense", example = "Weekly grocery shopping at supermarket")
    private String description;

    @Column(name = "amount", precision = 15, scale = 2, nullable = false)
    @Schema(description = "Monetary amount of the transaction", example = "45.50")
    private BigDecimal amount;

    @Column(name = "fee", precision = 15, scale = 2)
    @Schema(description = "Transaction fee, if any", example = "0.00")
    private BigDecimal fee;

    @Column(name = "currency", length = 10)
    @Schema(description = "Currency code in ISO 4217 format", example = "EUR")
    private String currency;

    @Column(name = "state", length = 50)
    @Schema(description = "State of transaction (e.g. COMPLETED, PENDING, REVERTED)", example = "COMPLETED")
    private String state;

    @Column(name = "category", length = 100)
    @Schema(description = "AI categorized expense category (e.g. Groceries, Entertainment, Utilities)", example = "Groceries")
    private String category;

    @Column(name = "username", length = 50, nullable = false)
    @Schema(description = "Username owning this expense record", example = "mario_rossi")
    private String username;

    @PrePersist
    public void prePersist() {
        if (this.id == null || this.id.isBlank()) {
            this.id = UUID.randomUUID().toString();
        }
        if (this.fee == null) {
            this.fee = BigDecimal.ZERO;
        }
    }

    public Expense() {}

    // Getters & Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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
