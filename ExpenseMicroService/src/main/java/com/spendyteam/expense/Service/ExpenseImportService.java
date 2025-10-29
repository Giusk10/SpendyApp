package com.spendyteam.expense.Service;

import com.opencsv.CSVReader;
import com.spendyteam.expense.Data.Expense;
import com.spendyteam.expense.Repository.IExpenseRepository;
import com.spendyteam.expense.Utility.ExpenseClassifier;
import jakarta.ws.rs.core.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;

@Service
public class ExpenseImportService {

    @Autowired
    private IExpenseRepository expenseRepository;

    public Response importExpensesFromCsv(MultipartFile file) throws Exception {
        if (file.isEmpty()) {
            return Response.status(Response.Status.NO_CONTENT).entity("CSV file is empty.").build();
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            String[] line;
            reader.readNext(); // Skip header row

            while ((line = reader.readNext()) != null) {
                Expense expense = new Expense();

                expense.setType(line[0]);
                expense.setProduct(line[1]);

                // Parsing date/time e conversione a LocalDate
                expense.setStartedDate(parseToLocalDateTime(line[2], formatter));
                expense.setCompletedDate(parseToLocalDateTime(line[3], formatter));

                expense.setDescription(line[4]);
                expense.setAmount(parseBigDecimal(line[5]));
                expense.setFee(parseBigDecimal(line[6]));
                expense.setCurrency(line[7]);
                expense.setState(line[8]);
                expense.setCategory(ExpenseClassifier.classify(line[4]));

                if (!expenseRepository.existsByStartedDateAndCompletedDate(
                        expense.getStartedDate(), expense.getCompletedDate())) {
                    expenseRepository.save(expense);
                    System.out.println("Expense saved: " + expense);
                }
            }

            if (expenseRepository.count() > 0) {
                return Response.ok("CSV file imported successfully.").build();
            } else {
                return Response.status(Response.Status.BAD_REQUEST).entity("No expenses found in the CSV file.").build();
            }
        } catch (DateTimeParseException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Failed to parse date: " + e.getParsedString()).build();
        }
    }

    private LocalDateTime parseToLocalDateTime(String value, DateTimeFormatter formatter) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        LocalDateTime dateTime = LocalDateTime.parse(value.trim(), formatter);
        return dateTime;
    }

    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(value.replace(",", "").trim());
    }

    public Response getExpenses() {
        try {
            Iterable<Expense> expenses = expenseRepository.findAll();
            if (!expenses.iterator().hasNext()) {
                return Response.status(Response.Status.NO_CONTENT).entity("No expenses found.").build();
            }
            return Response.ok(expenses).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Failed to retrieve expenses: " + e.getMessage()).build();
        }
    }


    public Response getExpenseByDate(String startedDate, String endDate) {

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime start = parseToLocalDateTime(startedDate, formatter);
            LocalDateTime end = parseToLocalDateTime(endDate, formatter);

            Iterable<Expense> expenses = expenseRepository.findAll().stream()
                .filter(e -> e.getStartedDate() != null && e.getCompletedDate() != null
                        && !e.getStartedDate().isBefore(start)
                        && !e.getCompletedDate().isAfter(end)
                        && e.getStartedDate().isBefore(end)
                        && e.getCompletedDate().isAfter(start))
                .toList();

            if (!expenses.iterator().hasNext()) {
                return Response.status(Response.Status.NO_CONTENT).entity("No expenses found for the given date range.").build();
            }
            return Response.ok(expenses).build();
        } catch (DateTimeParseException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Invalid date format: " + e.getMessage()).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Failed to retrieve expenses: " + e.getMessage()).build();
        }
    }

    public Response getExpenseByMonth_Year(String month, String year) {

        String startDate = year + "-" + month + "-01 00:00:00";
        String endDate = year + "-" + month + "-31 23:59:59";

        Response res = getExpenseByDate(startDate, endDate);

        if (res.getStatus() == Response.Status.OK.getStatusCode()) {
            return Response.ok(res.getEntity()).build();
        } else {
            return Response.status(res.getStatus()).entity(res.getEntity()).build();
        }
    }



    public Response getMonthly_Amount_of_Year(String year) {
        String startDate = year + "-01-01 00:00:00";
        String endDate = year + "-12-31 23:59:59";

        Response res = getExpenseByDate(startDate, endDate);

        if (res.getStatus() == Response.Status.OK.getStatusCode()) {
            Iterable<Expense> expenses = (Iterable<Expense>) res.getEntity();

            HashMap<String, BigDecimal> monthlyAmount = new HashMap<>();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            for (Expense expense : expenses) {
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
}
