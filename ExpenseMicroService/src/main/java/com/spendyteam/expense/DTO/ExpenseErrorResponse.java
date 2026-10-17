package com.spendyteam.expense.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Standard error response for expense operations")
public class ExpenseErrorResponse {

    @Schema(description = "High-level error classification or failure message", example = "Bad Request")
    private String error;

    @Schema(description = "Detailed human-readable error description", example = "Invalid date format: '2024-99-99'")
    private String message;

    @Schema(description = "HTTP status code", example = "400")
    private int status;

    @Schema(description = "Request path where the error occurred", example = "/rest/expense/getExpenseByDate")
    private String path;

    @Schema(description = "Distributed correlation ID for diagnostics", example = "d94b156a-7b3b-4786-8f35-6453f669db69")
    private String correlationId;

    @Schema(description = "Timestamp when the error occurred", example = "2026-03-15T14:30:00Z")
    private Instant timestamp;

    public ExpenseErrorResponse() {
        this.timestamp = Instant.now();
    }

    public ExpenseErrorResponse(String error) {
        this.error = error;
        this.message = error;
        this.timestamp = Instant.now();
    }

    public ExpenseErrorResponse(int status, String error, String message, String path, String correlationId) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
        this.correlationId = correlationId;
        this.timestamp = Instant.now();
    }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
