package com.spendyteam.expense.Service;

import com.spendyteam.expense.Data.Expense;
import com.spendyteam.expense.Repository.IExpenseRepository;
import com.spendyteam.expense.Utility.ExpenseDataParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Performance Engineering & Concurrency Benchmarks (1,000, 10,000, 50,000 rows)")
class ExpenseBenchmarkAndConcurrencyTest {

    private static final Logger log = LoggerFactory.getLogger(ExpenseBenchmarkAndConcurrencyTest.class);

    @Mock
    private IExpenseRepository expenseRepository;

    @Mock
    private SmartCategorizerService categorizerService;

    private ExpenseDataParser dataParser;
    private ExpenseCsvParser csvParser;
    private ExpenseBatchWriter batchWriter;

    @BeforeEach
    void setUp() {
        dataParser = new ExpenseDataParser();
        csvParser = new ExpenseCsvParser(dataParser, categorizerService);
        batchWriter = new ExpenseBatchWriter(expenseRepository);
    }

    private byte[] generateSyntheticCsv(int rows) {
        StringBuilder sb = new StringBuilder(rows * 110);
        sb.append("Type,Product,Started Date,Completed Date,Description,Amount,Fee,Currency,State\n");
        for (int i = 1; i <= rows; i++) {
            sb.append(String.format("CARD_PAYMENT,Merchant_%d,2024-03-01 10:00:00,2024-03-01 10:05:00,Grocery purchase %d,%d.75,0.00,EUR,COMPLETED\n",
                    (i % 100), i, (10 + (i % 500))));
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("Benchmark CSV Import: 1,000 synthetic rows")
    void benchmarkImport1000Rows() throws Exception {
        byte[] csvData = generateSyntheticCsv(1_000);
        when(categorizerService.predictCategory(any())).thenReturn("Supermercati");
        when(expenseRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        System.gc();
        long memBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long startParse = System.nanoTime();

        ExpenseCsvParser.CsvParseResult parseResult = csvParser.parse(new ByteArrayInputStream(csvData), "bench_user", categorizerService);
        long endParse = System.nanoTime();

        long startWrite = System.nanoTime();
        ExpenseBatchWriter.BatchWriteSummary summary = batchWriter.writeInBatchesWithSummary(expenseRepository, parseResult.expenses());
        long endWrite = System.nanoTime();

        long memAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        double parseDurationMs = (endParse - startParse) / 1_000_000.0;
        double writeDurationMs = (endWrite - startWrite) / 1_000_000.0;
        double totalDurationMs = parseDurationMs + writeDurationMs;
        double rowsPerSec = (1_000.0 / (totalDurationMs / 1000.0));
        long memUsedMb = Math.max(0, (memAfter - memBefore) / (1024 * 1024));

        log.info("=== BENCHMARK 1,000 ROWS ===");
        log.info("Total Duration: {} ms (Parse: {} ms, DB Batch Write: {} ms)",
                String.format("%.2f", totalDurationMs), String.format("%.2f", parseDurationMs), String.format("%.2f", writeDurationMs));
        log.info("Throughput: {} rows/second", String.format("%.2f", rowsPerSec));
        log.info("Heap Memory Delta: {} MB", memUsedMb);
        log.info("Parsed: {}, Persisted: {}, Errors: {}", parseResult.expenses().size(), summary.persistedCount(), summary.failedCount());

        assertEquals(1_000, parseResult.expenses().size());
        assertEquals(0, parseResult.parseErrors());
        assertEquals(1_000, summary.persistedCount());
        assertEquals(0, summary.failedCount());
    }

    @Test
    @DisplayName("Benchmark CSV Import: 10,000 synthetic rows")
    void benchmarkImport10000Rows() throws Exception {
        byte[] csvData = generateSyntheticCsv(10_000);
        when(categorizerService.predictCategory(any())).thenReturn("Supermercati");
        when(expenseRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        System.gc();
        long memBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long startParse = System.nanoTime();

        ExpenseCsvParser.CsvParseResult parseResult = csvParser.parse(new ByteArrayInputStream(csvData), "bench_user", categorizerService);
        long endParse = System.nanoTime();

        long startWrite = System.nanoTime();
        ExpenseBatchWriter.BatchWriteSummary summary = batchWriter.writeInBatchesWithSummary(expenseRepository, parseResult.expenses());
        long endWrite = System.nanoTime();

        long memAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        double parseDurationMs = (endParse - startParse) / 1_000_000.0;
        double writeDurationMs = (endWrite - startWrite) / 1_000_000.0;
        double totalDurationMs = parseDurationMs + writeDurationMs;
        double rowsPerSec = (10_000.0 / (totalDurationMs / 1000.0));
        long memUsedMb = Math.max(0, (memAfter - memBefore) / (1024 * 1024));

        log.info("=== BENCHMARK 10,000 ROWS ===");
        log.info("Total Duration: {} ms (Parse: {} ms, DB Batch Write: {} ms)",
                String.format("%.2f", totalDurationMs), String.format("%.2f", parseDurationMs), String.format("%.2f", writeDurationMs));
        log.info("Throughput: {} rows/second", String.format("%.2f", rowsPerSec));
        log.info("Heap Memory Delta: {} MB", memUsedMb);
        log.info("Parsed: {}, Persisted: {}, Errors: {}", parseResult.expenses().size(), summary.persistedCount(), summary.failedCount());

        assertEquals(10_000, parseResult.expenses().size());
        assertEquals(0, parseResult.parseErrors());
        assertEquals(10_000, summary.persistedCount());
        assertEquals(0, summary.failedCount());
    }

    @Test
    @DisplayName("Benchmark CSV Parser: 50,000 synthetic rows")
    void benchmarkParse50000Rows() throws Exception {
        csvParser.setMaxRows(55_000);
        byte[] csvData = generateSyntheticCsv(50_000);
        when(categorizerService.predictCategory(any())).thenReturn("General");

        System.gc();
        long memBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long startParse = System.nanoTime();

        ExpenseCsvParser.CsvParseResult parseResult = csvParser.parse(new ByteArrayInputStream(csvData), "bench_user", categorizerService);
        long endParse = System.nanoTime();

        long memAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        double parseDurationMs = (endParse - startParse) / 1_000_000.0;
        double rowsPerSec = (50_000.0 / (parseDurationMs / 1000.0));
        long memUsedMb = Math.max(0, (memAfter - memBefore) / (1024 * 1024));

        log.info("=== BENCHMARK 50,000 ROWS (PARSING & VIRTUAL THREADS) ===");
        log.info("Parse Duration: {} ms", String.format("%.2f", parseDurationMs));
        log.info("Throughput: {} rows/second", String.format("%.2f", rowsPerSec));
        log.info("Heap Memory Delta: {} MB", memUsedMb);
        log.info("Parsed: {}, Errors: {}", parseResult.expenses().size(), parseResult.parseErrors());

        assertEquals(50_000, parseResult.expenses().size());
        assertEquals(0, parseResult.parseErrors());
    }

    @Test
    @DisplayName("Concurrency Benchmark: 5 simultaneous imports")
    void testConcurrentImports() throws Exception {
        int concurrentUsers = 5;
        int rowsPerUser = 1_000;
        byte[] csvData = generateSyntheticCsv(rowsPerUser);

        when(categorizerService.predictCategory(any())).thenReturn("General");
        when(expenseRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        ExecutorService executor = Executors.newFixedThreadPool(concurrentUsers);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(concurrentUsers);
        List<Future<ExpenseBatchWriter.BatchWriteSummary>> futures = new ArrayList<>();

        long startBenchmark = System.nanoTime();

        for (int u = 0; u < concurrentUsers; u++) {
            final String username = "concurrent_user_" + u;
            futures.add(executor.submit(() -> {
                startLatch.await(); // ensure simultaneous execution
                ExpenseCsvParser.CsvParseResult parsed = csvParser.parse(new ByteArrayInputStream(csvData), username, categorizerService);
                return batchWriter.writeInBatchesWithSummary(expenseRepository, parsed.expenses());
            }));
        }

        startLatch.countDown(); // Release all threads at once
        executor.shutdown();
        boolean finished = executor.awaitTermination(30, TimeUnit.SECONDS);
        long endBenchmark = System.nanoTime();

        assertTrue(finished, "Concurrent execution timed out");

        double totalMs = (endBenchmark - startBenchmark) / 1_000_000.0;
        double totalRows = concurrentUsers * rowsPerUser;
        double throughput = (totalRows / (totalMs / 1000.0));

        log.info("=== CONCURRENT BENCHMARK (5 Threads x 1,000 Rows = 5,000 Rows) ===");
        log.info("Total Concurrent Duration: {} ms", String.format("%.2f", totalMs));
        log.info("Aggregated Throughput: {} rows/second", String.format("%.2f", throughput));

        for (Future<ExpenseBatchWriter.BatchWriteSummary> f : futures) {
            ExpenseBatchWriter.BatchWriteSummary summary = f.get();
            assertEquals(rowsPerUser, summary.persistedCount());
            assertEquals(0, summary.failedCount());
        }
    }
}
