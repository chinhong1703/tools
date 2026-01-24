package com.example.csvparser.core.conversion;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public class DateConverter {
    private final List<DateTimeFormatter> formatters;

    public DateConverter(List<String> patterns) {
        this.formatters = patterns.stream()
                .map(DateTimeFormatter::ofPattern)
                .toList();
    }

    public LocalDate toLocalDate(String rawValue) throws ConversionException {
        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(rawValue, formatter);
            } catch (DateTimeParseException ignored) {
                // try next
            }
        }
        throw new ConversionException("Invalid date value: " + rawValue);
    }

    public LocalDateTime toLocalDateTime(String rawValue) throws ConversionException {
        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDateTime.parse(rawValue, formatter);
            } catch (DateTimeParseException ignored) {
                // try next
            }
        }
        throw new ConversionException("Invalid date-time value: " + rawValue);
    }
}
