package com.spendyteam.expense.Service;

import com.spendyteam.expense.Data.Expense;
import com.spendyteam.expense.Repository.IExpenseRepository;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ExpenseImportServiceTest {

    @Mock
    private IExpenseRepository expenseRepository;

    @Mock
    private SmartCategorizerService smartCategorizerService;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private ExpenseImportService expenseImportService;

    private Expense mockExpense;

    @BeforeEach
    void setUp() {
        // Istanziamo manualmente il servizio con il WebClient mockato
        expenseImportService = new ExpenseImportService(webClient);

        // Inietta manualmente repository e smartCategorizer usando reflection
        try {
            java.lang.reflect.Field repoField = ExpenseImportService.class.getDeclaredField("expenseRepository");
            repoField.setAccessible(true);
            repoField.set(expenseImportService, expenseRepository);

            java.lang.reflect.Field smartField = ExpenseImportService.class.getDeclaredField("smartCategorizerService");
            smartField.setAccessible(true);
            smartField.set(expenseImportService, smartCategorizerService);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        mockExpense = new Expense();
        mockExpense.setType("Payment");
        mockExpense.setProduct("Netflix");
        mockExpense.setStartedDate(LocalDateTime.of(2024, 1, 15, 10, 0));
        mockExpense.setCompletedDate(LocalDateTime.of(2024, 1, 15, 10, 5));
        mockExpense.setDescription("Netflix subscription");
        mockExpense.setAmount(new BigDecimal("15.99"));
        mockExpense.setFee(new BigDecimal("0.50"));
        mockExpense.setCurrency("EUR");
        mockExpense.setState("COMPLETED");
        mockExpense.setCategory("Abbonamenti e Servizi Digitali");
        mockExpense.setUsername("testuser");

        // Setup WebClient mocks - lenient perché non tutti i test li usano
        lenient().when(webClient.post()).thenReturn(requestBodyUriSpec);
        lenient().when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        lenient().when(requestBodySpec.bodyValue(any())).thenAnswer(invocation -> requestBodySpec);
        lenient().when(requestBodySpec.retrieve()).thenReturn(responseSpec);
    }

    @Test
    void testImportExpensesFromCsvEmptyFile() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
            "file",
            "test.csv",
            "text/csv",
            new byte[0]
        );

        Response response = expenseImportService.importExpensesFromCsv(emptyFile, "test-token");

        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    @Test
    void testImportExpensesFromCsvUnauthorized() throws Exception {
        String csvContent = "type,product,startedDate,completedDate,description,amount,fee,currency,state\n" +
                           "Payment,Netflix,2024-01-15 10:00:00,2024-01-15 10:05:00,Netflix subscription,15.99,0.50,EUR,COMPLETED";

        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.csv",
            "text/csv",
            csvContent.getBytes()
        );

        // Mock unauthorized response - ritorna mappa senza username
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(Collections.emptyMap()));

        Response response = expenseImportService.importExpensesFromCsv(file, "invalid-token");

        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
    }

    @Test
    void testGetExpensesSuccess() {
        String token = "valid-token";
        Map<String, String> authResponse = new HashMap<>();
        authResponse.put("username", "testuser");

        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(authResponse));
        when(expenseRepository.findByUsername(eq("testuser"), any(Sort.class)))
            .thenReturn(List.of(mockExpense));

        Response response = expenseImportService.getExpenses(token);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertNotNull(response.getEntity());
    }

    @Test
    void testGetExpensesUnauthorized() {
        String token = "invalid-token";

        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(Collections.emptyMap()));

        Response response = expenseImportService.getExpenses(token);

        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
    }

    @Test
    void testGetExpensesNoContent() {
        String token = "valid-token";
        Map<String, String> authResponse = new HashMap<>();
        authResponse.put("username", "testuser");

        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(authResponse));
        when(expenseRepository.findByUsername(eq("testuser"), any(Sort.class)))
            .thenReturn(Collections.emptyList());

        Response response = expenseImportService.getExpenses(token);

        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    @Test
    void testGetExpenseByDateSuccess() {
        String token = "valid-token";
        String startDate = "2024-01-01";
        String endDate = "2024-01-31";

        Map<String, String> authResponse = new HashMap<>();
        authResponse.put("username", "testuser");

        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(authResponse));
        when(expenseRepository.findByUsername("testuser"))
            .thenReturn(List.of(mockExpense));

        Response response = expenseImportService.getExpenseByDate(startDate, endDate, token);

        assertNotNull(response);
    }

    @Test
    void testGetExpenseByDateUnauthorized() {
        String token = "invalid-token";
        String startDate = "2024-01-01";
        String endDate = "2024-01-31";

        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(Collections.emptyMap()));

        Response response = expenseImportService.getExpenseByDate(startDate, endDate, token);

        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
    }

    @Test
    void testGetExpenseByMonthYear() {
        String token = "valid-token";
        String month = "01";
        String year = "2024";

        Map<String, String> authResponse = new HashMap<>();
        authResponse.put("username", "testuser");

        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(authResponse));
        when(expenseRepository.findByUsername("testuser"))
            .thenReturn(List.of(mockExpense));

        Response response = expenseImportService.getExpenseByMonth_Year(month, year, token);

        assertNotNull(response);
    }

    @Test
    void testGetMonthlyAmountOfYearSuccess() {
        String token = "valid-token";
        String year = "2024";

        Map<String, String> authResponse = new HashMap<>();
        authResponse.put("username", "testuser");

        Expense expense1 = new Expense();
        expense1.setStartedDate(LocalDateTime.of(2024, 1, 15, 10, 0));
        expense1.setAmount(new BigDecimal("-50.00"));

        Expense expense2 = new Expense();
        expense2.setStartedDate(LocalDateTime.of(2024, 2, 10, 14, 0));
        expense2.setAmount(new BigDecimal("-75.00"));

        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(authResponse));
        when(expenseRepository.findByUsername("testuser"))
            .thenReturn(Arrays.asList(expense1, expense2));

        Response response = expenseImportService.getMonthly_Amount_of_Year(year, token);

        assertNotNull(response);
    }

    @Test
    void testDeleteExpenseSuccess() {
        String token = "valid-token";
        String expenseId = "expense123";

        Map<String, String> authResponse = new HashMap<>();
        authResponse.put("username", "testuser");

        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(authResponse));
        when(expenseRepository.findById(expenseId)).thenReturn(Optional.of(mockExpense));

        Response response = expenseImportService.deleteExpense(expenseId, token);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        verify(expenseRepository).deleteById(expenseId);
    }

    @Test
    void testDeleteExpenseNotFound() {
        String token = "valid-token";
        String expenseId = "nonexistent";

        Map<String, String> authResponse = new HashMap<>();
        authResponse.put("username", "testuser");

        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(authResponse));
        when(expenseRepository.findById(expenseId)).thenReturn(Optional.empty());

        Response response = expenseImportService.deleteExpense(expenseId, token);

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    void testDeleteExpenseForbidden() {
        String token = "valid-token";
        String expenseId = "expense123";

        Map<String, String> authResponse = new HashMap<>();
        authResponse.put("username", "otheruser");

        Expense otherUserExpense = new Expense();
        otherUserExpense.setUsername("testuser");

        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(authResponse));
        when(expenseRepository.findById(expenseId)).thenReturn(Optional.of(otherUserExpense));

        Response response = expenseImportService.deleteExpense(expenseId, token);

        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
    }

    @Test
    void testAddExpenseSuccess() {
        String token = "valid-token";
        Map<String, String> body = new HashMap<>();
        body.put("type", "Payment");
        body.put("product", "Netflix");
        body.put("startedDate", "2024-01-15 10:00:00");
        body.put("completedDate", "2024-01-15 10:05:00");
        body.put("description", "Netflix subscription");
        body.put("amount", "15.99");
        body.put("fee", "0.50");
        body.put("currency", "EUR");
        body.put("state", "COMPLETED");

        Map<String, String> authResponse = new HashMap<>();
        authResponse.put("username", "testuser");

        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(authResponse));
        when(smartCategorizerService.predictCategory(anyString()))
            .thenReturn("Abbonamenti e Servizi Digitali");
        when(expenseRepository.save(any(Expense.class))).thenReturn(mockExpense);

        Response response = expenseImportService.addExpense(body, token);

        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        verify(expenseRepository).save(any(Expense.class));
    }

    @Test
    void testAddExpenseUnauthorized() {
        String token = "invalid-token";
        Map<String, String> body = new HashMap<>();

        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(Collections.emptyMap()));

        Response response = expenseImportService.addExpense(body, token);

        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
    }

    @Test
    void testUpdateExpenseSuccess() {
        String token = "valid-token";
        Map<String, String> body = new HashMap<>();
        body.put("id", "expense123");
        body.put("amount", "19.99");
        body.put("description", "Updated Netflix subscription");

        Map<String, String> authResponse = new HashMap<>();
        authResponse.put("username", "testuser");

        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(authResponse));
        when(expenseRepository.findById("expense123")).thenReturn(Optional.of(mockExpense));
        when(smartCategorizerService.predictCategory(anyString()))
            .thenReturn("Abbonamenti e Servizi Digitali");
        when(expenseRepository.save(any(Expense.class))).thenReturn(mockExpense);

        Response response = expenseImportService.updateExpense(body, token);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        verify(expenseRepository).save(any(Expense.class));
    }

    @Test
    void testUpdateExpenseNotFound() {
        String token = "valid-token";
        Map<String, String> body = new HashMap<>();
        body.put("id", "nonexistent");

        when(expenseRepository.findById("nonexistent")).thenReturn(Optional.empty());

        Response response = expenseImportService.updateExpense(body, token);

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    void testUpdateExpenseMissingId() {
        Map<String, String> body = new HashMap<>();

        Response response = expenseImportService.updateExpense(body, "valid-token");

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    void testUpdateExpenseForbidden() {
        String token = "valid-token";
        Map<String, String> body = new HashMap<>();
        body.put("id", "expense123");

        Map<String, String> authResponse = new HashMap<>();
        authResponse.put("username", "otheruser");

        Expense otherUserExpense = new Expense();
        otherUserExpense.setUsername("testuser");

        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(authResponse));
        when(expenseRepository.findById("expense123")).thenReturn(Optional.of(otherUserExpense));

        Response response = expenseImportService.updateExpense(body, token);

        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
    }

    @Test
    void testDeleteAllExpensesSuccess() {
        String token = "valid-token";

        Map<String, String> authResponse = new HashMap<>();
        authResponse.put("username", "testuser");

        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(authResponse));
        when(expenseRepository.deleteByUsername("testuser")).thenReturn(5L);

        Response response = expenseImportService.deleteAllExpenses(token);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        verify(expenseRepository).deleteByUsername("testuser");
    }

    @Test
    void testDeleteAllExpensesNoExpenses() {
        String token = "valid-token";

        Map<String, String> authResponse = new HashMap<>();
        authResponse.put("username", "testuser");

        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(authResponse));
        when(expenseRepository.deleteByUsername("testuser")).thenReturn(0L);

        Response response = expenseImportService.deleteAllExpenses(token);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    void testDeleteAllExpensesUnauthorized() {
        String token = "invalid-token";

        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(Collections.emptyMap()));

        Response response = expenseImportService.deleteAllExpenses(token);

        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
    }
}

