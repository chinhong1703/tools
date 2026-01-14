package com.example.csvgenerator;

@FunctionalInterface
public interface CsvRowMapper<T> {
    String[] mapRow(T row);
}
