package com.spendyteam.expense.Service;

import com.spendyteam.expense.Data.Expense;
import com.spendyteam.expense.Repository.IExpenseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.LongAdder;

/**
 * Component responsible for persisting large volumes of {@link Expense} records
 * in batches using Virtual Threads, throttled by a global static semaphore
 * to safeguard the MySQL connection pool.
 */
@Component
public class ExpenseBatchWriter {

    /**
     * Static semaphore shared across all instances and requests to limit concurrent MySQL batch writes to 10.
     */
    private static final Semaphore DB_WRITE_SEMAPHORE = new Semaphore(10);

    /**
     * Batch size for chunked persistence.
     */
    public static final int BATCH_SIZE = 500;

    private final IExpenseRepository defaultRepository;

    public ExpenseBatchWriter() {
        this(null);
    }

    @Autowired
    public ExpenseBatchWriter(@Autowired(required = false) IExpenseRepository defaultRepository) {
        this.defaultRepository = defaultRepository;
    }

    /**
     * Writes expenses in batches using the default injected repository.
     *
     * @param expenses list of expenses to save
     * @return number of records that failed to persist
     */
    public long writeInBatches(List<Expense> expenses) {
        return writeInBatches(this.defaultRepository, expenses);
    }

    /**
     * Writes expenses in batches to the provided repository.
     *
     * @param repository target expense repository
     * @param expenses   list of expenses to save
     * @return number of records that failed to persist
     */
    public long writeInBatches(IExpenseRepository repository, List<Expense> expenses) {
        if (expenses == null || expenses.isEmpty()) {
            return 0;
        }

        IExpenseRepository effectiveRepo = repository != null ? repository : this.defaultRepository;
        if (effectiveRepo == null) {
            throw new IllegalStateException("No IExpenseRepository available for batch persistence.");
        }

        List<List<Expense>> batches = chunkList(expenses, BATCH_SIZE);
        LongAdder dbErrors = new LongAdder();

        // Phase 2: Controlled batch I/O write protected by Semaphore
        try (var dbExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (List<Expense> batch : batches) {
                dbExecutor.submit(() -> {
                    try {
                        DB_WRITE_SEMAPHORE.acquire();
                        try {
                            effectiveRepo.saveAll(batch);
                        } finally {
                            DB_WRITE_SEMAPHORE.release();
                        }
                    } catch (Exception e) {
                        dbErrors.add(batch.size());
                        System.err.println("Batch write failed: " + e.getMessage());
                    }
                });
            }
        } // Wait for all batches to finish saving

        return dbErrors.sum();
    }

    /**
     * Splits a list into chunks of the given size.
     */
    public <T> List<List<T>> chunkList(List<T> list, int size) {
        List<List<T>> chunks = new ArrayList<>();
        if (list == null || list.isEmpty() || size <= 0) {
            return chunks;
        }
        for (int i = 0; i < list.size(); i += size) {
            chunks.add(list.subList(i, Math.min(list.size(), i + size)));
        }
        return chunks;
    }
}
