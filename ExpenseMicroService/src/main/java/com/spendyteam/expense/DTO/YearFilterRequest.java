package com.spendyteam.expense.DTO;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Year filter payload for retrieving monthly expense aggregates")
public class YearFilterRequest {

    @Schema(description = "Four-digit year", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026")
    private String year;

    public YearFilterRequest() {}

    public YearFilterRequest(String year) {
        this.year = year;
    }

    public String getYear() { return year; }
    public void setYear(String year) { this.year = year; }
}
