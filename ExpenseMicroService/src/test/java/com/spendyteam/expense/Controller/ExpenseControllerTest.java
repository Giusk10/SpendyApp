package com.spendyteam.expense.Controller;

import com.spendyteam.expense.Service.ExpenseImportService;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpenseControllerTest {

    @Mock
    private ExpenseImportService expenseService;

    @InjectMocks
    private ExpenseController expenseController;

    private String authHeader;

    @BeforeEach
    void setUp() {
        authHeader = "Bearer test-token-12345";
    }

    @Test
    void testImportCsvSuccess() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.csv",
            "text/csv",
            "type,product,description\nPayment,Netflix,Subscription".getBytes()
        );

        Response mockResponse = Response.ok("Expenses imported successfully").build();
        when(expenseService.importExpensesFromCsv(any(), eq("test-token-12345")))
            .thenReturn(mockResponse);

        ResponseEntity<String> response = expenseController.importCsv(file, authHeader);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Expenses imported successfully", response.getBody());
    }

    @Test
    void testImportCsvBadRequest() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.csv",
            "text/csv",
            "invalid csv".getBytes()
        );

        Response mockResponse = Response.status(Response.Status.BAD_REQUEST)
            .entity("Invalid CSV format").build();
        when(expenseService.importExpensesFromCsv(any(), anyString()))
            .thenReturn(mockResponse);

        ResponseEntity<String> response = expenseController.importCsv(file, authHeader);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void testImportCsvException() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.csv",
            "text/csv",
            "data".getBytes()
        );

        when(expenseService.importExpensesFromCsv(any(), anyString()))
            .thenThrow(new RuntimeException("Test exception"));

        ResponseEntity<String> response = expenseController.importCsv(file, authHeader);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().contains("Failed to import CSV"));
    }

    @Test
    void testGetExpensesSuccess() {
        Response mockResponse = Response.ok("Expenses data").build();
        when(expenseService.getExpenses("test-token-12345")).thenReturn(mockResponse);

        ResponseEntity<?> response = expenseController.getExpenses(authHeader);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Expenses data", response.getBody());
    }

    @Test
    void testGetExpensesUnauthorized() {
        Response mockResponse = Response.status(Response.Status.UNAUTHORIZED)
            .entity("Unauthorized").build();
        when(expenseService.getExpenses(anyString())).thenReturn(mockResponse);

        ResponseEntity<?> response = expenseController.getExpenses(authHeader);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void testGetExpensesException() {
        when(expenseService.getExpenses(anyString()))
            .thenThrow(new RuntimeException("Test exception"));

        ResponseEntity<?> response = expenseController.getExpenses(authHeader);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void testGetExpenseByDateSuccess() {
        Map<String, String> body = new HashMap<>();
        body.put("startedDate", "2024-01-01");
        body.put("completedDate", "2024-01-31");

        Response mockResponse = Response.ok("Date expenses").build();
        when(expenseService.getExpenseByDate("2024-01-01", "2024-01-31", "test-token-12345"))
            .thenReturn(mockResponse);

        ResponseEntity<?> response = expenseController.getExpenseByDate(body, authHeader);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Date expenses", response.getBody());
    }

    @Test
    void testGetExpenseByDateNoContent() {
        Map<String, String> body = new HashMap<>();
        body.put("startedDate", "2024-01-01");
        body.put("completedDate", "2024-01-31");

        Response mockResponse = Response.status(Response.Status.NO_CONTENT)
            .entity("No expenses found").build();
        when(expenseService.getExpenseByDate(anyString(), anyString(), anyString()))
            .thenReturn(mockResponse);

        ResponseEntity<?> response = expenseController.getExpenseByDate(body, authHeader);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void testGetExpenseByMonthSuccess() {
        Map<String, String> body = new HashMap<>();
        body.put("month", "01");
        body.put("year", "2024");

        Response mockResponse = Response.ok("Month expenses").build();
        when(expenseService.getExpenseByMonth_Year("01", "2024", "test-token-12345"))
            .thenReturn(mockResponse);

        ResponseEntity<?> response = expenseController.getExpenseByMonth_Year(body, authHeader);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void testGetMonthlyAmountOfYearSuccess() {
        Map<String, String> body = new HashMap<>();
        body.put("year", "2024");

        Response mockResponse = Response.ok(Map.of("2024-01", "-100.00")).build();
        when(expenseService.getMonthly_Amount_of_Year("2024", "test-token-12345"))
            .thenReturn(mockResponse);

        ResponseEntity<?> response = expenseController.getMonthly_Amount_of_Year(body, authHeader);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void testDeleteExpenseSuccess() {
        Map<String, String> body = new HashMap<>();
        body.put("expenseId", "expense123");

        Response mockResponse = Response.ok("Expense deleted successfully").build();
        when(expenseService.deleteExpense("expense123", "test-token-12345"))
            .thenReturn(mockResponse);

        ResponseEntity<Object> response = expenseController.deleteExpense(body, authHeader);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void testDeleteExpenseNotFound() {
        Map<String, String> body = new HashMap<>();
        body.put("expenseId", "nonexistent");

        Response mockResponse = Response.status(Response.Status.NOT_FOUND)
            .entity("Expense not found").build();
        when(expenseService.deleteExpense(anyString(), anyString()))
            .thenReturn(mockResponse);

        ResponseEntity<Object> response = expenseController.deleteExpense(body, authHeader);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testDeleteAllExpensesSuccess() {
        Response mockResponse = Response.ok("All expenses deleted").build();
        when(expenseService.deleteAllExpenses("test-token-12345"))
            .thenReturn(mockResponse);

        ResponseEntity<Object> response = expenseController.deleteAllExpenses(authHeader);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void testDeleteAllExpensesException() {
        when(expenseService.deleteAllExpenses(anyString()))
            .thenThrow(new RuntimeException("Test exception"));

        ResponseEntity<Object> response = expenseController.deleteAllExpenses(authHeader);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void testAddExpenseSuccess() {
        Map<String, String> body = new HashMap<>();
        body.put("type", "Payment");
        body.put("product", "Netflix");
        body.put("description", "Subscription");
        body.put("amount", "15.99");

        Response mockResponse = Response.status(Response.Status.CREATED)
            .entity(Map.of("id", "newExpense123")).build();
        when(expenseService.addExpense(any(), eq("test-token-12345")))
            .thenReturn(mockResponse);

        ResponseEntity<Object> response = expenseController.addExpense(body, authHeader);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    void testAddExpenseUnauthorized() {
        Map<String, String> body = new HashMap<>();

        Response mockResponse = Response.status(Response.Status.UNAUTHORIZED)
            .entity("Unauthorized").build();
        when(expenseService.addExpense(any(), anyString()))
            .thenReturn(mockResponse);

        ResponseEntity<Object> response = expenseController.addExpense(body, authHeader);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void testAddExpenseException() {
        Map<String, String> body = new HashMap<>();

        when(expenseService.addExpense(any(), anyString()))
            .thenThrow(new RuntimeException("Test exception"));

        ResponseEntity<Object> response = expenseController.addExpense(body, authHeader);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void testUpdateExpenseSuccess() {
        Map<String, String> body = new HashMap<>();
        body.put("id", "expense123");
        body.put("amount", "19.99");

        Response mockResponse = Response.ok(Map.of("id", "expense123")).build();
        when(expenseService.updateExpense(any(), eq("test-token-12345")))
            .thenReturn(mockResponse);

        ResponseEntity<Object> response = expenseController.updateExpense(body, authHeader);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void testUpdateExpenseNotFound() {
        Map<String, String> body = new HashMap<>();
        body.put("id", "nonexistent");

        Response mockResponse = Response.status(Response.Status.NOT_FOUND)
            .entity("Expense not found").build();
        when(expenseService.updateExpense(any(), anyString()))
            .thenReturn(mockResponse);

        ResponseEntity<Object> response = expenseController.updateExpense(body, authHeader);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testUpdateExpenseException() {
        Map<String, String> body = new HashMap<>();

        when(expenseService.updateExpense(any(), anyString()))
            .thenThrow(new RuntimeException("Test exception"));

        ResponseEntity<Object> response = expenseController.updateExpense(body, authHeader);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void testTestEndpoint() {
        ResponseEntity<String> response = expenseController.testEndpoint();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Expense Microservice is up and running!", response.getBody());
    }

    @Test
    void testImportCsvWithoutAuthHeader() {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.csv",
            "text/csv",
            "data".getBytes()
        );

        ResponseEntity<String> response = expenseController.importCsv(file, null);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("Failed to import CSV"));
    }

    @Test
    void testGetExpensesWithoutAuthHeader() {
        ResponseEntity<?> response = expenseController.getExpenses(null);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}

