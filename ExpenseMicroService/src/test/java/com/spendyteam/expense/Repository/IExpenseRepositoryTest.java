package com.spendyteam.expense.Repository;

import com.spendyteam.expense.Data.Expense;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test unitario per IExpenseRepository
 * Usa Mockito per simulare il comportamento del repository SENZA connessione al database reale
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Test per IExpenseRepository")
class IExpenseRepositoryTest {

    @Mock
    private IExpenseRepository expenseRepository;

    private Expense expense1;
    private Expense expense2;
    private Expense expense3;

    @BeforeEach
    void setUp() {
        expense1 = new Expense();
        expense1.setType("Payment");
        expense1.setProduct("Netflix");
        expense1.setStartedDate(LocalDateTime.of(2024, 1, 15, 10, 0));
        expense1.setCompletedDate(LocalDateTime.of(2024, 1, 15, 10, 5));
        expense1.setDescription("Netflix subscription");
        expense1.setAmount(new BigDecimal("15.99"));
        expense1.setFee(new BigDecimal("0.50"));
        expense1.setCurrency("EUR");
        expense1.setState("COMPLETED");
        expense1.setCategory("Abbonamenti e Servizi Digitali");
        expense1.setUsername("user1");

        expense2 = new Expense();
        expense2.setType("Transfer");
        expense2.setProduct("Groceries");
        expense2.setStartedDate(LocalDateTime.of(2024, 1, 16, 14, 0));
        expense2.setCompletedDate(LocalDateTime.of(2024, 1, 16, 14, 30));
        expense2.setDescription("Weekly groceries");
        expense2.setAmount(new BigDecimal("85.50"));
        expense2.setFee(new BigDecimal("1.20"));
        expense2.setCurrency("EUR");
        expense2.setState("COMPLETED");
        expense2.setCategory("Supermercati e Alimentari");
        expense2.setUsername("user2");

        expense3 = new Expense();
        expense3.setType("Payment");
        expense3.setProduct("Amazon Prime");
        expense3.setStartedDate(LocalDateTime.of(2024, 1, 17, 9, 0));
        expense3.setCompletedDate(LocalDateTime.of(2024, 1, 17, 9, 5));
        expense3.setDescription("Amazon Prime subscription");
        expense3.setAmount(new BigDecimal("8.99"));
        expense3.setFee(BigDecimal.ZERO);
        expense3.setCurrency("EUR");
        expense3.setState("COMPLETED");
        expense3.setCategory("Abbonamenti e Servizi Digitali");
        expense3.setUsername("user2");
    }

    @Test
    @DisplayName("Salva una spesa correttamente")
    void testSaveExpense() {
        // Simula il salvataggio
        when(expenseRepository.save(expense1)).thenReturn(expense1);

        Expense saved = expenseRepository.save(expense1);

        assertNotNull(saved);
        assertEquals("Netflix", saved.getProduct());
        assertEquals("user1", saved.getUsername());
        verify(expenseRepository, times(1)).save(expense1);
    }

    @Test
    @DisplayName("Trova spesa per ID")
    void testFindById() {
        // Simula il ritrovamento per ID
        when(expenseRepository.findById("exp1")).thenReturn(Optional.of(expense1));

        Optional<Expense> found = expenseRepository.findById("exp1");

        assertTrue(found.isPresent());
        assertEquals("Netflix", found.get().getProduct());
        assertEquals("user1", found.get().getUsername());
        verify(expenseRepository, times(1)).findById("exp1");
    }

    @Test
    @DisplayName("Restituisce Optional.empty() quando ID non trovato")
    void testFindByIdNotFound() {
        // Simula ID non trovato
        when(expenseRepository.findById("nonexistent-id")).thenReturn(Optional.empty());

        Optional<Expense> found = expenseRepository.findById("nonexistent-id");

        assertFalse(found.isPresent());
        verify(expenseRepository, times(1)).findById("nonexistent-id");
    }

    @Test
    @DisplayName("Trova spese per username con ordinamento")
    void testFindByUsernameWithSort() {
        List<Expense> user1Expenses = Arrays.asList(expense1, expense2);
        Sort sort = Sort.by(Sort.Direction.ASC, "startedDate");

        // Simula la ricerca con ordinamento
        when(expenseRepository.findByUsername("user1", sort)).thenReturn(user1Expenses);

        List<Expense> expenses = expenseRepository.findByUsername("user1", sort);

        assertEquals(2, expenses.size());
        assertEquals("Netflix", expenses.get(0).getProduct());
        assertEquals("Groceries", expenses.get(1).getProduct());
        verify(expenseRepository, times(1)).findByUsername("user1", sort);
    }

    @Test
    @DisplayName("Trova spese per username")
    void testFindByUsername() {
        List<Expense> user1Expenses = Arrays.asList(expense1, expense2);

        // Simula la ricerca
        when(expenseRepository.findByUsername("user1")).thenReturn(user1Expenses);

        List<Expense> expenses = expenseRepository.findByUsername("user1");

        assertEquals(2, expenses.size());
        verify(expenseRepository, times(1)).findByUsername("user1");
    }

    @Test
    @DisplayName("Restituisce lista vuota per username inesistente")
    void testFindByUsernameNoResults() {
        // Simula lista vuota
        when(expenseRepository.findByUsername("nonexistent-user")).thenReturn(Arrays.asList());

        List<Expense> expenses = expenseRepository.findByUsername("nonexistent-user");

        assertTrue(expenses.isEmpty());
        verify(expenseRepository, times(1)).findByUsername("nonexistent-user");
    }

    @Test
    @DisplayName("Verifica esistenza per date di inizio e completamento")
    void testExistsByStartedDateAndCompletedDate() {
        LocalDateTime startedDate = LocalDateTime.of(2024, 1, 15, 10, 0);
        LocalDateTime completedDate = LocalDateTime.of(2024, 1, 15, 10, 5);

        // Simula esistenza
        when(expenseRepository.existsByStartedDateAndCompletedDate(startedDate, completedDate))
            .thenReturn(true);

        boolean exists = expenseRepository.existsByStartedDateAndCompletedDate(startedDate, completedDate);

        assertTrue(exists);
        verify(expenseRepository, times(1)).existsByStartedDateAndCompletedDate(startedDate, completedDate);
    }

    @Test
    @DisplayName("Restituisce false quando date non trovate")
    void testExistsByStartedDateAndCompletedDateNotFound() {
        LocalDateTime startedDate = LocalDateTime.of(2024, 1, 20, 10, 0);
        LocalDateTime completedDate = LocalDateTime.of(2024, 1, 20, 10, 5);

        // Simula non esistenza
        when(expenseRepository.existsByStartedDateAndCompletedDate(startedDate, completedDate))
            .thenReturn(false);

        boolean exists = expenseRepository.existsByStartedDateAndCompletedDate(startedDate, completedDate);

        assertFalse(exists);
        verify(expenseRepository, times(1)).existsByStartedDateAndCompletedDate(startedDate, completedDate);
    }

    @Test
    @DisplayName("Verifica esistenza per descrizione e importo")
    void testExistsByDescriptionAndAmount() {
        // Simula esistenza
        when(expenseRepository.existsByDescriptionAndAmount("Netflix subscription", new BigDecimal("15.99")))
            .thenReturn(true);

        boolean exists = expenseRepository.existsByDescriptionAndAmount(
            "Netflix subscription",
            new BigDecimal("15.99")
        );

        assertTrue(exists);
        verify(expenseRepository, times(1))
            .existsByDescriptionAndAmount("Netflix subscription", new BigDecimal("15.99"));
    }

    @Test
    @DisplayName("Restituisce false quando descrizione e importo non trovati")
    void testExistsByDescriptionAndAmountNotFound() {
        // Simula non esistenza
        when(expenseRepository.existsByDescriptionAndAmount("Different description", new BigDecimal("99.99")))
            .thenReturn(false);

        boolean exists = expenseRepository.existsByDescriptionAndAmount(
            "Different description",
            new BigDecimal("99.99")
        );

        assertFalse(exists);
        verify(expenseRepository, times(1))
            .existsByDescriptionAndAmount("Different description", new BigDecimal("99.99"));
    }

    @Test
    @DisplayName("Elimina spese per username")
    void testDeleteByUsername() {
        // Simula eliminazione di 2 record
        when(expenseRepository.deleteByUsername("user1")).thenReturn(2L);

        long deleted = expenseRepository.deleteByUsername("user1");

        assertEquals(2, deleted);
        verify(expenseRepository, times(1)).deleteByUsername("user1");
    }

    @Test
    @DisplayName("Restituisce 0 quando username da eliminare non trovato")
    void testDeleteByUsernameNoMatches() {
        // Simula nessuna eliminazione
        when(expenseRepository.deleteByUsername("nonexistent-user")).thenReturn(0L);

        long deleted = expenseRepository.deleteByUsername("nonexistent-user");

        assertEquals(0, deleted);
        verify(expenseRepository, times(1)).deleteByUsername("nonexistent-user");
    }

    @Test
    @DisplayName("Elimina spesa per ID")
    void testDeleteExpense() {
        // Simula l'eliminazione (void method)
        doNothing().when(expenseRepository).deleteById("exp1");
        when(expenseRepository.findById("exp1")).thenReturn(Optional.empty());

        expenseRepository.deleteById("exp1");
        Optional<Expense> found = expenseRepository.findById("exp1");

        assertFalse(found.isPresent());
        verify(expenseRepository, times(1)).deleteById("exp1");
    }

    @Test
    @DisplayName("Aggiorna una spesa esistente")
    void testUpdateExpense() {
        Expense updatedExpense = new Expense();
        updatedExpense.setType("Payment");
        updatedExpense.setProduct("Netflix");
        updatedExpense.setAmount(new BigDecimal("19.99"));
        updatedExpense.setDescription("Updated Netflix subscription");
        updatedExpense.setUsername("user1");

        // Simula l'aggiornamento
        when(expenseRepository.save(any(Expense.class))).thenReturn(updatedExpense);

        Expense updated = expenseRepository.save(updatedExpense);

        assertEquals(new BigDecimal("19.99"), updated.getAmount());
        assertEquals("Updated Netflix subscription", updated.getDescription());
        verify(expenseRepository, times(1)).save(any(Expense.class));
    }

    @Test
    @DisplayName("Trova tutte le spese")
    void testFindAll() {
        List<Expense> allExpenses = Arrays.asList(expense1, expense2, expense3);

        // Simula findAll
        when(expenseRepository.findAll()).thenReturn(allExpenses);

        List<Expense> all = expenseRepository.findAll();

        assertEquals(3, all.size());
        verify(expenseRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Conta le spese")
    void testCount() {
        // Simula count progressivo
        when(expenseRepository.count()).thenReturn(0L).thenReturn(1L).thenReturn(2L);

        assertEquals(0, expenseRepository.count());
        assertEquals(1, expenseRepository.count());
        assertEquals(2, expenseRepository.count());

        verify(expenseRepository, times(3)).count();
    }

    @Test
    @DisplayName("Elimina tutte le spese")
    void testDeleteAll() {
        // Simula deleteAll (void method)
        doNothing().when(expenseRepository).deleteAll();
        when(expenseRepository.count()).thenReturn(0L);

        expenseRepository.deleteAll();
        long count = expenseRepository.count();

        assertEquals(0, count);
        verify(expenseRepository, times(1)).deleteAll();
    }
}

