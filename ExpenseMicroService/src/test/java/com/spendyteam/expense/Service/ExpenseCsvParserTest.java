package com.spendyteam.expense.Service;

import com.spendyteam.expense.Data.Expense;
import com.spendyteam.expense.Utility.ExpenseDataParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpenseCsvParserTest {

    @Mock
    private SmartCategorizerService categorizerService;

    private ExpenseDataParser dataParser;
    private ExpenseCsvParser csvParser;

    @BeforeEach
    void setUp() {
        dataParser = new ExpenseDataParser();
        csvParser = new ExpenseCsvParser(dataParser, categorizerService);
    }

    @Test
    void testDetectSeparator() {
        assertEquals(',', csvParser.detectSeparator("Type,Product,Started Date,Amount"));
        assertEquals(';', csvParser.detectSeparator("Type;Product;Started Date;Amount"));
        assertEquals('\t', csvParser.detectSeparator("Type\tProduct\tStarted Date\tAmount"));
    }

    @Test
    void testMapHeaderToIndex() {
        String[] header = {"Tipo", "Prodotto", "Data", "Descrizione", "Importo", "Valuta", "Stato"};
        Map<String, Integer> map = csvParser.mapHeaderToIndex(header);

        assertEquals(0, map.get("type"));
        assertEquals(1, map.get("product"));
        assertEquals(2, map.get("startedDate"));
        assertEquals(3, map.get("description"));
        assertEquals(4, map.get("amount"));
        assertEquals(5, map.get("currency"));
        assertEquals(6, map.get("state"));
    }

    @Test
    void testParseLineToExpense() {
        String[] line = {"CARD_PAYMENT", "Supermarket", "2024-03-01 12:00:00", "2024-03-01 12:05:00", "Lidl grocery", "35.50", "0.00", "EUR", "COMPLETED"};
        Map<String, Integer> idx = Map.of(
                "type", 0,
                "product", 1,
                "startedDate", 2,
                "completedDate", 3,
                "description", 4,
                "amount", 5,
                "fee", 6,
                "currency", 7,
                "state", 8
        );

        when(categorizerService.predictCategory(anyString())).thenReturn("Supermercati e Alimentari");

        Expense expense = csvParser.parseLineToExpense(line, idx, "testuser", categorizerService);

        assertNotNull(expense);
        assertEquals("CARD_PAYMENT", expense.getType());
        assertEquals("Supermarket", expense.getProduct());
        assertEquals(LocalDateTime.of(2024, 3, 1, 12, 0, 0), expense.getStartedDate());
        assertEquals(LocalDateTime.of(2024, 3, 1, 12, 5, 0), expense.getCompletedDate());
        assertEquals("Lidl grocery", expense.getDescription());
        assertEquals(new BigDecimal("35.50"), expense.getAmount());
        assertEquals(0, expense.getFee().compareTo(BigDecimal.ZERO));
        assertEquals("EUR", expense.getCurrency());
        assertEquals("COMPLETED", expense.getState());
        assertEquals("Supermercati e Alimentari", expense.getCategory());
        assertEquals("testuser", expense.getUsername());
    }

    @Test
    void testParseCsvValidContent() throws Exception {
        String csv = "Type,Product,Started Date,Completed Date,Description,Amount,Fee,Currency,State\n" +
                     "Payment,Netflix,2024-01-15 10:00:00,2024-01-15 10:05:00,Netflix subscription,15.99,0.00,EUR,COMPLETED\n" +
                     "Payment,Spotify,2024-01-16 09:00:00,2024-01-16 09:01:00,Spotify Premium,9.99,0.00,EUR,COMPLETED\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "expenses.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        when(categorizerService.predictCategory(anyString())).thenReturn("Abbonamenti e Servizi Digitali");

        ExpenseCsvParser.CsvParseResult result = csvParser.parse(file, "testuser");

        assertEquals(2, result.expenses().size());
        assertEquals(0, result.parseErrors());
        assertEquals("testuser", result.expenses().get(0).getUsername());
    }

    @Test
    void testParseCsvWithCorruptedLines() throws Exception {
        String csv = "Type,Product,Started Date,Completed Date,Description,Amount,Fee,Currency,State\n" +
                     "Payment,Netflix,2024-01-15 10:00:00,2024-01-15 10:05:00,Netflix subscription,15.99,0.00,EUR,COMPLETED\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "expenses.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        ExpenseCsvParser.CsvParseResult result = csvParser.parse(file, "testuser");

        assertEquals(1, result.expenses().size());
    }

    @Test
    void testParseCsvMissingHeaderThrowsException() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "empty.csv", "text/csv", new byte[0]);

        assertThrows(IllegalArgumentException.class, () -> csvParser.parse(emptyFile, "testuser"));
    }
}
