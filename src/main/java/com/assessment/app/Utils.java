package com.assessment.app;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class Utils {
    public static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    public static boolean validDateRange(String start, String end) {
        try {
            LocalDate s = LocalDate.parse(start);
            LocalDate e = LocalDate.parse(end);
            return !e.isBefore(s);
        } catch (DateTimeParseException ex) {
            return false;
        }
    }
}
