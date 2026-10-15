package com.spendyteam.expense.Service;

import com.spendyteam.expense.DTO.MonthlyExpenseTotal;
import com.spendyteam.expense.Data.Expense;
import com.spendyteam.expense.Repository.IExpenseRepository;
import com.spendyteam.expense.Utility.ExpenseDataParser;
import jakarta.ws.rs.core.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Core business service for managing financial expenses, coordinating CSV imports,
 * date-based filtering, aggregations, and CRUD operations.
 */
@Service("expenseService")
public class ExpenseService {

    @Autowired
    private IExpenseRepository expenseRepository;

    @Autowired
    private SmartCategorizerService smartCategorizerService;

    @Autowired
    private UserAuthService userAuthService;

    @Autowired
    private ExpenseDataParser expenseDataParser;

    @Autowired
    private ExpenseCsvParser expenseCsvParser;

    @Autowired
    private ExpenseBatchWriter expenseBatchWriter;

    public ExpenseService() {
        this.userAuthService = new UserAuthService();
        this.expenseDataParser = new ExpenseDataParser();
        this.expenseCsvParser = new ExpenseCsvParser(this.expenseDataParser, this.smartCategorizerService);
        this.expenseBatchWriter = new ExpenseBatchWriter(this.expenseRepository);
    }

    /**
     * Constructor for unit tests supplying a mock {@link WebClient}.
     */
    public ExpenseService(WebClient webClient) {
        this.userAuthService = new UserAuthService(webClient);
        this.expenseDataParser = new ExpenseDataParser();
        this.expenseCsvParser = new ExpenseCsvParser(this.expenseDataParser, this.smartCategorizerService);
        this.expenseBatchWriter = new ExpenseBatchWriter(this.expenseRepository);
    }

    /**
     * Full dependency injection constructor adhering to Spring Boot best practices.
     */
    @Autowired
    public ExpenseService(
            IExpenseRepository expenseRepository,
            SmartCategorizerService smartCategorizerService,
            UserAuthService userAuthService,
            ExpenseDataParser expenseDataParser,
            ExpenseCsvParser expenseCsvParser,
            ExpenseBatchWriter expenseBatchWriter) {
        this.expenseRepository = expenseRepository;
        this.smartCategorizerService = smartCategorizerService;
        this.userAuthService = userAuthService != null ? userAuthService : new UserAuthService();
        this.expenseDataParser = expenseDataParser != null ? expenseDataParser : new ExpenseDataParser();
        this.expenseCsvParser = expenseCsvParser != null ? expenseCsvParser : new ExpenseCsvParser(this.expenseDataParser, smartCategorizerService);
        this.expenseBatchWriter = expenseBatchWriter != null ? expenseBatchWriter : new ExpenseBatchWriter(expenseRepository);
    }

    /**
     * Imports expenses from an uploaded CSV file, executing parallel line parsing and throttled batch persistence.
     */
    public Response importExpensesFromCsv(MultipartFile file, String token) throws Exception {
        if (file == null || file.isEmpty()) {
            return Response.status(Response.Status.NO_CONTENT).entity("CSV file is empty.").build();
        }

        // 1. User validation via Gateway
        String username = getUsernameFromTokenViaRest(token);
        if (username == null) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Invalid Token").build();
        }

        try {
            // Phase 1: Parsing CSV rows in parallel
            ExpenseCsvParser.CsvParseResult parseResult = expenseCsvParser.parse(file, username, this.smartCategorizerService);

            if (parseResult.expenses().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Nessuna riga valida trovata. Errori di parsing: " + parseResult.parseErrors())
                        .build();
            }

            // Phase 2: Batch saving with semaphore throttling
            long dbErrors = expenseBatchWriter.writeInBatches(this.expenseRepository, parseResult.expenses());

            String msg = String.format("Import completato. Importati: %d. Errori Parsing: %d. Errori DB: %d",
                    parseResult.expenses().size() - dbErrors, parseResult.parseErrors(), dbErrors);

            return Response.ok(msg).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Critical error: " + e.getMessage()).build();
        }
    }

    /**
     * Retrieves all expenses for the authenticated user, sorted by start date ascending.
     */
    @Transactional(readOnly = true)
    public Response getExpenses(String token) {
        try {
            String username = getUsernameFromTokenViaRest(token);
            if (username == null) {
                return Response.status(Response.Status.UNAUTHORIZED).entity("Invalid or expired token.").build();
            }

            List<Expense> expenses = expenseRepository.findByUsername(
                    username,
                    Sort.by(Sort.Direction.ASC, "startedDate")
            );

            if (expenses == null || expenses.isEmpty()) {
                return Response.status(Response.Status.NO_CONTENT).entity("No expenses found.").build();
            }
            return Response.ok(expenses).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Failed to retrieve expenses: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Retrieves expenses within a specific date range for the authenticated user.
     */
    @Transactional(readOnly = true)
    public Response getExpenseByDate(String startedDate, String endDate, String token) {
        String username = getUsernameFromTokenViaRest(token);
        if (username == null) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Invalid or expired token.").build();
        }

        try {
            LocalDateTime start = parseToLocalDateTime(startedDate);
            LocalDateTime parsedEnd = parseToLocalDateTime(endDate);
            if (start == null || parsedEnd == null) {
                return Response.status(Response.Status.BAD_REQUEST).entity("Invalid date format.").build();
            }

            LocalDateTime end = parsedEnd.toLocalTime().equals(LocalTime.MIN)
                    ? parsedEnd.plusDays(1).minusSeconds(1)
                    : parsedEnd;

            List<Expense> expenses = expenseRepository.findExpensesByDateRange(username, start, end);

            if (expenses.isEmpty()) {
                return Response.status(Response.Status.NO_CONTENT).entity("No expenses found for the given date range.").build();
            }
            return Response.ok(expenses).build();
        } catch (DateTimeParseException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Invalid date format: " + e.getMessage()).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Failed to retrieve expenses: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Retrieves expenses for a specific month and year.
     */
    @Transactional(readOnly = true)
    public Response getExpenseByMonth_Year(String month, String year, String token) {
        String username = getUsernameFromTokenViaRest(token);
        if (username == null) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Invalid or expired token.").build();
        }

        try {
            int y = Integer.parseInt(year);
            int m = Integer.parseInt(month);
            YearMonth ym = YearMonth.of(y, m);
            LocalDateTime start = ym.atDay(1).atStartOfDay();
            LocalDateTime end = ym.atEndOfMonth().atTime(23, 59, 59);

            List<Expense> expenses = expenseRepository.findExpensesByDateRange(username, start, end);

            if (expenses.isEmpty()) {
                return Response.status(Response.Status.NO_CONTENT).entity("No expenses found for the given date range.").build();
            }
            return Response.ok(expenses).build();
        } catch (NumberFormatException | DateTimeException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Invalid month or year format: " + e.getMessage()).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Failed to retrieve expenses: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Calculates monthly aggregated expense amounts for a given year.
     */
    @Transactional(readOnly = true)
    public Response getMonthly_Amount_of_Year(String year, String token) {
        String username = getUsernameFromTokenViaRest(token);
        if (username == null) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Invalid or expired token.").build();
        }

        try {
            int y = Integer.parseInt(year);
            LocalDateTime start = LocalDateTime.of(y, 1, 1, 0, 0, 0);
            LocalDateTime end = LocalDateTime.of(y, 12, 31, 23, 59, 59);

            List<MonthlyExpenseTotal> totals = expenseRepository.aggregateMonthlyExpenses(username, start, end);

            if (totals.isEmpty()) {
                return Response.status(Response.Status.NO_CONTENT).entity("No expenses found for the given year.").build();
            }

            Map<String, BigDecimal> monthlyAmount = new LinkedHashMap<>();
            for (MonthlyExpenseTotal total : totals) {
                if (total.getMonthKey() != null) {
                    monthlyAmount.put(total.getMonthKey(), total.getTotalAmount() != null ? total.getTotalAmount() : BigDecimal.ZERO);
                }
            }

            return Response.ok(monthlyAmount).build();
        } catch (NumberFormatException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Invalid year format: " + e.getMessage()).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Failed to retrieve monthly amounts: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Deletes an individual expense after confirming user authorization.
     */
    @Transactional
    public Response deleteExpense(String expenseId, String token) {
        try {
            String username = getUsernameFromTokenViaRest(token);
            if (username == null) {
                return Response.status(Response.Status.UNAUTHORIZED).entity("Invalid or expired token.").build();
            }

            Optional<Expense> expenseOpt = expenseRepository.findById(expenseId);
            if (expenseOpt.isPresent()) {
                Expense expense = expenseOpt.get();
                if (!username.equals(expense.getUsername())) {
                    return Response.status(Response.Status.FORBIDDEN).entity("You are not authorized to delete this expense.").build();
                }
                expenseRepository.deleteById(expenseId);
                return Response.status(Response.Status.OK).entity("Expense deleted successfully.").build();
            } else {
                return Response.status(Response.Status.NOT_FOUND).entity("Expense not found.").build();
            }
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Failed to delete expense: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Adds a new expense record with automatic category classification.
     */
    @Transactional
    public Response addExpense(Map<String, String> body, String token) {
        try {
            Expense expense = new Expense();

            expense.setType(body.get("type"));
            expense.setProduct(body.get("product"));
            expense.setStartedDate(parseToLocalDateTime(body.get("startedDate")));
            expense.setCompletedDate(parseToLocalDateTime(body.get("completedDate")));
            expense.setDescription(body.get("description"));
            expense.setAmount(parseBigDecimal(body.get("amount")));
            expense.setFee(parseBigDecimal(body.get("fee")));
            expense.setCurrency(body.get("currency"));
            expense.setState(body.get("state"));

            if (smartCategorizerService != null) {
                expense.setCategory(smartCategorizerService.predictCategory(body.get("description")));
            }

            String username = getUsernameFromTokenViaRest(token);
            if (username == null) {
                return Response.status(Response.Status.UNAUTHORIZED).entity("Invalid or expired token").build();
            }
            expense.setUsername(username);

            expenseRepository.save(expense);
            return Response.status(Response.Status.CREATED).entity(expense).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Failed to add expense: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Updates an existing expense record after confirming user ownership.
     */
    @Transactional
    public Response updateExpense(Map<String, String> body, String token) {
        try {
            String expenseId = body.get("id");
            if (expenseId == null || expenseId.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST).entity("Expense ID is required for update.").build();
            }

            Optional<Expense> expenseOpt = expenseRepository.findById(expenseId);
            if (expenseOpt.isEmpty()) {
                return Response.status(Response.Status.NOT_FOUND).entity("Expense not found.").build();
            }

            Expense expense = expenseOpt.get();

            String username = getUsernameFromTokenViaRest(token);
            if (username == null) {
                return Response.status(Response.Status.UNAUTHORIZED).entity("Invalid or expired token").build();
            }
            if (!username.equals(expense.getUsername())) {
                return Response.status(Response.Status.FORBIDDEN).entity("You are not authorized to update this expense.").build();
            }

            if (body.get("type") != null) {
                expense.setType(body.get("type"));
            }
            if (body.get("startedDate") != null) {
                expense.setStartedDate(parseToLocalDateTime(body.get("startedDate")));
            }
            if (body.get("completedDate") != null) {
                expense.setCompletedDate(parseToLocalDateTime(body.get("completedDate")));
            }
            if (body.get("description") != null) {
                expense.setDescription(body.get("description"));
                if (smartCategorizerService != null) {
                    expense.setCategory(smartCategorizerService.predictCategory(body.get("description")));
                }
            }
            if (body.get("amount") != null) {
                expense.setAmount(parseBigDecimal(body.get("amount")));
            }

            expenseRepository.save(expense);
            return Response.status(Response.Status.OK).entity(expense).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Failed to update expense: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Deletes all expenses for the authenticated user directly at the database level.
     */
    @Transactional
    public Response deleteAllExpenses(String token) {
        try {
            String username = getUsernameFromTokenViaRest(token);
            if (username == null) {
                return Response.status(Response.Status.UNAUTHORIZED).entity("Invalid or expired token.").build();
            }

            long deletedCount = expenseRepository.deleteByUsername(username);

            if (deletedCount > 0) {
                return Response.status(Response.Status.OK)
                        .entity("Deleted " + deletedCount + " expenses for user: " + username)
                        .build();
            } else {
                return Response.status(Response.Status.OK)
                        .entity("No expenses found for user: " + username)
                        .build();
            }
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Failed to delete expenses: " + e.getMessage())
                    .build();
        }
    }

    // --- Helper delegation methods for backward compatibility ---

    public String getUsernameFromTokenViaRest(String token) {
        return userAuthService != null ? userAuthService.getUsernameFromToken(token) : null;
    }

    public LocalDateTime parseToLocalDateTime(String value) {
        return expenseDataParser != null ? expenseDataParser.parseToLocalDateTime(value) : null;
    }

    public BigDecimal parseBigDecimal(String value) {
        return expenseDataParser != null ? expenseDataParser.parseBigDecimal(value) : BigDecimal.ZERO;
    }
}
