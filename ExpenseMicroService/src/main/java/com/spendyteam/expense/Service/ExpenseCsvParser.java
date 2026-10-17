package com.spendyteam.expense.Service;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.spendyteam.expense.Data.Expense;
import com.spendyteam.expense.Utility.ExpenseDataParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.LongAdder;

/**
 * Component responsible for parsing CSV expense files, header mapping,
 * delimiter detection, and parallel entity transformation using Virtual Threads.
 */
@Component
public class ExpenseCsvParser {

    private static final Logger log = LoggerFactory.getLogger(ExpenseCsvParser.class);

    @Value("${app.csv.max-rows:50000}")
    private int maxRows = 50000;

    @Value("${app.csv.max-file-size-bytes:10485760}")
    private long maxFileSizeBytes = 10 * 1024 * 1024; // 10MB default

    private final ExpenseDataParser dataParser;
    private final SmartCategorizerService categorizerService;

    public ExpenseCsvParser() {
        this(new ExpenseDataParser(), null);
    }

    @Autowired
    public ExpenseCsvParser(
            ExpenseDataParser dataParser,
            @Autowired(required = false) SmartCategorizerService categorizerService) {
        this.dataParser = dataParser != null ? dataParser : new ExpenseDataParser();
        this.categorizerService = categorizerService;
    }

    /**
     * Details about an individual row parse failure.
     */
    public record CsvRowError(int lineNumber, String message) {
    }

    /**
     * DTO containing parsed expenses, count of rows with parsing errors, structured error details, and total rows read.
     */
    public record CsvParseResult(
            List<Expense> expenses,
            long parseErrors,
            List<CsvRowError> errorDetails,
            long totalRowsRead) {

        public CsvParseResult(List<Expense> expenses, long parseErrors) {
            this(expenses, parseErrors, Collections.emptyList(), (expenses != null ? expenses.size() : 0) + parseErrors);
        }
    }

    private record IndexedExpense(int lineNumber, Expense expense) {
    }

    public int getMaxRows() {
        return maxRows;
    }

    public void setMaxRows(int maxRows) {
        this.maxRows = maxRows;
    }

    public long getMaxFileSizeBytes() {
        return maxFileSizeBytes;
    }

    public void setMaxFileSizeBytes(long maxFileSizeBytes) {
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    /**
     * Parses a CSV {@link MultipartFile} into a list of {@link Expense} records for the given username.
     *
     * @param file     CSV file upload
     * @param username user owning the expenses
     * @return parse result with list of parsed expenses and parse error count
     * @throws Exception if reading the stream or parsing headers fails
     */
    public CsvParseResult parse(MultipartFile file, String username) throws Exception {
        return parse(file, username, this.categorizerService);
    }

    /**
     * Parses a CSV {@link MultipartFile} with an explicit categorizer service instance.
     */
    public CsvParseResult parse(MultipartFile file, String username, SmartCategorizerService categorizer) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("No header.");
        }
        if (file.getSize() > maxFileSizeBytes) {
            throw new IllegalArgumentException(String.format("File size (%d bytes) exceeds the maximum allowed limit of %d bytes.", file.getSize(), maxFileSizeBytes));
        }
        return parse(file.getInputStream(), username, categorizer);
    }

    /**
     * Parses a CSV {@link InputStream} into a list of {@link Expense} records.
     */
    public CsvParseResult parse(InputStream inputStream, String username, SmartCategorizerService categorizer) throws Exception {
        SmartCategorizerService effectiveCategorizer = categorizer != null ? categorizer : this.categorizerService;

        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String headerLine = br.readLine();
            if (headerLine == null || headerLine.trim().isEmpty()) {
                throw new IllegalArgumentException("No header.");
            }

            char separator = detectSeparator(headerLine);
            CSVParser parser = new CSVParserBuilder().withSeparator(separator).build();
            String[] header = parser.parseLine(headerLine);

            try (CSVReader reader = new CSVReaderBuilder(br).withCSVParser(parser).build()) {
                Map<String, Integer> idx = mapHeaderToIndex(header);

                ConcurrentLinkedQueue<IndexedExpense> parsedExpenses = new ConcurrentLinkedQueue<>();
                ConcurrentLinkedQueue<CsvRowError> errorDetails = new ConcurrentLinkedQueue<>();
                LongAdder parseErrors = new LongAdder();

                int rowsRead = 0;

                // Phase 1: Parallel parsing using Virtual Threads preserving original row order
                try (var parserExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
                    String[] line;
                    while ((line = reader.readNext()) != null) {
                        rowsRead++;
                        if (rowsRead > maxRows) {
                            throw new IllegalArgumentException(String.format("CSV row count exceeds maximum allowed limit of %d rows.", maxRows));
                        }
                        final int currentLineNum = rowsRead + 1;
                        final String[] finalLine = line;

                        parserExecutor.submit(() -> {
                            try {
                                boolean empty = true;
                                for (String col : finalLine) {
                                    if (col != null && !col.trim().isEmpty()) {
                                        empty = false;
                                        break;
                                    }
                                }
                                if (empty) {
                                    return;
                                }

                                Expense expense = parseLineToExpense(finalLine, idx, username, effectiveCategorizer);
                                if (expense != null) {
                                    parsedExpenses.add(new IndexedExpense(currentLineNum, expense));
                                }
                            } catch (Exception e) {
                                parseErrors.increment();
                                errorDetails.add(new CsvRowError(currentLineNum, e.getMessage() != null ? e.getMessage() : "Parsing error"));
                                log.debug("CSV parse error at line {}: {}", currentLineNum, e.getMessage());
                            }
                        });
                    }
                } // Await completion of all line parsing tasks

                List<Expense> sortedExpenses = parsedExpenses.stream()
                        .sorted(Comparator.comparingInt(IndexedExpense::lineNumber))
                        .map(IndexedExpense::expense)
                        .toList();

                List<CsvRowError> sortedErrors = errorDetails.stream()
                        .sorted(Comparator.comparingInt(CsvRowError::lineNumber))
                        .toList();

                return new CsvParseResult(new ArrayList<>(sortedExpenses), parseErrors.sum(), sortedErrors, rowsRead);
            }
        }
    }

    /**
     * Maps an individual CSV row into an {@link Expense} entity.
     */
    public Expense parseLineToExpense(String[] line, Map<String, Integer> idx, String username, SmartCategorizerService categorizer) {
        Expense expense = new Expense();

        expense.setType(getValueByIndex(line, idx.get("type")));
        expense.setProduct(getValueByIndex(line, idx.get("product")));
        expense.setStartedDate(dataParser.parseToLocalDateTime(getValueByIndex(line, idx.get("startedDate"))));
        expense.setCompletedDate(dataParser.parseToLocalDateTime(getValueByIndex(line, idx.get("completedDate"))));

        String desc = getValueByIndex(line, idx.get("description"));
        expense.setDescription(desc);

        if (categorizer != null && desc != null) {
            expense.setCategory(categorizer.predictCategory(desc));
        }

        expense.setAmount(dataParser.parseBigDecimal(getValueByIndex(line, idx.get("amount"))));
        expense.setFee(dataParser.parseBigDecimal(getValueByIndex(line, idx.get("fee"))));
        expense.setCurrency(getValueByIndex(line, idx.get("currency")));
        expense.setState(getValueByIndex(line, idx.get("state")));
        expense.setUsername(username);

        return expense;
    }

    /**
     * Automatically detects separator delimiter (comma, semicolon, or tab) based on frequency in header line.
     */
    public char detectSeparator(String headerLine) {
        int countComma = headerLine.length() - headerLine.replace(",", "").length();
        int countSemi = headerLine.length() - headerLine.replace(";", "").length();
        int countTab = headerLine.length() - headerLine.replace("\t", "").length();
        char separator = ',';
        int max = Math.max(countComma, Math.max(countSemi, countTab));
        if (max == countSemi) separator = ';';
        else if (max == countTab) separator = '\t';
        return separator;
    }

    /**
     * Maps CSV header column names to logical model field indices using synonym lookup.
     */
    public Map<String, Integer> mapHeaderToIndex(String[] header) {
        Map<String, Integer> indexMap = new HashMap<>();
        Map<String, List<String>> synonyms = new HashMap<>();
        synonyms.put("type", Arrays.asList("Type", "Tipo"));
        synonyms.put("product", Arrays.asList("Product", "Prodotto", "item", "description_item"));
        synonyms.put("startedDate", Arrays.asList("started", "started_date", "starteddate", "Data di inizio", "start_date", "Data"));
        synonyms.put("completedDate", Arrays.asList("completed", "completed_date", "Data di completamento", "data_fine", "end_date", "Data"));
        synonyms.put("description", Arrays.asList("Description", "Descrizione", "Operazione"));
        synonyms.put("amount", Arrays.asList("Amount", "Importo", "value", "valore", "totale"));
        synonyms.put("fee", Arrays.asList("Fee", "tax", "commission"));
        synonyms.put("currency", Arrays.asList("Currency", "Valuta", "moneta"));
        synonyms.put("state", Arrays.asList("State", "Stato", "status", "Contabilizzazione"));

        for (int i = 0; i < header.length; i++) {
            String h = header[i].trim().toLowerCase(Locale.ROOT).replaceAll("[\"]", "");
            for (Map.Entry<String, List<String>> e : synonyms.entrySet()) {
                for (String syn : e.getValue()) {
                    if (h.equals(syn.toLowerCase(Locale.ROOT)) || h.contains(syn.toLowerCase(Locale.ROOT))) {
                        indexMap.putIfAbsent(e.getKey(), i);
                    }
                }
            }
        }
        return indexMap;
    }

    /**
     * Safely retrieves trimmed value from line array at specified column index.
     */
    public String getValueByIndex(String[] line, Integer idx) {
        if (idx == null || idx < 0 || idx >= line.length) return null;
        String v = line[idx];
        return (v == null || v.trim().isEmpty()) ? null : v.trim();
    }
}
