package com.example.csvparser.core.conversion;

public interface Converter<T> {
    T convert(String rawValue) throws ConversionException;
}
