package com.spendyteam.expense.Utility;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;

/**
 * Utility component responsible for parsing raw string values into domain types
 * (LocalDateTime and BigDecimal), supporting multiple formats, locales, and currency conventions.
 */
@Component
public class ExpenseDataParser {

    private static final List<String> DATE_PATTERNS = Arrays.asList(
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd",
            "dd/MM/yyyy",
            "dd/MM/yyyy HH:mm:ss"
    );

    /**
     * Parses a string representation of a date/time into a {@link LocalDateTime}.
     * Supports ISO strings, ZonedDateTime, common date/datetime formats, and epoch timestamps (seconds or milliseconds).
     *
     * @param value raw date/time string
     * @return parsed {@link LocalDateTime}, or null if input is null or blank
     */
    public LocalDateTime parseToLocalDateTime(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String v = value.trim();

        // 1. Try ISO ZonedDateTime
        try {
            return java.time.ZonedDateTime.parse(v).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
        }

        // 2. Try common date and datetime patterns
        for (String p : DATE_PATTERNS) {
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

        // 3. Try epoch timestamp (seconds or milliseconds)
        try {
            long num = Long.parseLong(v);
            if (v.length() >= 13) {
                return LocalDateTime.ofEpochSecond(num / 1000, 0, ZoneOffset.UTC);
            } else {
                return LocalDateTime.ofEpochSecond(num, 0, ZoneOffset.UTC);
            }
        } catch (NumberFormatException ignored) {
        }

        // 4. Fallback to standard LocalDateTime.parse
        return LocalDateTime.parse(v);
    }

    /**
     * Parses a string representation of an amount into a {@link BigDecimal}.
     * Handles currency symbols (€, $, £, ¥), non-breaking spaces, and European/American thousand and decimal separators.
     *
     * @param value raw numeric string
     * @return parsed {@link BigDecimal}, or {@link BigDecimal#ZERO} if input is null, blank, or resolves to empty
     */
    public BigDecimal parseBigDecimal(String value) {
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
        if (v.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(v);
    }
}
