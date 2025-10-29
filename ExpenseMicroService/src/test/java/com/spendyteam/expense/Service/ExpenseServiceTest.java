package com.spendyteam.expense.Service;

import com.spendyteam.expense.Data.*;
import com.spendyteam.expense.Repository.IExpenseRepository;
import com.spendyteam.expense.Utility.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test suite per il servizio ExpenseService.
 * Verifica il comportamento dei metodi principali per la gestione delle spese.
 */
@ExtendWith(MockitoExtension.class) // Abilita l'uso di Mockito con JUnit 5
class ExpenseServiceTest {

    @Mock // Crea un mock dell'interfaccia IExpenseRepository
    private IExpenseRepository expenseRepository;

    @InjectMocks // Inietta i mock nel servizio da testare
    private ExpenseService expenseService;

    // Variabili di test
    private String testHouseId;
    private String testUserId1;
    private String testUserId2;
    private String testUserId3;
    private List<String> testParticipants;
    private Expense testExpense1;
    private Expense testExpense2;
    private Expense testExpense3;

    /**
     * Metodo eseguito prima di ogni test per inizializzare i dati di test.
     */
    @BeforeEach
    void setUp() {
        // Inizializzazione ID di test
        testHouseId = "house123";
        testUserId1 = "user1";
        testUserId2 = "user2";
        testUserId3 = "user3";
        testParticipants = Arrays.asList(testUserId1, testUserId2, testUserId3);

        // Spesa PENDING con 3 partecipanti
        testExpense1 = new Expense(
                "Spesa supermercato",
                150.0,
                testUserId1,
                new Date(),
                CategoryExpense.FOOD,
                testHouseId,
                testParticipants
        );
        testExpense1.setId("expense1");
        testExpense1.setStatus(StatusExpense.PENDING);

        // Spesa SETTLED con 2 partecipanti
        testExpense2 = new Expense(
                "Cinema",
                40.0,
                testUserId2,
                new Date(),
                CategoryExpense.ENTERTAINMENT,
                testHouseId,
                Arrays.asList(testUserId1, testUserId2)
        );
        testExpense2.setId("expense2");
        testExpense2.setStatus(StatusExpense.SETTLED);

        // Spesa PENDING con 2 partecipanti
        testExpense3 = new Expense(
                "Bolletta gas",
                80.0,
                testUserId1,
                new Date(),
                CategoryExpense.BILLS,
                testHouseId,
                Arrays.asList(testUserId1, testUserId3)
        );
        testExpense3.setId("expense3");
        testExpense3.setStatus(StatusExpense.PENDING);
    }

    /**
     * Test per verificare la creazione corretta di una spesa.
     */
    @Test
    void testCreateExpense_Success() {
        // Dati di input validi
        String description = "Test expense";
        Date date = new Date();
        Double amount = 100.0;
        String createdBy = testUserId1;
        CategoryExpense category = CategoryExpense.FOOD;
        List<String> participants = Arrays.asList(testUserId1, testUserId2);

        when(expenseRepository.insert(any(Expense.class))).thenReturn(testExpense1);

        // Quando creo la spesa
        ExpenseResult result = expenseService.createExpense(description, date, amount, testHouseId, createdBy, category, participants);

        // Allora verifico il successo dell'operazione
        assertEquals(ExpenseStatus.SUCCESS, result.getStatus());
        assertEquals("Expense created successfully", result.getMessage());
        assertNotNull(result.getExpenses());
        verify(expenseRepository, times(1)).insert(any(Expense.class));
    }

    /**
     * Test per verificare il fallimento della creazione con partecipanti vuoti.
     */
    @Test
    void testCreateExpense_EmptyParticipants() {
        // Dati di input con lista partecipanti vuota
        List<String> emptyParticipants = List.of();

        // Quando provo a creare la spesa
        ExpenseResult result = expenseService.createExpense("Test", new Date(), 100.0,
                testHouseId, testUserId1, CategoryExpense.FOOD, emptyParticipants);

        // Allora verifico che fallisce
        assertEquals(ExpenseStatus.INVALID_INPUT, result.getStatus());
        assertEquals("At least two participants are required for an expense", result.getMessage());
        verify(expenseRepository, never()).insert(any(Expense.class));
    }

    /**
     * Test per verificare la gestione degli errori del repository.
     */
    @Test
    void testCreateExpense_RepositoryException() {
        // Configuro il mock per sollevare un'eccezione
        when(expenseRepository.insert(any(Expense.class))).thenThrow(new RuntimeException("Database error"));

        // Quando provo a creare la spesa
        ExpenseResult result = expenseService.createExpense("Test", new Date(), 100.0,
                testHouseId, testUserId1, CategoryExpense.FOOD, Arrays.asList(testUserId1, testUserId2));

        // Allora verifico che viene gestito l'errore
        assertEquals(ExpenseStatus.ERROR, result.getStatus());
        verify(expenseRepository, times(1)).insert(any(Expense.class));
    }

    /**
     * Test per verificare il recupero di tutte le spese.
     */
    @Test
    void testGetAllExpenses_Success() {
        // Configuro il mock con una lista di spese
        List<Expense> expenses = Arrays.asList(testExpense1, testExpense2, testExpense3);
        when(expenseRepository.findByHouseId(testHouseId)).thenReturn(expenses);

        // Quando recupero tutte le spese
        ExpenseResult result = expenseService.getAllExpenses(testHouseId);

        // Allora verifico che vengono restituite correttamente
        assertEquals(ExpenseStatus.SUCCESS, result.getStatus());
        assertEquals(expenses, result.getExpenses());
    }

    /**
     * Test per verificare il calcolo dei debiti tra utenti.
     */
    @Test
    void testCalculateDebt_Success() {
        // Configuro il mock con spese PENDING
        List<Expense> expenses = Arrays.asList(testExpense1, testExpense3);
        when(expenseRepository.findByHouseId(testHouseId)).thenReturn(expenses);

        // Quando calcolo i debiti
        ExpenseDebtResult result = expenseService.calculateDebt(testHouseId);

        // Allora verifico il calcolo corretto
        assertEquals(ExpenseStatus.SUCCESS, result.getStatus());
        @SuppressWarnings("unchecked")
        List<Debt> debts = (List<Debt>) result.getDebts();

        // Verifica che il creatore non sia incluso tra i debitori
        debts.forEach(debt ->
                assertFalse(debt.getParticipants().containsKey(debt.getCreatedBy()))
        );
    }

    /**
     * Test per verificare che le spese già saldate non vengano incluse nel calcolo.
     */
    @Test
    void testCalculateDebt_ExcludeSettledExpenses() {
        // Configuro il mock con spese miste (PENDING e SETTLED)
        when(expenseRepository.findByHouseId(testHouseId)).thenReturn(Arrays.asList(testExpense1, testExpense2, testExpense3));

        // Quando calcolo i debiti
        ExpenseDebtResult result = expenseService.calculateDebt(testHouseId);

        // Allora verifico che solo le PENDING sono incluse
        @SuppressWarnings("unchecked")
        List<Debt> debts = (List<Debt>) result.getDebts();
        assertEquals(2, debts.size()); // Solo testExpense1 e testExpense3
    }

    /**
     * Test per verificare l'aggiornamento dello stato di una spesa.
     */
    @Test
    void testUpdateExpenseStatus_Success() {
        // Configuro il mock per trovare la spesa
        when(expenseRepository.findById("expense1")).thenReturn(Optional.of(testExpense1));
        when(expenseRepository.save(any(Expense.class))).thenReturn(testExpense1);

        // Quando aggiorno lo stato
        ExpenseResult result = expenseService.updateExpenseStatus("expense1");

        // Allora verifico che è stato aggiornato a SETTLED
        assertEquals(StatusExpense.SETTLED, testExpense1.getStatus());
        assertEquals(ExpenseStatus.SUCCESS, result.getStatus());
    }

    /**
     * Test per verificare il caso limite di spesa con importo zero.
     */
    @Test
    void testCalculateDebt_ZeroAmount() {
        // Creo una spesa con importo zero
        Expense zeroExpense = new Expense(
                "Zero", 0.0, testUserId1, new Date(),
                CategoryExpense.OTHER, testHouseId,
                Arrays.asList(testUserId1, testUserId2));
        zeroExpense.setStatus(StatusExpense.PENDING);

        when(expenseRepository.findByHouseId(testHouseId)).thenReturn(List.of(zeroExpense));

        // Quando calcolo i debiti
        ExpenseDebtResult result = expenseService.calculateDebt(testHouseId);

        // Allora verifico che il debito è zero
        @SuppressWarnings("unchecked")
        List<Debt> debts = (List<Debt>) result.getDebts();
        assertEquals(0.0, debts.get(0).getParticipants().get(testUserId2));
    }
}