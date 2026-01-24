package com.example.csvparser.core.conversion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConverterRegistry {
    private final Map<Class<?>, Converter<?>> converters = new HashMap<>();

    public ConverterRegistry() {
        converters.put(String.class, rawValue -> rawValue);
        converters.put(Integer.class, rawValue -> Integer.valueOf(rawValue));
        converters.put(int.class, rawValue -> Integer.parseInt(rawValue));
        converters.put(Long.class, rawValue -> Long.valueOf(rawValue));
        converters.put(long.class, rawValue -> Long.parseLong(rawValue));
        converters.put(Double.class, rawValue -> Double.valueOf(rawValue));
        converters.put(double.class, rawValue -> Double.parseDouble(rawValue));
        converters.put(BigDecimal.class, BigDecimal::new);
        converters.put(Boolean.class, rawValue -> parseBoolean(rawValue));
        converters.put(boolean.class, rawValue -> parseBoolean(rawValue));
    }

    public Converter<?> getConverter(Class<?> targetType, List<String> datePatterns) {
        if (targetType.isEnum()) {
            return rawValue -> Enum.valueOf((Class<Enum>) targetType, rawValue);
        }
        if (targetType.equals(LocalDate.class)) {
            DateConverter converter = new DateConverter(datePatterns);
            return converter::toLocalDate;
        }
        if (targetType.equals(LocalDateTime.class)) {
            DateConverter converter = new DateConverter(datePatterns);
            return converter::toLocalDateTime;
        }
        Converter<?> converter = converters.get(targetType);
        if (converter == null) {
            throw new IllegalArgumentException("Unsupported field type: " + targetType.getName());
        }
        return converter;
    }

    private static Boolean parseBoolean(String rawValue) throws ConversionException {
        if ("true".equals(rawValue) || "false".equals(rawValue)) {
            return Boolean.valueOf(rawValue);
        }
        throw new ConversionException("Invalid boolean value: " + rawValue);
    }
}
