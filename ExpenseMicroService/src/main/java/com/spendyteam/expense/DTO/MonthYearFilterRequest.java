package com.spendyteam.expense.DTO;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Month and year filter payload for retrieving expenses")
public class MonthYearFilterRequest {

    @Schema(description = "Two-digit month number (01 to 12) or single-digit (1 to 12)", requiredMode = Schema.RequiredMode.REQUIRED, example = "03")
    private String month;

    @Schema(description = "Four-digit year", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026")
    private String year;

    public MonthYearFilterRequest() {}

    public MonthYearFilterRequest(String month, String year) {
        this.month = month;
        this.year = year;
    }

    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }

    public String getYear() { return year; }
    public void setYear(String year) { this.year = year; }
}
