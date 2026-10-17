package com.spendyteam.expense.Service;

import com.spendyteam.expense.Data.Expense;
import com.spendyteam.expense.Repository.IExpenseRepository;
import com.spendyteam.expense.Utility.ExpenseDataParser;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Extended Business Logic and Edge Case Tests for ExpenseService")
class ExpenseServiceExtendedTest {

    @Mock
    private IExpenseRepository expenseRepository;

    @Mock
    private SmartCategorizerService smartCategorizerService;

    @Mock
    private UserAuthService userAuthService;

    @Mock
    private ExpenseBatchWriter expenseBatchWriter;

    private ExpenseService expenseService;

    @BeforeEach
    void setUp() {
        ExpenseDataParser dataParser = new ExpenseDataParser();
        ExpenseCsvParser csvParser = new ExpenseCsvParser(dataParser, smartCategorizerService);

        expenseService = new ExpenseService(
                expenseRepository,
                smartCategorizerService,
                userAuthService,
                dataParser,
                csvParser,
                expenseBatchWriter
        );
    }

    @Test
    @DisplayName("Should return HTTP 500 when all database write batches fail")
    void testImportCsvCompleteDatabaseFailure() throws Exception {
        String csv = "Type,Product,Started Date,Completed Date,Description,Amount,Fee,Currency,State\n" +
                "Payment,Shop,2024-01-01 10:00:00,2024-01-01 10:05:00,Item A,10.00,0.00,EUR,COMPLETED\n" +
                "Payment,Shop,2024-01-02 10:00:00,2024-01-02 10:05:00,Item B,20.00,0.00,EUR,COMPLETED\n";

        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        when(userAuthService.getUsernameFromToken(anyString())).thenReturn("test_user");
        when(expenseRepository.existsByUsernameAndStartedDateAndAmountAndDescription(any(), any(), any(), any()))
                .thenReturn(false);
        // Simulate all records failing to save
        when(expenseBatchWriter.writeInBatches(eq(expenseRepository), anyList())).thenReturn(2L);

        Response response = expenseService.importExpensesFromCsv(file, "token-123");

        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        assertTrue(response.getEntity().toString().contains("Import fallito per errori di persistenza"));
    }

    @Test
    @DisplayName("Should return HTTP 207 Multi-Status when import is partially successful")
    void testImportCsvPartialSuccess() throws Exception {
        String csv = "Type,Product,Started Date,Completed Date,Description,Amount,Fee,Currency,State\n" +
                "Payment,Shop,2024-01-01 10:00:00,2024-01-01 10:05:00,Item A,10.00,0.00,EUR,COMPLETED\n" +
                "Payment,Shop,2024-01-02 10:00:00,2024-01-02 10:05:00,Item B,20.00,0.00,EUR,COMPLETED\n";

        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        when(userAuthService.getUsernameFromToken(anyString())).thenReturn("test_user");
        when(expenseRepository.existsByUsernameAndStartedDateAndAmountAndDescription(any(), any(), any(), any()))
                .thenReturn(false);
        // Simulate 1 out of 2 records failing
        when(expenseBatchWriter.writeInBatches(eq(expenseRepository), anyList())).thenReturn(1L);

        Response response = expenseService.importExpensesFromCsv(file, "token-123");

        assertEquals(207, response.getStatus());
        assertTrue(response.getEntity().toString().contains("Import parzialmente riuscito"));
    }

    @Test
    @DisplayName("Should skip duplicates when deduplication is enabled")
    void testImportCsvWithDeduplication() throws Exception {
        String csv = "Type,Product,Started Date,Completed Date,Description,Amount,Fee,Currency,State\n" +
                "Payment,Shop,2024-01-01 10:00:00,2024-01-01 10:05:00,Duplicate Item,10.00,0.00,EUR,COMPLETED\n" +
                "Payment,Shop,2024-01-02 10:00:00,2024-01-02 10:05:00,New Item,20.00,0.00,EUR,COMPLETED\n";

        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        when(userAuthService.getUsernameFromToken(anyString())).thenReturn("test_user");

        // First item already exists in database
        when(expenseRepository.existsByUsernameAndStartedDateAndAmountAndDescription(
                eq("test_user"), eq(LocalDateTime.of(2024, 1, 1, 10, 0, 0)),
                eq(new BigDecimal("10.00")), eq("Duplicate Item")))
                .thenReturn(true);

        // Second item does not exist
        when(expenseRepository.existsByUsernameAndStartedDateAndAmountAndDescription(
                eq("test_user"), eq(LocalDateTime.of(2024, 1, 2, 10, 0, 0)),
                eq(new BigDecimal("20.00")), eq("New Item")))
                .thenReturn(false);

        when(expenseBatchWriter.writeInBatches(eq(expenseRepository), argThat(list -> list.size() == 1)))
                .thenReturn(0L);

        Response response = expenseService.importExpensesFromCsv(file, "token-123", true);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertTrue(response.getEntity().toString().contains("Importati: 1"));
    }

    @Test
    @DisplayName("Should retrieve paginated expenses with safe limit boundaries")
    void testGetExpensesPaged() {
        when(userAuthService.getUsernameFromToken(anyString())).thenReturn("page_user");

        Expense exp = new Expense();
        exp.setDescription("Page expense");
        Page<Expense> page = new PageImpl<>(List.of(exp));

        when(expenseRepository.findByUsername(eq("page_user"), any(Pageable.class)))
                .thenReturn(page);

        Response response = expenseService.getExpensesPaged("token-123", 0, 10, "startedDate", "asc");

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertNotNull(response.getEntity());
        verify(expenseRepository).findByUsername(eq("page_user"), any(Pageable.class));
    }

    @Test
    @DisplayName("Should return 401 when token is invalid for paginated query")
    void testGetExpensesPagedUnauthorized() {
        when(userAuthService.getUsernameFromToken(anyString())).thenReturn(null);

        Response response = expenseService.getExpensesPaged("invalid-token", 0, 10, "startedDate", "asc");

        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
    }

    @Test
    @DisplayName("Should return 400 Bad Request when startedDate is after completedDate")
    void testGetExpenseByDateInvalidRange() {
        when(userAuthService.getUsernameFromToken(anyString())).thenReturn("date_user");

        Response response = expenseService.getExpenseByDate("2024-02-01", "2024-01-01", "token-123");

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }
}
