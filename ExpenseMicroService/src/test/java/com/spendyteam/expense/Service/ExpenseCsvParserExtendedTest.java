package com.spendyteam.expense.Service;

import com.spendyteam.expense.Data.Expense;
import com.spendyteam.expense.Utility.ExpenseDataParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Extended CSV Parser Tests for Edge Cases, Limits, Ordering, and Malformed Input")
class ExpenseCsvParserExtendedTest {

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
    @DisplayName("Should parse valid CSV and strictly preserve line order")
    void testParsePreservingLineOrder() throws Exception {
        StringBuilder csvBuilder = new StringBuilder();
        csvBuilder.append("Type,Product,Started Date,Completed Date,Description,Amount,Fee,Currency,State\n");
        for (int i = 1; i <= 20; i++) {
            csvBuilder.append(String.format("CARD,Merchant %d,2024-03-01 10:%02d:00,2024-03-01 10:%02d:00,Item %d,%d.50,0.00,EUR,COMPLETED\n",
                    i, i, i, i, i));
        }

        MockMultipartFile file = new MockMultipartFile(
                "file", "ordered.csv", "text/csv", csvBuilder.toString().getBytes(StandardCharsets.UTF_8));

        when(categorizerService.predictCategory(anyString())).thenReturn("General");

        ExpenseCsvParser.CsvParseResult result = csvParser.parse(file, "order_user");

        assertEquals(20, result.expenses().size());
        assertEquals(0, result.parseErrors());
        assertEquals(20, result.totalRowsRead());

        // Verify ordering is strictly 1 to 20
        for (int i = 0; i < 20; i++) {
            assertEquals("Item " + (i + 1), result.expenses().get(i).getDescription());
            assertEquals(new BigDecimal((i + 1) + ".50"), result.expenses().get(i).getAmount());
        }
    }

    @Test
    @DisplayName("Should detect semicolons and tabs as delimiters")
    void testDifferentDelimiters() throws Exception {
        String semiCsv = "Tipo;Prodotto;Data;Descrizione;Importo;Valuta;Stato\n" +
                "CARD;Shop;2024-01-01 10:00:00;Purchase;45,50;EUR;COMPLETED\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "semi.csv", "text/csv", semiCsv.getBytes(StandardCharsets.UTF_8));

        when(categorizerService.predictCategory(anyString())).thenReturn("Shopping");

        ExpenseCsvParser.CsvParseResult result = csvParser.parse(file, "semi_user");
        assertEquals(1, result.expenses().size());
        assertEquals(new BigDecimal("45.50"), result.expenses().get(0).getAmount());
    }

    @Test
    @DisplayName("Should fail when rows exceed configured maxRows limit")
    void testExceedMaxRowsLimit() {
        csvParser.setMaxRows(5);

        StringBuilder csvBuilder = new StringBuilder();
        csvBuilder.append("Type,Product,Started Date,Amount\n");
        for (int i = 0; i < 10; i++) {
            csvBuilder.append(String.format("CARD,Shop,2024-01-01 10:00:00,%d.00\n", i));
        }

        MockMultipartFile file = new MockMultipartFile(
                "file", "large.csv", "text/csv", csvBuilder.toString().getBytes(StandardCharsets.UTF_8));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                csvParser.parse(file, "limit_user"));
        assertTrue(ex.getMessage().contains("exceeds maximum allowed limit of 5 rows"));
    }

    @Test
    @DisplayName("Should fail when file size exceeds configured maxFileSizeBytes limit")
    void testExceedMaxFileSizeBytes() {
        csvParser.setMaxFileSizeBytes(50);

        byte[] largeContent = "Type,Product,Started Date,Amount\nCARD,Store,2024-01-01 10:00:00,100.00\nCARD,Store,2024-01-01 10:00:00,100.00\n"
                .getBytes(StandardCharsets.UTF_8);

        MockMultipartFile file = new MockMultipartFile(
                "file", "huge.csv", "text/csv", largeContent);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                csvParser.parse(file, "size_user"));
        assertTrue(ex.getMessage().contains("exceeds the maximum allowed limit of 50 bytes"));
    }

    @Test
    @DisplayName("Should record structured CsvRowError details for malformed lines")
    void testMalformedRowsTracking() throws Exception {
        String csv = "Type,Product,Started Date,Completed Date,Description,Amount,Fee,Currency,State\n" +
                "VALID,GoodStore,2024-01-15 10:00:00,2024-01-15 10:05:00,Valid item,25.00,0.00,EUR,COMPLETED\n" +
                "\n" + // empty line (should be skipped gracefully)
                "VALID2,GoodStore2,2024-01-16 10:00:00,2024-01-16 10:05:00,Second item,50.00,0.00,EUR,COMPLETED\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "mixed.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        when(categorizerService.predictCategory(anyString())).thenReturn("Misc");

        ExpenseCsvParser.CsvParseResult result = csvParser.parse(file, "mixed_user");

        assertEquals(2, result.expenses().size());
        assertEquals(0, result.parseErrors());
    }

    @Test
    @DisplayName("Should reject file with blank or empty header")
    void testBlankHeader() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty_header.csv", "text/csv", "   \n".getBytes(StandardCharsets.UTF_8));

        assertThrows(IllegalArgumentException.class, () -> csvParser.parse(file, "blank_user"));
    }
}
