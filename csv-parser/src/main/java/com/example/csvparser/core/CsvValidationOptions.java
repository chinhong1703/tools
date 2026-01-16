package com.example.csvparser.core;

public record CsvValidationOptions(
        boolean beanValidationEnabled
) {
    public static CsvValidationOptions defaults(boolean validatorAvailable) {
        return new CsvValidationOptions(validatorAvailable);
    }
}
