package com.spendyteam.expense.Data;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Document(collection = "expenses")
public class Expense {

    @Id
    private String id;

    @Field("type")
    private String type;

    @Field("product")
    private String product;

    @Field("startedDate")
    private LocalDateTime startedDate;

    @Field("completedDate")
    private LocalDateTime completedDate;

    @Field("description")
    private String description;

    @Field("amount")
    private BigDecimal amount;

    @Field("fee")
    private BigDecimal fee;

    @Field("currency")
    private String currency;

    @Field("state")
    private String state;

    @Field("category")
    private String category;

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
                '}';
    }
}
