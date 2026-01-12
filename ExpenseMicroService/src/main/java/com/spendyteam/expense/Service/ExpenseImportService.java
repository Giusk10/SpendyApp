package com.spendyteam.expense.Service;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.spendyteam.expense.Data.Expense;
import com.spendyteam.expense.Repository.IExpenseRepository;
import com.spendyteam.expense.Utility.ExpenseClassifier;
import jakarta.ws.rs.core.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.Executors; // Importante per i Virtual Threads
import java.util.stream.Collectors;

@Service
public class ExpenseImportService {

    @Autowired
    private IExpenseRepository expenseRepository;
    private WebClient webClient;

    public ExpenseImportService() {
        this.webClient = WebClient.builder().build();
    }

    public Response importExpensesFromCsv(MultipartFile file, String token) throws Exception {
        if (file.isEmpty()) {
            return Response.status(Response.Status.NO_CONTENT).entity("CSV file is empty.").build();
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String headerLine = br.readLine(); // read raw header line to detect separator
            if (headerLine == null) {
                return Response.status(Response.Status.BAD_REQUEST).entity("CSV file has no header.").build();
            }

            // detect separator logic
            int countComma = headerLine.length() - headerLine.replace(",", "").length();
            int countSemi = headerLine.length() - headerLine.replace(";", "").length();
            int countTab = headerLine.length() - headerLine.replace("\t", "").length();
            char separator = ',';
            int max = Math.max(countComma, Math.max(countSemi, countTab));
            if (max == countSemi) separator = ';';
            else if (max == countTab) separator = '\t';

            CSVParser parser = new CSVParserBuilder().withSeparator(separator).build();
            String[] header = parser.parseLine(headerLine);

            try (CSVReader reader = new CSVReaderBuilder(br).withCSVParser(parser).build()) {
                Map<String, Integer> idx = mapHeaderToIndex(header);

                // --- OTTIMIZZAZIONE 1: Chiamata Auth unica ---
                // Invece di chiamare il Gateway per ogni riga, lo facciamo una volta sola qui.
                String username = getUsernameFromTokenViaRest(token);
                if (username == null) {
                    return Response.status(Response.Status.UNAUTHORIZED).entity("Invalid Token").build();
                }

                // --- OTTIMIZZAZIONE 2: Virtual Threads ---
                // Usiamo un Executor che sfrutta i Virtual Threads di Java 21 per parallelizzare l'import
                try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {

                    String[] line;
                    while ((line = reader.readNext()) != null) {
                        // Variabile finale per la lambda
                        String[] finalLine = line;

                        // Sottometto il task all'executor
                        executor.submit(() -> {
                            try {
                                processAndSaveLine(finalLine, idx, username);
                            } catch (Exception e) {
                                System.err.println("Errore durante l'importazione della riga: " + e.getMessage());
                                // Non fermiamo l'intero processo per una riga guasta
                            }
                        });
                    }

                } // Il blocco try-with-resources attende automaticamente che TUTTI i thread finiscano

                // Controllo finale
                if (expenseRepository.count() > 0) {
                    return Response.ok("Expenses imported successfully in background").build();
                } else {
                    return Response.status(Response.Status.BAD_REQUEST).entity("No expenses found or saved.").build();
                }
            }
        } catch (DateTimeParseException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Failed to parse date: " + e.getParsedString()).build();
        }
    }

    // --- NUOVO METODO HELPER per processare la singola riga ---
    private void processAndSaveLine(String[] line, Map<String, Integer> idx, String username) {
        Expense expense = new Expense();

        // Estrazione dati
        String type = getValueByIndex(line, idx.get("type"));
        String product = getValueByIndex(line, idx.get("product"));
        String startedRaw = getValueByIndex(line, idx.get("startedDate"));
        String completedRaw = getValueByIndex(line, idx.get("completedDate"));
        String description = getValueByIndex(line, idx.get("description"));
        String amountRaw = getValueByIndex(line, idx.get("amount"));
        String feeRaw = getValueByIndex(line, idx.get("fee"));
        String currency = getValueByIndex(line, idx.get("currency"));
        String state = getValueByIndex(line, idx.get("state"));

        // Setting dati
        expense.setType(type);
        expense.setProduct(product);
        expense.setStartedDate(parseToLocalDateTime(startedRaw));
        expense.setCompletedDate(parseToLocalDateTime(completedRaw));
        expense.setDescription(description);
        expense.setAmount(parseBigDecimal(amountRaw));
        expense.setFee(parseBigDecimal(feeRaw));
        expense.setCurrency(currency);
        expense.setState(state);

        // Classificazione automatica
        expense.setCategory(ExpenseClassifier.classify(description));

        // Impostazione Username (già recuperato prima)
        expense.setUsername(username);

        // Salvataggio su DB
        expenseRepository.save(expense);
    }

    // --- MAPPATURA HEADER ---
    private Map<String, Integer> mapHeaderToIndex(String[] header) {
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

    private String getValueByIndex(String[] line, Integer idx) {
        if (idx == null) return null;
        if (idx < 0 || idx >= line.length) return null;
        String v = line[idx];
        if (v == null) return null;
        v = v.trim();
        return v.isEmpty() ? null : v;
    }

    // --- PARSING HELPERS ---
    private LocalDateTime parseToLocalDateTime(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String v = value.trim();

        try {
            return java.time.ZonedDateTime.parse(v).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
        }

        List<String> patterns = Arrays.asList(
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd",
                "dd/MM/yyyy",
                "dd/MM/yyyy HH:mm:ss"
        );

        for (String p : patterns) {
            try {
                DateTimeFormatter f = DateTimeFormatter.ofPattern(p);
                if (p.contains("H") || p.contains("h")) {
                    return LocalDateTime.parse(v, f);
                } else {
                    LocalDate d = LocalDate.parse(v, f);
                    return d.atStartOfDay();
                }
            } catch (DateTimeParseException ignored) {
            }
        }

        try {
            long num = Long.parseLong(v);
            if (v.length() >= 13) {
                return LocalDateTime.ofEpochSecond(num / 1000, 0, ZoneOffset.UTC);
            } else {
                return LocalDateTime.ofEpochSecond(num, 0, ZoneOffset.UTC);
            }
        } catch (NumberFormatException ignored) {
        }
        return LocalDateTime.parse(v);
    }

    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        String v = value.trim();
        v = v.replaceAll("[€$£¥]", "").trim();
        v = v.replaceAll("\u00A0", "").trim();

        if (v.contains(",") && v.contains(".")) {
            int lastComma = v.lastIndexOf(',');
            int lastDot = v.lastIndexOf('.');
            if (lastDot > lastComma) {
                v = v.replaceAll(",", "");
            } else {
                v = v.replaceAll("\\.", "");
                v = v.replaceAll(",", ".");
            }
        } else if (v.contains(",") && !v.contains(".")) {
            v = v.replaceAll(",", ".");
        }

        v = v.replaceAll("[^0-9.\\-]", "");
        if (v.isEmpty()) return BigDecimal.ZERO;
        return new BigDecimal(v);
    }

    public Response getExpenses(String token) {
        try {
            String username = getUsernameFromTokenViaRest(token);
            Iterable<Expense> expenses = expenseRepository.findAll(Sort.by(Sort.Direction.ASC, "startedDate"))
                    .stream()
                    .filter(e -> e.getAmount() != null && e.getAmount().compareTo(BigDecimal.ZERO) < 0 && username != null && username.equals(e.getUsername()))
                    .collect(Collectors.toList());

            if (!expenses.iterator().hasNext()) {
                return Response.status(Response.Status.NO_CONTENT).entity("No expenses found.").build();
            }
            return Response.ok(expenses).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Failed to retrieve expenses: " + e.getMessage()).build();
        }
    }


    public Response getExpenseByDate(String startedDate, String endDate, String token) {

        String username = getUsernameFromTokenViaRest(token);

        try {
            LocalDateTime start = parseToLocalDateTime(startedDate);
            LocalDateTime end = parseToLocalDateTime(endDate);

            Iterable<Expense> expenses = expenseRepository.findAll().stream()
                .filter(e -> e.getStartedDate() != null
                        && e.getCompletedDate() != null
                        && !e.getStartedDate().isBefore(start)
                        && !e.getCompletedDate().isAfter(end)
                        && e.getStartedDate().isBefore(end)
                        && e.getCompletedDate().isAfter(start)
                        && e.getAmount() != null
                        && e.getAmount().compareTo(BigDecimal.ZERO) < 0)
                .filter(e -> username != null && username.equals(e.getUsername()))
                .toList();


            if (!expenses.iterator().hasNext()) {
                return Response.status(Response.Status.NO_CONTENT).entity("No expenses found for the given date range.").build();
            }
            return Response.ok(expenses).build();
        } catch (DateTimeParseException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Invalid date format: " + e.getMessage()).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Failed to retrieve expenses: " + e.getMessage()).build();
        }
    }



    public Response getExpenseByMonth_Year(String month, String year, String token) {

        String startDate = year + "-" + month + "-01 00:00:00";
        String endDate = year + "-" + month + "-31 23:59:59";

        Response res = getExpenseByDate(startDate, endDate, token);

        if (res.getStatus() == Response.Status.OK.getStatusCode()) {
            return Response.ok(res.getEntity()).build();
        } else {
            return Response.status(res.getStatus()).entity(res.getEntity()).build();
        }
    }



    public Response getMonthly_Amount_of_Year(String year , String token) {
        String startDate = year + "-01-01 00:00:00";
        String endDate = year + "-12-31 23:59:59";

        Response res = getExpenseByDate(startDate, endDate, token);

        if (res.getStatus() == Response.Status.OK.getStatusCode()) {
            Iterable<?> expensesObj = (Iterable<?>) res.getEntity();

            HashMap<String, BigDecimal> monthlyAmount = new HashMap<>();

            for (Object o : expensesObj) {
                if (!(o instanceof Expense expense)) continue;
                if (expense.getStartedDate() != null) {
                    if (expense.getAmount().compareTo(BigDecimal.ZERO) > 0) {
                        continue;
                    }
                    String monthKey = expense.getStartedDate().format(DateTimeFormatter.ofPattern("yyyy-MM"));
                    monthlyAmount.put(monthKey, monthlyAmount.getOrDefault(monthKey, BigDecimal.ZERO).add(expense.getAmount()));
                }
            }

            return Response.ok(monthlyAmount).build();
        }
        else if (res.getStatus() == Response.Status.NO_CONTENT.getStatusCode()) {
            return Response.status(Response.Status.NO_CONTENT).entity("No expenses found for the given year.").build();
        }
        else {
            return Response.status(res.getStatus()).entity(res.getEntity()).build();
        }
    }

    private String getUsernameFromTokenViaRest(String token) {
        try {
            Map<String, String> response = webClient.post()
                    .uri("http://localhost:7860/gateway/verify-token")
                    .bodyValue(Map.of("token", token))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            // La risposta JSON dovrebbe essere tipo {"username": "theUser"}
            return response != null ? response.get("username") : null;
        } catch (Exception e) {
            return null;
        }
    }

    public Response deleteExpense(String expenseId, String token) {
        try {
            String username = getUsernameFromTokenViaRest(token);
            if (username == null) {
                return Response.status(Response.Status.UNAUTHORIZED).entity("Invalid or expired token.").build();
            }

            Optional<Expense> expenseOpt = expenseRepository.findById(expenseId);
            if (expenseOpt.isPresent()) {
                Expense expense = expenseOpt.get();
                // Verifica che l'utente sia il proprietario della spesa
                if (!username.equals(expense.getUsername())) {
                    return Response.status(Response.Status.FORBIDDEN).entity("You are not authorized to delete this expense.").build();
                }
                expenseRepository.deleteById(expenseId);
                return Response.status(Response.Status.OK).entity("Expense deleted successfully.").build();

            } else {
                return Response.status(Response.Status.NOT_FOUND).entity("Expense not found.").build();
            }
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Failed to delete expense: " + e.getMessage()).build();
        }
    }

    public Response addExpense(Map<String, String> body, String token) {
        try {
            Expense expense = new Expense();

            expense.setType(body.get("type"));
            expense.setProduct(body.get("product"));
            expense.setStartedDate(parseToLocalDateTime(body.get("startedDate")));
            expense.setCompletedDate(parseToLocalDateTime(body.get("completedDate")));
            expense.setDescription(body.get("description"));
            expense.setAmount(parseBigDecimal(body.get("amount")));
            expense.setFee(parseBigDecimal(body.get("fee")));
            expense.setCurrency(body.get("currency"));
            expense.setState(body.get("state"));
            expense.setCategory(ExpenseClassifier.classify(body.get("description")));

            String username = getUsernameFromTokenViaRest(token);
            expense.setUsername(username);

            expenseRepository.save(expense);
            return Response.status(Response.Status.CREATED).entity(expense).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Failed to add expense: " + e.getMessage()).build();
        }
    }
}
