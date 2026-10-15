package com.spendyteam.expense.Utility;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ExpenseDataParserTest {

    private ExpenseDataParser parser;

    @BeforeEach
    void setUp() {
        parser = new ExpenseDataParser();
    }

    @Test
    void testParseToLocalDateTimeNullOrBlank() {
        assertNull(parser.parseToLocalDateTime(null));
        assertNull(parser.parseToLocalDateTime(""));
        assertNull(parser.parseToLocalDateTime("   "));
    }

    @Test
    void testParseToLocalDateTimeZonedDateTime() {
        LocalDateTime dt = parser.parseToLocalDateTime("2024-05-10T14:30:00+02:00");
        assertNotNull(dt);
        assertEquals(2024, dt.getYear());
        assertEquals(5, dt.getMonthValue());
        assertEquals(10, dt.getDayOfMonth());
        assertEquals(14, dt.getHour());
        assertEquals(30, dt.getMinute());
    }

    @Test
    void testParseToLocalDateTimeCommonPatterns() {
        // yyyy-MM-dd HH:mm:ss
        LocalDateTime dt1 = parser.parseToLocalDateTime("2024-01-15 10:30:45");
        assertNotNull(dt1);
        assertEquals(LocalDateTime.of(2024, 1, 15, 10, 30, 45), dt1);

        // yyyy-MM-dd'T'HH:mm:ss
        LocalDateTime dt2 = parser.parseToLocalDateTime("2024-01-15T10:30:45");
        assertNotNull(dt2);
        assertEquals(LocalDateTime.of(2024, 1, 15, 10, 30, 45), dt2);

        // yyyy-MM-dd
        LocalDateTime dt3 = parser.parseToLocalDateTime("2024-01-15");
        assertNotNull(dt3);
        assertEquals(LocalDateTime.of(2024, 1, 15, 0, 0, 0), dt3);

        // dd/MM/yyyy
        LocalDateTime dt4 = parser.parseToLocalDateTime("15/01/2024");
        assertNotNull(dt4);
        assertEquals(LocalDateTime.of(2024, 1, 15, 0, 0, 0), dt4);

        // dd/MM/yyyy HH:mm:ss
        LocalDateTime dt5 = parser.parseToLocalDateTime("15/01/2024 18:45:00");
        assertNotNull(dt5);
        assertEquals(LocalDateTime.of(2024, 1, 15, 18, 45, 0), dt5);
    }

    @Test
    void testParseToLocalDateTimeEpochSecondsAndMillis() {
        // Epoch seconds: 1705315200 -> 2024-01-15 10:40:00 UTC
        LocalDateTime dtSeconds = parser.parseToLocalDateTime("1705315200");
        assertNotNull(dtSeconds);

        // Epoch millis: 1705315200000 (length >= 13)
        LocalDateTime dtMillis = parser.parseToLocalDateTime("1705315200000");
        assertNotNull(dtMillis);
        assertEquals(dtSeconds, dtMillis);
    }

    @Test
    void testParseBigDecimalNullOrEmpty() {
        assertEquals(BigDecimal.ZERO, parser.parseBigDecimal(null));
        assertEquals(BigDecimal.ZERO, parser.parseBigDecimal(""));
        assertEquals(BigDecimal.ZERO, parser.parseBigDecimal("   "));
        assertEquals(BigDecimal.ZERO, parser.parseBigDecimal("abc"));
    }

    @Test
    void testParseBigDecimalCurrencyAndFormats() {
        // Simple decimal
        assertEquals(new BigDecimal("15.99"), parser.parseBigDecimal("15.99"));

        // Comma as decimal separator
        assertEquals(new BigDecimal("15.99"), parser.parseBigDecimal("15,99"));

        // European thousand separator: 1.250,50 €
        assertEquals(new BigDecimal("1250.50"), parser.parseBigDecimal("1.250,50 €"));

        // American thousand separator: $1,250.50
        assertEquals(new BigDecimal("1250.50"), parser.parseBigDecimal("$1,250.50"));

        // Negative values
        assertEquals(new BigDecimal("-45.20"), parser.parseBigDecimal("-45,20 €"));

        // Currency symbols and non-breaking spaces
        assertEquals(new BigDecimal("99.90"), parser.parseBigDecimal("£\u00A099.90"));
        assertEquals(new BigDecimal("5000"), parser.parseBigDecimal("¥ 5000"));
    }
}
