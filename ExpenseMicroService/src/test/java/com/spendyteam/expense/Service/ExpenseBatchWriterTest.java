package com.spendyteam.expense.Service;

import com.spendyteam.expense.Data.Expense;
import com.spendyteam.expense.Repository.IExpenseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpenseBatchWriterTest {

    @Mock
    private IExpenseRepository expenseRepository;

    private ExpenseBatchWriter batchWriter;

    @BeforeEach
    void setUp() {
        batchWriter = new ExpenseBatchWriter(expenseRepository);
    }

    @Test
    void testChunkList() {
        List<Integer> list = List.of(1, 2, 3, 4, 5, 6, 7);
        List<List<Integer>> chunks = batchWriter.chunkList(list, 3);

        assertEquals(3, chunks.size());
        assertEquals(List.of(1, 2, 3), chunks.get(0));
        assertEquals(List.of(4, 5, 6), chunks.get(1));
        assertEquals(List.of(7), chunks.get(2));
    }

    @Test
    void testChunkListEmptyOrInvalid() {
        assertTrue(batchWriter.chunkList(Collections.emptyList(), 5).isEmpty());
        assertTrue(batchWriter.chunkList(null, 5).isEmpty());
        assertTrue(batchWriter.chunkList(List.of(1, 2), 0).isEmpty());
    }

    @Test
    void testWriteInBatchesSuccess() {
        List<Expense> expenses = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            Expense e = new Expense();
            e.setDescription("Item " + i);
            expenses.add(e);
        }

        when(expenseRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        long errors = batchWriter.writeInBatches(expenseRepository, expenses);

        assertEquals(0, errors);
        verify(expenseRepository, atLeastOnce()).saveAll(anyList());
    }

    @Test
    void testWriteInBatchesWithEmptyList() {
        long errors = batchWriter.writeInBatches(expenseRepository, Collections.emptyList());
        assertEquals(0, errors);
        verifyNoInteractions(expenseRepository);
    }

    @Test
    void testWriteInBatchesWithErrorHandling() {
        List<Expense> expenses = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Expense e = new Expense();
            e.setDescription("Item " + i);
            expenses.add(e);
        }

        doThrow(new RuntimeException("Database error")).when(expenseRepository).saveAll(anyList());

        long errors = batchWriter.writeInBatches(expenseRepository, expenses);

        assertEquals(10, errors);
    }
}
