package com.spendyteam.expense.Repository;

import com.spendyteam.expense.Data.Expense;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test suite per l'interfaccia IExpenseRepository.
 * Utilizza Mockito per simulare il comportamento del repository.
 */
@ExtendWith(MockitoExtension.class) // Abilita l'uso di Mockito nei test JUnit
@DisplayName("IExpenseRepository Test Suite") // Nome descrittivo per la suite di test
class IExpenseRepositoryTest {

    @Mock // Crea un mock dell'interfaccia IExpenseRepository
    private IExpenseRepository expenseRepository;

    // Variabili di test
    private Expense testExpense1;
    private Expense testExpense2;
    private Expense testExpense3;
    private String testHouseId;
    private String testUserId;
    private Date testStartDate;
    private Date testEndDate;

    /**
     * Metodo eseguito prima di ogni test per inizializzare i dati di test.
     */
    @BeforeEach
    void setUp() {
        testHouseId = "house123";
        testUserId = "user456";
        testStartDate = new Date(System.currentTimeMillis() - 86400000); // Ieri
        testEndDate = new Date(); // Oggi

        // Inizializzazione di spese di test
        testExpense1 = new Expense(
                "Spesa supermercato",
                50.0,
                testUserId,
                new Date(),
                CategoryExpense.FOOD,
                testHouseId,
                Arrays.asList("user456", "user789")
        );
        testExpense1.setId("expense1");
        testExpense1.setStatus(StatusExpense.PENDING);

        testExpense2 = new Expense(
                "Bolletta elettricità",
                120.0,
                "user789",
                new Date(),
                CategoryExpense.BILLS,
                testHouseId,
                Arrays.asList("user456", "user789", "user101")
        );
        testExpense2.setId("expense2");
        testExpense2.setStatus(StatusExpense.SETTLED);

        testExpense3 = new Expense(
                "Cinema",
                30.0,
                testUserId,
                new Date(),
                CategoryExpense.ENTERTAINMENT,
                "house999",
                Arrays.asList("user456", "user555")
        );
        testExpense3.setId("expense3");
        testExpense3.setStatus(StatusExpense.PENDING);
    }

    /**
     * Test per verificare il corretto recupero di una spesa tramite ID.
     */
    @Test
    @DisplayName("Should find expense by expense ID successfully")
    void testFindByExpenseId_Success() {
        // Dato un ID spesa
        String expenseId = "expense1";
        when(expenseRepository.findByExpenseId(expenseId)).thenReturn(testExpense1);

        // Quando cerco la spesa per ID
        Expense result = expenseRepository.findByExpenseId(expenseId);

        // Allora verifico che la spesa sia stata trovata correttamente
        assertNotNull(result);
        assertEquals(testExpense1.getId(), result.getId());
        assertEquals(testExpense1.getDescription(), result.getDescription());
        verify(expenseRepository, times(1)).findByExpenseId(expenseId);
    }

    /**
     * Test per verificare il comportamento quando una spesa non viene trovata.
     */
    @Test
    @DisplayName("Should return null when expense ID not found")
    void testFindByExpenseId_NotFound() {
        // Dato un ID inesistente
        String nonExistentId = "nonexistent";
        when(expenseRepository.findByExpenseId(nonExistentId)).thenReturn(null);

        // Quando cerco una spesa con ID inesistente
        Expense result = expenseRepository.findByExpenseId(nonExistentId);

        // Allora verifico che il risultato sia null
        assertNull(result);
        verify(expenseRepository, times(1)).findByExpenseId(nonExistentId);
    }

    /**
     * Test per verificare il recupero di tutte le spese di una casa.
     */
    @Test
    @DisplayName("Should find all expenses by house ID successfully")
    void testFindByHouseId_Success() {
        // Date le spese attese per una casa
        List<Expense> expectedExpenses = Arrays.asList(testExpense1, testExpense2);
        when(expenseRepository.findByHouseId(testHouseId)).thenReturn(expectedExpenses);

        // Quando cerco le spese per ID casa
        List<Expense> result = expenseRepository.findByHouseId(testHouseId);

        // Allora verifico che tutte le spese siano state trovate
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains(testExpense1));
        assertTrue(result.contains(testExpense2));
        verify(expenseRepository, times(1)).findByHouseId(testHouseId);
    }

    /**
     * Test per verificare il comportamento quando non ci sono spese per una casa.
     */
    @Test
    @DisplayName("Should return empty list when no expenses found for house ID")
    void testFindByHouseId_EmptyResult() {
        // Data una casa senza spese
        String emptyHouseId = "emptyHouse";
        when(expenseRepository.findByHouseId(emptyHouseId)).thenReturn(List.of());

        // Quando cerco le spese per questa casa
        List<Expense> result = expenseRepository.findByHouseId(emptyHouseId);

        // Allora verifico che la lista sia vuota
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(expenseRepository, times(1)).findByHouseId(emptyHouseId);
    }

    /**
     * Test per verificare il recupero delle spese create da un utente.
     */
    @Test
    @DisplayName("Should find all expenses created by specific user")
    void testFindByCreatedBy_Success() {
        // Date le spese attese create dall'utente
        List<Expense> expectedExpenses = Arrays.asList(testExpense1, testExpense3);
        when(expenseRepository.findByCreatedBy(testUserId)).thenReturn(expectedExpenses);

        // Quando cerco le spese create dall'utente
        List<Expense> result = expenseRepository.findByCreatedBy(testUserId);

        // Allora verifico che tutte le spese siano state trovate
        assertNotNull(result);
        assertEquals(2, result.size());
        result.forEach(expense -> assertEquals(testUserId, expense.getCreatedBy()));
        verify(expenseRepository, times(1)).findByCreatedBy(testUserId);
    }

    /**
     * Test per verificare il recupero delle spese dove un utente è partecipante.
     */
    @Test
    @DisplayName("Should find all expenses where user is participant")
    void testFindByParticipant_Success() {
        // Date le spese attese dove l'utente è partecipante
        List<Expense> expectedExpenses = Arrays.asList(testExpense1, testExpense2, testExpense3);
        when(expenseRepository.findByParticipant(testUserId)).thenReturn(expectedExpenses);

        // Quando cerco le spese per partecipante
        List<Expense> result = expenseRepository.findByParticipant(testUserId);

        // Allora verifico che tutte le spese siano state trovate
        assertNotNull(result);
        assertEquals(3, result.size());
        result.forEach(expense -> assertTrue(expense.getParticipants().contains(testUserId)));
        verify(expenseRepository, times(1)).findByParticipant(testUserId);
    }

    /**
     * Test per verificare il recupero delle spese per casa e stato.
     */
    @Test
    @DisplayName("Should find expenses by house ID and status")
    void testFindByHouseIdAndStatus_Success() {
        // Dato uno stato di spesa
        String status = StatusExpense.PENDING.name();
        List<Expense> expectedExpenses = Collections.singletonList(testExpense1);
        when(expenseRepository.findByHouseIdAndStatus(testHouseId, status)).thenReturn(expectedExpenses);

        // Quando cerco le spese per casa e stato
        List<Expense> result = expenseRepository.findByHouseIdAndStatus(testHouseId, status);

        // Allora verifico che le spese siano state filtrate correttamente
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(StatusExpense.PENDING, result.get(0).getStatus());
        assertEquals(testHouseId, result.get(0).getHouseId());
        verify(expenseRepository, times(1)).findByHouseIdAndStatus(testHouseId, status);
    }

    /**
     * Test per verificare il recupero delle spese per partecipante e stato.
     */
    @Test
    @DisplayName("Should find expenses by participant and status")
    void testFindByParticipantAndStatus_Success() {
        // Dato uno stato di spesa
        String status = StatusExpense.SETTLED.name();
        List<Expense> expectedExpenses = Collections.singletonList(testExpense2);
        when(expenseRepository.findByParticipantAndStatus(testUserId, status)).thenReturn(expectedExpenses);

        // Quando cerco le spese per partecipante e stato
        List<Expense> result = expenseRepository.findByParticipantAndStatus(testUserId, status);

        // Allora verifico che le spese siano state filtrate correttamente
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(StatusExpense.SETTLED, result.get(0).getStatus());
        assertTrue(result.get(0).getParticipants().contains(testUserId));
        verify(expenseRepository, times(1)).findByParticipantAndStatus(testUserId, status);
    }

    /**
     * Test per verificare il recupero delle spese per casa e intervallo di date.
     */
    @Test
    @DisplayName("Should find expenses by house ID and date range")
    void testFindByHouseIdAndDateRange_Success() {
        // Date le spese attese nell'intervallo di date
        List<Expense> expectedExpenses = Arrays.asList(testExpense1, testExpense2);
        when(expenseRepository.findByHouseIdAndDateRange(testHouseId, testStartDate, testEndDate))
                .thenReturn(expectedExpenses);

        // Quando cerco le spese per casa e intervallo di date
        List<Expense> result = expenseRepository.findByHouseIdAndDateRange(testHouseId, testStartDate, testEndDate);

        // Allora verifico che le spese siano state filtrate correttamente
        assertNotNull(result);
        assertEquals(2, result.size());
        result.forEach(expense -> {
            assertEquals(testHouseId, expense.getHouseId());
            assertTrue(expense.getCreatedDate().compareTo(testStartDate) >= 0);
            assertTrue(expense.getCreatedDate().compareTo(testEndDate) <= 0);
        });
        verify(expenseRepository, times(1)).findByHouseIdAndDateRange(testHouseId, testStartDate, testEndDate);
    }

    /**
     * Test per verificare il recupero delle spese per casa e categoria.
     */
    @Test
    @DisplayName("Should find expenses by house ID and category")
    void testFindByHouseIdAndCategory_Success() {
        // Data una categoria di spesa
        String category = CategoryExpense.FOOD.name();
        List<Expense> expectedExpenses = Collections.singletonList(testExpense1);
        when(expenseRepository.findByHouseIdAndCategory(testHouseId, category)).thenReturn(expectedExpenses);

        // Quando cerco le spese per casa e categoria
        List<Expense> result = expenseRepository.findByHouseIdAndCategory(testHouseId, category);

        // Allora verifico che le spese siano state filtrate correttamente
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(CategoryExpense.FOOD, result.get(0).getCategory());
        assertEquals(testHouseId, result.get(0).getHouseId());
        verify(expenseRepository, times(1)).findByHouseIdAndCategory(testHouseId, category);
    }

    /**
     * Test per verificare il comportamento con parametri nulli.
     */
    @Test
    @DisplayName("Should handle null parameters gracefully")
    void testRepositoryMethods_WithNullParameters() {
        // Dati parametri nulli
        when(expenseRepository.findByExpenseId(null)).thenReturn(null);
        when(expenseRepository.findByHouseId(null)).thenReturn(List.of());
        when(expenseRepository.findByCreatedBy(null)).thenReturn(List.of());

        // Quando e Allora verifico che i metodi gestiscano correttamente i null
        assertNull(expenseRepository.findByExpenseId(null));
        assertTrue(expenseRepository.findByHouseId(null).isEmpty());
        assertTrue(expenseRepository.findByCreatedBy(null).isEmpty());

        verify(expenseRepository, times(1)).findByExpenseId(null);
        verify(expenseRepository, times(1)).findByHouseId(null);
        verify(expenseRepository, times(1)).findByCreatedBy(null);
    }

    /**
     * Test per verificare che tutti i metodi del repository vengano chiamati correttamente.
     */
    @Test
    @DisplayName("Should verify all repository methods are called correctly")
    void testAllRepositoryMethodsInvocation() {
        // Dati parametri di test
        String expenseId = "test-expense";
        String houseId = "test-house";
        String userId = "test-user";
        String status = "PENDING";
        String category = "FOOD";
        Date startDate = new Date();
        Date endDate = new Date();

        // Quando chiamo tutti i metodi del repository
        expenseRepository.findByExpenseId(expenseId);
        expenseRepository.findByHouseId(houseId);
        expenseRepository.findByCreatedBy(userId);
        expenseRepository.findByParticipant(userId);
        expenseRepository.findByHouseIdAndStatus(houseId, status);
        expenseRepository.findByParticipantAndStatus(userId, status);
        expenseRepository.findByHouseIdAndDateRange(houseId, startDate, endDate);
        expenseRepository.findByHouseIdAndCategory(houseId, category);

        // Allora verifico che tutti i metodi siano stati chiamati esattamente una volta
        verify(expenseRepository, times(1)).findByExpenseId(expenseId);
        verify(expenseRepository, times(1)).findByHouseId(houseId);
        verify(expenseRepository, times(1)).findByCreatedBy(userId);
        verify(expenseRepository, times(1)).findByParticipant(userId);
        verify(expenseRepository, times(1)).findByHouseIdAndStatus(houseId, status);
        verify(expenseRepository, times(1)).findByParticipantAndStatus(userId, status);
        verify(expenseRepository, times(1)).findByHouseIdAndDateRange(houseId, startDate, endDate);
        verify(expenseRepository, times(1)).findByHouseIdAndCategory(houseId, category);
    }
}