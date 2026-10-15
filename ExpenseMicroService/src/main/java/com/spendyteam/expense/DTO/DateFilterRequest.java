package com.spendyteam.expense.DTO;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Date range filter for retrieving expenses")
public class DateFilterRequest {

    @Schema(description = "Start date in yyyy-MM-dd format", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-01-01")
    private String startedDate;

    @Schema(description = "End date in yyyy-MM-dd format", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-01-31")
    private String completedDate;

    public DateFilterRequest() {}

    public DateFilterRequest(String startedDate, String completedDate) {
        this.startedDate = startedDate;
        this.completedDate = completedDate;
    }

    public String getStartedDate() { return startedDate; }
    public void setStartedDate(String startedDate) { this.startedDate = startedDate; }

    public String getCompletedDate() { return completedDate; }
    public void setCompletedDate(String completedDate) { this.completedDate = completedDate; }
}
