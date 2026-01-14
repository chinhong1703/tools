package com.example.csvgenerator;

import java.util.Objects;

public enum CsvFileType {
    USERS(
            "users",
            new String[]{"id", "name", "email"},
            (CsvRowMapper<UserRecord>) user -> new String[]{
                    String.valueOf(user.id()),
                    user.name(),
                    user.email()
            },
            CsvFormatSettings.defaultSettings(),
            OutputNamingConfig.defaultConfig("users", "users")
    ),
    ORDERS(
            "orders",
            new String[]{"id", "user_id", "total"},
            (CsvRowMapper<OrderRecord>) order -> new String[]{
                    String.valueOf(order.id()),
                    String.valueOf(order.userId()),
                    String.valueOf(order.total())
            },
            new CsvFormatSettings(
                    ';',
                    '"',
                    "\n",
                    CsvFormatSettings.defaultSettings().charset(),
                    CsvQuotingPolicy.MINIMAL,
                    CsvNullPolicy.EMPTY_STRING
            ),
            new OutputNamingConfig("orders", "orders", "", "", true, "yyyyMMddHHmmss")
    );

    private final String typeName;
    private final String[] headers;
    private final CsvRowMapper<?> rowMapper;
    private final CsvFormatSettings formatSettings;
    private final OutputNamingConfig namingConfig;

    CsvFileType(String typeName,
                String[] headers,
                CsvRowMapper<?> rowMapper,
                CsvFormatSettings formatSettings,
                OutputNamingConfig namingConfig) {
        this.typeName = Objects.requireNonNull(typeName, "typeName");
        this.headers = Objects.requireNonNull(headers, "headers");
        this.rowMapper = Objects.requireNonNull(rowMapper, "rowMapper");
        this.formatSettings = Objects.requireNonNull(formatSettings, "formatSettings");
        this.namingConfig = Objects.requireNonNull(namingConfig, "namingConfig");
    }

    public String typeName() {
        return typeName;
    }

    public String[] headers() {
        return headers;
    }

    @SuppressWarnings("unchecked")
    public <T> CsvRowMapper<T> rowMapper() {
        return (CsvRowMapper<T>) rowMapper;
    }

    public CsvFormatSettings formatSettings() {
        return formatSettings;
    }

    public OutputNamingConfig namingConfig() {
        return namingConfig;
    }
}
