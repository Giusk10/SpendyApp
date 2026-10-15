package com.spendyteam.expense.Utility;

import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;

public class H2FunctionAliases {

    public static String dateFormat(Timestamp date, String pattern) {
        if (date == null) {
            return null;
        }
        return DateTimeFormatter.ofPattern("yyyy-MM").format(date.toLocalDateTime());
    }
}
