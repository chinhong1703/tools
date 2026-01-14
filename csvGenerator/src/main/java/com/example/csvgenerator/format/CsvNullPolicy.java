package com.example.csvgenerator;

public enum CsvNullPolicy {
    EMPTY_STRING(""),
    LITERAL_NULL("null");

    private final String nullValue;

    CsvNullPolicy(String nullValue) {
        this.nullValue = nullValue;
    }

    public String nullValue() {
        return nullValue;
    }
}
