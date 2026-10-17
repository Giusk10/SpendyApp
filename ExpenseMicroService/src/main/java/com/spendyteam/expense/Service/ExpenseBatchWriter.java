package com.spendyteam.expense.Service;

import com.spendyteam.expense.Data.Expense;
import com.spendyteam.expense.Repository.IExpenseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(ExpenseBatchWriter.class);

    /**
     * Static semaphore shared across all instances and requests to limit concurrent MySQL batch writes to 10.
     */
    private static final Semaphore DB_WRITE_SEMAPHORE = new Semaphore(10);

    /**
     * Batch size for chunked persistence.
     */
    public static final int BATCH_SIZE = 500;

    /**
     * Structured summary of a batch persistence operation.
     */
    public record BatchWriteSummary(long totalSubmitted, long persistedCount, long failedCount) {
        public boolean isFullSuccess() {
            return failedCount == 0 && totalSubmitted > 0;
        }
        public boolean isPartialSuccess() {
            return persistedCount > 0 && failedCount > 0;
        }
        public boolean isCompleteFailure() {
            return persistedCount == 0 && totalSubmitted > 0;
        }
    }

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
     * Writes expenses in batches to the provided repository with structured summary.
     */
    public BatchWriteSummary writeInBatchesWithSummary(IExpenseRepository repository, List<Expense> expenses) {
        if (expenses == null || expenses.isEmpty()) {
            return new BatchWriteSummary(0, 0, 0);
        }
        long failed = writeInBatches(repository, expenses);
        long persisted = expenses.size() - failed;
        return new BatchWriteSummary(expenses.size(), persisted, failed);
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
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        dbErrors.add(batch.size());
                        log.error("Batch write interrupted while waiting for semaphore permit: {}", ie.getMessage());
                    } catch (Exception e) {
                        dbErrors.add(batch.size());
                        log.error("Batch write failed for {} records: {}", batch.size(), e.getMessage());
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
