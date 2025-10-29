package com.spendyteam.expense.Controller;

import com.spendyteam.expense.Data.Debt;
import com.spendyteam.expense.Data.Expense;
import com.spendyteam.expense.Service.ExpenseService;
import com.spendyteam.expense.Utility.ExpenseDebtResult;
import com.spendyteam.expense.Utility.ExpenseResult;
import com.spendyteam.expense.Utility.ExpenseStatus;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) // Estensione per abilitare Mockito nei test JUnit
class ExpenseControllerTest {

    @Mock // Crea un mock del servizio ExpenseService
    private ExpenseService expenseService;

    @InjectMocks // Inietta i mock nel controller da testare
    private ExpenseController expenseController;

    // Variabili di test
    private Expense testExpense;
    private List<Expense> testExpenseList;
    private Debt testDebt;
    private List<Debt> testDebtList;
    private ExpenseResult successExpenseResult;
    private ExpenseResult errorExpenseResult;
    private ExpenseResult invalidInputExpenseResult;
    private ExpenseDebtResult successDebtResult;
    private ExpenseDebtResult noContentDebtResult;

    /**
     * Metodo eseguito prima di ogni test per inizializzare i dati di test
     */
    @BeforeEach
    void setUp() {
        // Configurazione dei dati di test per le spese
        testExpense = new Expense();
        testExpense.setId("exp1");
        testExpense.setDescription("Test Expense");
        testExpense.setAmount(100.0);
        testExpense.setCreatedBy("user1");
        testExpense.setHouseId("house1");
        testExpense.setCategory(CategoryExpense.FOOD);
        testExpense.setParticipants(Arrays.asList("user1", "user2"));
        testExpense.setStatus(StatusExpense.PENDING);
        testExpense.setCreatedDate(new Date());

        // Lista di spese per i test
        testExpenseList = Collections.singletonList(testExpense);

        // Configurazione dei dati di test per i debiti
        Map<String, Double> participants = new HashMap<>();
        participants.put("user2", 50.0);
        testDebt = new Debt("user1", participants);
        testDebtList = List.of(testDebt);

        // Risultati attesi per i test
        successExpenseResult = new ExpenseResult(ExpenseStatus.SUCCESS, "Expense created successfully", testExpense);
        errorExpenseResult = new ExpenseResult(ExpenseStatus.ERROR, "Internal server error", null);
        invalidInputExpenseResult = new ExpenseResult(ExpenseStatus.INVALID_INPUT, "Invalid input data", null);
        successDebtResult = new ExpenseDebtResult(ExpenseStatus.SUCCESS, "Debt calculated successfully", testDebtList);
        noContentDebtResult = new ExpenseDebtResult(ExpenseStatus.NO_CONTENT, "No expenses found", null);
    }

    /**
     * Test per verificare la creazione di una spesa con successo
     */
    @Test
    void testCreateExpense_Success() {
        // Dato che il servizio restituisce un risultato di successo
        when(expenseService.createExpense(
                anyString(),
                any(Date.class),
                anyDouble(),
                anyString(),
                anyString(),
                any(CategoryExpense.class),
                anyList()
        )).thenReturn(successExpenseResult);

        // Quando viene chiamato il controller
        Response response = expenseController.createExpense(testExpense);

        // Allora verifica che la risposta sia OK e contenga il risultato atteso
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(successExpenseResult, response.getEntity());

        // Verifica che il servizio sia stato chiamato con i parametri corretti
        verify(expenseService, times(1)).createExpense(
                eq("Test Expense"),
                any(Date.class),
                eq(100.0),
                eq("house1"),
                eq("user1"),
                eq(CategoryExpense.FOOD),
                eq(Arrays.asList("user1", "user2"))
        );
    }

    /**
     * Test per verificare la creazione di una spesa con errore del server
     */
    @Test
    void testCreateExpense_Error() {
        // Dato che il servizio restituisce un errore
        when(expenseService.createExpense(
                anyString(),
                any(Date.class),
                anyDouble(),
                anyString(),
                anyString(),
                any(CategoryExpense.class),
                anyList()
        )).thenReturn(errorExpenseResult);

        // Quando viene chiamato il controller
        Response response = expenseController.createExpense(testExpense);

        // Allora verifica che la risposta sia INTERNAL_SERVER_ERROR
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        assertEquals(errorExpenseResult, response.getEntity());
    }

    /**
     * Test per verificare la creazione di una spesa con input non valido
     */
    @Test
    void testCreateExpense_InvalidInput() {
        // Dato che il servizio restituisce un errore di input non valido
        when(expenseService.createExpense(
                anyString(),
                any(Date.class),
                anyDouble(),
                anyString(),
                anyString(),
                any(CategoryExpense.class),
                anyList()
        )).thenReturn(invalidInputExpenseResult);

        // Quando viene chiamato il controller
        Response response = expenseController.createExpense(testExpense);

        // Allora verifica che la risposta sia BAD_REQUEST
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals(invalidInputExpenseResult, response.getEntity());
    }

    /**
     * Test parametrico per verificare la creazione di una spesa con ogni categoria possibile
     * @param category La categoria da testare (fornita dall'EnumSource)
     */
    @ParameterizedTest
    @EnumSource(CategoryExpense.class)
    void testCreateExpense_WithEachCategory(CategoryExpense category) {
        // Dato una spesa con una categoria specifica
        testExpense.setCategory(category);

        // E dato che il servizio restituisce un risultato di successo
        when(expenseService.createExpense(
                anyString(), any(Date.class), anyDouble(), anyString(),
                anyString(), eq(category), anyList()
        )).thenReturn(successExpenseResult);

        // Quando viene chiamato il controller
        Response response = expenseController.createExpense(testExpense);

        // Allora verifica che la risposta sia OK
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(successExpenseResult, response.getEntity());

        // E verifica che il servizio sia stato chiamato con la categoria corretta
        verify(expenseService, times(1)).createExpense(
                eq(testExpense.getDescription()),
                any(Date.class),
                eq(testExpense.getAmount()),
                eq(testExpense.getHouseId()),
                eq(testExpense.getCreatedBy()),
                eq(category),
                eq(testExpense.getParticipants())
        );
    }

    /**
     * Test per verificare il recupero di tutte le spese con successo
     */
    @Test
    void testGetAllExpenses_Success() {
        // Dato un houseId e un risultato atteso di successo
        String houseId = "house1";
        ExpenseResult result = new ExpenseResult(ExpenseStatus.SUCCESS, "Success", testExpenseList);

        when(expenseService.getAllExpenses(houseId)).thenReturn(result);

        // Quando viene chiamato il controller
        Response response = expenseController.getAllExpenses(houseId);

        // Allora verifica che la risposta sia OK e contenga il risultato atteso
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(result, response.getEntity());
        verify(expenseService, times(1)).getAllExpenses(houseId);
    }

    /**
     * Test per verificare il recupero di tutte le spese quando non ci sono contenuti
     */
    @Test
    void testGetAllExpenses_NoContent() {
        // Dato un houseId e un risultato atteso senza contenuti
        String houseId = "house1";
        ExpenseResult result = new ExpenseResult(ExpenseStatus.NO_CONTENT, "No content", null);

        when(expenseService.getAllExpenses(houseId)).thenReturn(result);

        // Quando viene chiamato il controller
        Response response = expenseController.getAllExpenses(houseId);

        // Allora verifica che la risposta sia NO_CONTENT
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        assertEquals(result, response.getEntity());
    }

    /**
     * Test per verificare il recupero di tutte le spese con errore del server
     */
    @Test
    void testGetAllExpenses_ServerError() {
        // Dato un houseId e un risultato atteso di errore
        String houseId = "house1";
        ExpenseResult result = new ExpenseResult(ExpenseStatus.ERROR, "Server error", null);

        when(expenseService.getAllExpenses(houseId)).thenReturn(result);

        // Quando viene chiamato il controller
        Response response = expenseController.getAllExpenses(houseId);

        // Allora verifica che la risposta sia INTERNAL_SERVER_ERROR
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        assertEquals("Server error", response.getEntity());
    }

    /**
     * Test per verificare il calcolo dei debiti con successo
     */
    @Test
    void testCalculateDebt_Success() {
        // Dato un request body con houseId e un risultato atteso di successo
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("houseId", "house1");

        when(expenseService.calculateDebt("house1")).thenReturn(successDebtResult);

        // Quando viene chiamato il controller
        Response response = expenseController.calculateDebt(requestBody);

        // Allora verifica che la risposta sia OK
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(successDebtResult, response.getEntity());
        verify(expenseService, times(1)).calculateDebt("house1");
    }

    /**
     * Test per verificare il calcolo dei debiti quando non ci sono contenuti
     */
    @Test
    void testCalculateDebt_NoContent() {
        // Dato un request body con houseId e un risultato atteso senza contenuti
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("houseId", "house1");

        when(expenseService.calculateDebt("house1")).thenReturn(noContentDebtResult);

        // Quando viene chiamato il controller
        Response response = expenseController.calculateDebt(requestBody);

        // Allora verifica che la risposta sia NOT_FOUND
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        assertEquals(noContentDebtResult, response.getEntity());
    }

    /**
     * Test per verificare il calcolo dei debiti con errore del server
     */
    @Test
    void testCalculateDebt_ServerError() {
        // Dato un request body con houseId e un risultato atteso di errore
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("houseId", "house1");

        ExpenseDebtResult errorResult = new ExpenseDebtResult(ExpenseStatus.ERROR, "Server error", null);

        when(expenseService.calculateDebt("house1")).thenReturn(errorResult);

        // Quando viene chiamato il controller
        Response response = expenseController.calculateDebt(requestBody);

        // Allora verifica che la risposta sia INTERNAL_SERVER_ERROR
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        assertEquals("Server error", response.getEntity());
    }

    /**
     * Test per verificare l'aggiornamento dello stato di una spesa con successo
     */
    @Test
    void testUpdateExpenseStatus_Success() {
        // Dato un request body con expenseId e un risultato atteso di successo
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("expenseId", "expense1");

        when(expenseService.updateExpenseStatus("expense1")).thenReturn(successExpenseResult);

        // Quando viene chiamato il controller
        Response response = expenseController.updateExpenseStatus(requestBody);

        // Allora verifica che la risposta sia OK
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(successExpenseResult, response.getEntity());
        verify(expenseService, times(1)).updateExpenseStatus("expense1");
    }

    /**
     * Test per verificare l'aggiornamento dello stato di una spesa con bad request
     */
    @Test
    void testUpdateExpenseStatus_BadRequest() {
        // Dato un request body con expenseId e un risultato atteso di bad request
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("expenseId", "expense1");

        ExpenseResult badRequestResult = new ExpenseResult(ExpenseStatus.ERROR, "Bad request", null);
        when(expenseService.updateExpenseStatus("expense1")).thenReturn(badRequestResult);

        // Quando viene chiamato il controller
        Response response = expenseController.updateExpenseStatus(requestBody);

        // Allora verifica che la risposta sia BAD_REQUEST
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals(badRequestResult, response.getEntity());
    }

    /**
     * Test per verificare l'aggiornamento dello stato di una spesa con eccezione
     */
    @Test
    void testUpdateExpenseStatus_Exception() {
        // Dato un request body con expenseId e un'eccezione attesa
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("expenseId", "expense1");

        when(expenseService.updateExpenseStatus("expense1"))
                .thenThrow(new RuntimeException("Database connection failed"));

        // Quando viene chiamato il controller
        Response response = expenseController.updateExpenseStatus(requestBody);

        // Allora verifica che la risposta sia INTERNAL_SERVER_ERROR
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        assertTrue(response.getEntity().toString().contains("Error updating expense status"));
        assertTrue(response.getEntity().toString().contains("Database connection failed"));
    }

    /**
     * Test per verificare il calcolo dei debiti con houseId nullo
     */
    @Test
    void testCalculateDebt_NullHouseId() {
        // Dato un request body senza houseId
        Map<String, String> requestBody = new HashMap<>();
        // houseId è null

        when(expenseService.calculateDebt(null)).thenReturn(noContentDebtResult);

        // Quando viene chiamato il controller
        Response response = expenseController.calculateDebt(requestBody);

        // Allora verifica che la risposta sia NOT_FOUND
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    /**
     * Test per verificare l'aggiornamento dello stato di una spesa con expenseId nullo
     */
    @Test
    void testUpdateExpenseStatus_NullExpenseId() {
        // Dato un request body senza expenseId
        Map<String, String> requestBody = new HashMap<>();
        // expenseId è null

        ExpenseResult nullResult = new ExpenseResult(ExpenseStatus.ERROR, "Null expense ID", null);
        when(expenseService.updateExpenseStatus(null)).thenReturn(nullResult);

        // Quando viene chiamato il controller
        Response response = expenseController.updateExpenseStatus(requestBody);

        // Allora verifica che la risposta sia BAD_REQUEST
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        verify(expenseService, times(1)).updateExpenseStatus(null);
    }

    /**
     * Test per verificare il calcolo dei debiti con dati completi
     */
    @Test
    void testCalculateDebt_WithDebtData() {
        // Dato un request body con houseId
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("houseId", "house1");

        // E dati di debito più complessi
        Map<String, Double> participants1 = new HashMap<>();
        participants1.put("user2", 25.0);
        participants1.put("user3", 25.0);
        Debt debt1 = new Debt("user1", participants1);

        Map<String, Double> participants2 = new HashMap<>();
        participants2.put("user1", 30.0);
        Debt debt2 = new Debt("user2", participants2);

        List<Debt> debts = Arrays.asList(debt1, debt2);
        ExpenseDebtResult debtResult = new ExpenseDebtResult(ExpenseStatus.SUCCESS, "Debts calculated", debts);

        when(expenseService.calculateDebt("house1")).thenReturn(debtResult);

        // Quando viene chiamato il controller
        Response response = expenseController.calculateDebt(requestBody);

        // Allora verifica che la risposta sia OK
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(debtResult, response.getEntity());
        verify(expenseService, times(1)).calculateDebt("house1");
    }
}