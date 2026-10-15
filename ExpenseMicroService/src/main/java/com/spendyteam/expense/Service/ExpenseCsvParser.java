package com.spendyteam.expense.Service;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.spendyteam.expense.Data.Expense;
import com.spendyteam.expense.Utility.ExpenseDataParser;
import org.springframework.beans.factory.annotation.Autowired;
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
     * DTO containing parsed expenses and count of rows that produced parsing errors.
     */
    public record CsvParseResult(List<Expense> expenses, long parseErrors) {
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
        return parse(file.getInputStream(), username, categorizer);
    }

    /**
     * Parses a CSV {@link InputStream} into a list of {@link Expense} records.
     */
    public CsvParseResult parse(InputStream inputStream, String username, SmartCategorizerService categorizer) throws Exception {
        SmartCategorizerService effectiveCategorizer = categorizer != null ? categorizer : this.categorizerService;

        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String headerLine = br.readLine();
            if (headerLine == null) {
                throw new IllegalArgumentException("No header.");
            }

            char separator = detectSeparator(headerLine);
            CSVParser parser = new CSVParserBuilder().withSeparator(separator).build();
            String[] header = parser.parseLine(headerLine);

            try (CSVReader reader = new CSVReaderBuilder(br).withCSVParser(parser).build()) {
                Map<String, Integer> idx = mapHeaderToIndex(header);

                ConcurrentLinkedQueue<Expense> parsedExpenses = new ConcurrentLinkedQueue<>();
                LongAdder parseErrors = new LongAdder();

                // Phase 1: Parallel parsing using Virtual Threads (CPU bound)
                try (var parserExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
                    String[] line;
                    while ((line = reader.readNext()) != null) {
                        String[] finalLine = line;
                        parserExecutor.submit(() -> {
                            try {
                                Expense expense = parseLineToExpense(finalLine, idx, username, effectiveCategorizer);
                                if (expense != null) {
                                    parsedExpenses.add(expense);
                                }
                            } catch (Exception e) {
                                parseErrors.increment();
                            }
                        });
                    }
                } // Await completion of all line parsing tasks

                return new CsvParseResult(new ArrayList<>(parsedExpenses), parseErrors.sum());
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
