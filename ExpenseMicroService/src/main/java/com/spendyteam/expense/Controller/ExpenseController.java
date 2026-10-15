package com.spendyteam.expense.Controller;

import com.spendyteam.expense.DTO.*;
import com.spendyteam.expense.Data.Expense;
import com.spendyteam.expense.Service.ExpenseService;
import com.spendyteam.expense.Utility.ExpenseResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.core.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

@Tag(name = "Expenses", description = "Endpoints for managing financial expenses, importing transactions, date filtering, and analytics")
@RestController
@RequestMapping("/rest/expense")
public class ExpenseController {

    @Autowired
    private ExpenseService expenseService;

    private String extractBearerToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ") || authHeader.length() <= 7) {
            return null;
        }
        String token = authHeader.substring(7).trim();
        return token.isEmpty() ? null : token;
    }

    @Operation(
        summary = "Import expenses from CSV file",
        description = "Parses a CSV file containing transactions and persists them, automatically classifying each expense with the NLP categorizer.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "CSV file imported successfully",
            content = @Content(mediaType = "text/plain", schema = @Schema(example = "Expenses imported successfully"))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "File is empty or CSV parsing failed",
            content = @Content(mediaType = "text/plain", schema = @Schema(example = "Failed to import CSV: The file is empty."))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Missing, invalid, or expired authentication token",
            content = @Content(mediaType = "text/plain", schema = @Schema(example = "Invalid or expired token."))
        )
    })
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> importCsv(
        @Parameter(description = "CSV file to import", required = true)
        @RequestParam("file") MultipartFile file,
        @Parameter(
            name = "Authorization",
            description = "JWT Bearer access token formatted as 'Bearer <token>'",
            required = true,
            in = ParameterIn.HEADER,
            schema = @Schema(type = "string", example = "Bearer eyJhbGciOi...")
        )
        @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        String token = extractBearerToken(authHeader);
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired token.");
        }
        try {
            Response res = expenseService.importExpensesFromCsv(file, token);
            if (res.getStatus() == 200) {
                return ResponseEntity.ok(res.getEntity().toString());
            } else {
                return ResponseEntity.status(res.getStatus()).body(res.getEntity().toString());
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to import CSV: " + e.getMessage());
        }
    }

    @Operation(
        summary = "Get all expenses",
        description = "Retrieves all expense records associated with the authenticated user.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Expenses retrieved successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExpenseResult.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Error occurred while retrieving expenses",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExpenseErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Invalid or expired authentication token",
            content = @Content(mediaType = "text/plain", schema = @Schema(example = "Invalid or expired token."))
        )
    })
    @GetMapping(value = "/getExpenses", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getExpenses(
        @Parameter(
            name = "Authorization",
            description = "JWT Bearer access token formatted as 'Bearer <token>'",
            required = true,
            in = ParameterIn.HEADER,
            schema = @Schema(type = "string", example = "Bearer eyJhbGciOi...")
        )
        @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        String token = extractBearerToken(authHeader);
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired token.");
        }
        try {
            Response res = expenseService.getExpenses(token);
            if (res.getStatus() == 200) {
                return ResponseEntity.ok().body(res.getEntity());
            } else {
                return ResponseEntity.status(res.getStatus()).body(res.getEntity());
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                java.util.Collections.singletonMap("error", "Failed to retrieve expenses: " + e.getMessage())
            );
        }
    }

    @Operation(
        summary = "Filter expenses by date range",
        description = "Retrieves all expenses between startedDate and completedDate for the authenticated user.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @RequestBody(
        description = "Date range boundaries in yyyy-MM-dd format",
        required = true,
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = DateFilterRequest.class))
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Expenses in date range retrieved successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExpenseResult.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid date format or processing failure",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExpenseErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Invalid or expired authentication token",
            content = @Content(mediaType = "text/plain", schema = @Schema(example = "Invalid or expired token."))
        )
    })
    @PostMapping(value = "/getExpenseByDate", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getExpenseByDate(
        @org.springframework.web.bind.annotation.RequestBody Map<String, String> body,
        @Parameter(
            name = "Authorization",
            description = "JWT Bearer access token formatted as 'Bearer <token>'",
            required = true,
            in = ParameterIn.HEADER,
            schema = @Schema(type = "string", example = "Bearer eyJhbGciOi...")
        )
        @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        String token = extractBearerToken(authHeader);
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired token.");
        }
        try {
            String startedDate = body.get("startedDate");
            String completedDate = body.get("completedDate");

            Response res = expenseService.getExpenseByDate(startedDate, completedDate, token);
            if (res.getStatus() == 200) {
                return ResponseEntity.ok().body(res.getEntity());
            } else {
                return ResponseEntity.status(res.getStatus()).body(res.getEntity());
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                java.util.Collections.singletonMap("error", "Failed to retrieve expense: " + e.getMessage())
            );
        }
    }

    @Operation(
        summary = "Filter expenses by month and year",
        description = "Retrieves expenses for a specific month and year for the authenticated user.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @RequestBody(
        description = "Month and year filter payload",
        required = true,
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = MonthYearFilterRequest.class))
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Expenses for specified month retrieved successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExpenseResult.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid month/year format or processing error",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExpenseErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Invalid or expired authentication token",
            content = @Content(mediaType = "text/plain", schema = @Schema(example = "Invalid or expired token."))
        )
    })
    @PostMapping(value = "/getExpenseByMonth", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getExpenseByMonth_Year(
        @org.springframework.web.bind.annotation.RequestBody Map<String, String> body,
        @Parameter(
            name = "Authorization",
            description = "JWT Bearer access token formatted as 'Bearer <token>'",
            required = true,
            in = ParameterIn.HEADER,
            schema = @Schema(type = "string", example = "Bearer eyJhbGciOi...")
        )
        @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        String token = extractBearerToken(authHeader);
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired token.");
        }
        try {
            String month = body.get("month");
            String year = body.get("year");
            Response res = expenseService.getExpenseByMonth_Year(month, year, token);
            if (res.getStatus() == 200) {
                return ResponseEntity.ok().body(res.getEntity());
            } else {
                return ResponseEntity.status(res.getStatus()).body(res.getEntity());
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                java.util.Collections.singletonMap("error", "Failed to retrieve expenses: " + e.getMessage())
            );
        }
    }

    @Operation(
        summary = "Get monthly expense amounts for a year",
        description = "Calculates monthly expense aggregations for the specified year.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @RequestBody(
        description = "Target year payload",
        required = true,
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = YearFilterRequest.class))
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Monthly amounts retrieved successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExpenseResult.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid year format or calculation error",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExpenseErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Invalid or expired authentication token",
            content = @Content(mediaType = "text/plain", schema = @Schema(example = "Invalid or expired token."))
        )
    })
    @PostMapping(value = "/getMonthlyAmountOfYear", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getMonthly_Amount_of_Year(
        @org.springframework.web.bind.annotation.RequestBody Map<String, String> body,
        @Parameter(
            name = "Authorization",
            description = "JWT Bearer access token formatted as 'Bearer <token>'",
            required = true,
            in = ParameterIn.HEADER,
            schema = @Schema(type = "string", example = "Bearer eyJhbGciOi...")
        )
        @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        String token = extractBearerToken(authHeader);
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired token.");
        }
        try {
            String year = body.get("year");
            Response res = expenseService.getMonthly_Amount_of_Year(year, token);
            if (res.getStatus() == 200) {
                return ResponseEntity.ok().body(res.getEntity());
            } else {
                return ResponseEntity.status(res.getStatus()).body(res.getEntity());
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                java.util.Collections.singletonMap("error", "Failed to retrieve monthly amounts: " + e.getMessage())
            );
        }
    }

    @Operation(
        summary = "Delete single expense",
        description = "Deletes an expense record by its unique identifier for the authenticated user.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @RequestBody(
        description = "Payload containing ID of expense to delete",
        required = true,
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = DeleteExpenseRequest.class))
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Expense successfully deleted",
            content = @Content(mediaType = "text/plain", schema = @Schema(example = "Expense deleted successfully"))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Failed to delete expense",
            content = @Content(mediaType = "text/plain", schema = @Schema(example = "Failed to delete expense: ..."))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Invalid or expired authentication token",
            content = @Content(mediaType = "text/plain", schema = @Schema(example = "Invalid or expired token."))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Expense record not found",
            content = @Content(mediaType = "text/plain", schema = @Schema(example = "Expense not found."))
        )
    })
    @DeleteMapping(value = "/deleteExpense", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> deleteExpense(
        @org.springframework.web.bind.annotation.RequestBody Map<String, String> body,
        @Parameter(
            name = "Authorization",
            description = "JWT Bearer access token formatted as 'Bearer <token>'",
            required = true,
            in = ParameterIn.HEADER,
            schema = @Schema(type = "string", example = "Bearer eyJhbGciOi...")
        )
        @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        String token = extractBearerToken(authHeader);
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired token.");
        }
        try {
            Response res = expenseService.deleteExpense(body.get("expenseId"), token);
            if (res.getStatus() == 200) {
                return ResponseEntity.ok(res.getEntity());
            } else {
                return ResponseEntity.status(res.getStatus()).body(res.getEntity());
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to delete expense: " + e.getMessage());
        }
    }

    @Operation(
        summary = "Delete all expenses",
        description = "Deletes all expense records associated with the authenticated user.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "All user expenses successfully deleted",
            content = @Content(mediaType = "text/plain", schema = @Schema(example = "All expenses deleted successfully"))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Failed to delete expenses",
            content = @Content(mediaType = "text/plain", schema = @Schema(example = "Failed to delete all expenses: ..."))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Invalid or expired authentication token",
            content = @Content(mediaType = "text/plain", schema = @Schema(example = "Invalid or expired token."))
        )
    })
    @DeleteMapping(value = "/deleteAllExpenses", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> deleteAllExpenses(
        @Parameter(
            name = "Authorization",
            description = "JWT Bearer access token formatted as 'Bearer <token>'",
            required = true,
            in = ParameterIn.HEADER,
            schema = @Schema(type = "string", example = "Bearer eyJhbGciOi...")
        )
        @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        String token = extractBearerToken(authHeader);
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired token.");
        }
        try {
            Response res = expenseService.deleteAllExpenses(token);
            if (res.getStatus() == 200) {
                return ResponseEntity.ok(res.getEntity());
            } else {
                return ResponseEntity.status(res.getStatus()).body(res.getEntity());
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to delete all expenses: " + e.getMessage());
        }
    }

    @Operation(
        summary = "Add single expense",
        description = "Creates a new expense record for the authenticated user. If category is omitted, OpenNLP categorizer assigns one based on description.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @RequestBody(
        description = "Expense creation details",
        required = true,
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExpenseCreateRequest.class))
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Expense successfully created",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Expense.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid expense payload or processing error",
            content = @Content(mediaType = "text/plain", schema = @Schema(example = "Failed to add expense: ..."))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Invalid or expired authentication token",
            content = @Content(mediaType = "text/plain", schema = @Schema(example = "Invalid or expired token"))
        )
    })
    @PostMapping(value = "/addExpense", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> addExpense(
        @org.springframework.web.bind.annotation.RequestBody Map<String, String> body,
        @Parameter(
            name = "Authorization",
            description = "JWT Bearer access token formatted as 'Bearer <token>'",
            required = true,
            in = ParameterIn.HEADER,
            schema = @Schema(type = "string", example = "Bearer eyJhbGciOi...")
        )
        @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        String token = extractBearerToken(authHeader);
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired token");
        }
        try {
            Response res = expenseService.addExpense(body, token);
            if (res.getStatus() == 201) {
                return ResponseEntity.created(null).body(res.getEntity());
            } else {
                return ResponseEntity.status(res.getStatus()).body(res.getEntity().toString());
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to add expense: " + e.getMessage());
        }
    }

    @Operation(
        summary = "Update single expense",
        description = "Updates an existing expense entry by its ID for the authenticated user.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @RequestBody(
        description = "Expense update details with ID",
        required = true,
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExpenseUpdateRequest.class))
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Expense successfully updated",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Expense.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid expense update payload or processing error",
            content = @Content(mediaType = "text/plain", schema = @Schema(example = "Failed to update expense: ..."))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Invalid or expired authentication token",
            content = @Content(mediaType = "text/plain", schema = @Schema(example = "Invalid or expired token"))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Expense record not found",
            content = @Content(mediaType = "text/plain", schema = @Schema(example = "Expense not found."))
        )
    })
    @PostMapping(value = "/updateExpense", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> updateExpense(
        @org.springframework.web.bind.annotation.RequestBody Map<String, String> body,
        @Parameter(
            name = "Authorization",
            description = "JWT Bearer access token formatted as 'Bearer <token>'",
            required = true,
            in = ParameterIn.HEADER,
            schema = @Schema(type = "string", example = "Bearer eyJhbGciOi...")
        )
        @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        String token = extractBearerToken(authHeader);
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired token");
        }
        try {
            Response res = expenseService.updateExpense(body, token);
            if (res.getStatus() == 200) {
                return ResponseEntity.ok().body(res.getEntity());
            } else {
                return ResponseEntity.status(res.getStatus()).body(res.getEntity().toString());
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to update expense: " + e.getMessage());
        }
    }

    @Operation(
        summary = "Expense service health check",
        description = "Lightweight health check endpoint to verify that ExpenseMicroService is running."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Service is healthy and responding",
            content = @Content(mediaType = "text/plain", schema = @Schema(example = "Expense Microservice is up and running!"))
        )
    })
    @PostMapping(value = "/test", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> testEndpoint() {
        return ResponseEntity.ok("Expense Microservice is up and running!");
    }
}
