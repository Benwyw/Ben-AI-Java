package com.benwyw.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class FormatUtil {

    private static final Locale locale = Locale.ENGLISH;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd", locale);

    public static LocalDate convertStringToLocalDate(String dateString) {
        return LocalDate.parse(dateString, FORMATTER);
    }

    public static String getCurrentLocalDateStr() {
        return LocalDate.now().format(FORMATTER);
    }

}
