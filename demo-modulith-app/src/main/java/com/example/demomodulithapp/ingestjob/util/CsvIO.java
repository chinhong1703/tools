package com.example.demomodulithapp.ingestjob.util;

import com.example.demomodulithapp.ingestjob.domain.AggregatedOrder;
import com.example.demomodulithapp.ingestjob.domain.OrderRecord;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

public final class CsvIO {

    public static final String[] ORDER_HEADERS = {"client", "side", "ticker", "price", "quantity", "sourceSystem"};

    private CsvIO() {
    }

    public static List<OrderRecord> readOrders(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader(ORDER_HEADERS)
                     .setSkipHeaderRecord(true)
                     .setIgnoreEmptyLines(true)
                     .setTrim(true)
                     .build()
                     .parse(reader)) {
            List<OrderRecord> records = new ArrayList<>();
            for (CSVRecord record : parser) {
                records.add(mapOrder(record.toMap()));
            }
            return records;
        }
    }

    private static OrderRecord mapOrder(Map<String, String> map) {
        BigDecimal price = new BigDecimal(map.getOrDefault("price", "0"));
        long quantity = Long.parseLong(map.getOrDefault("quantity", "0"));
        return new OrderRecord(
                map.getOrDefault("client", ""),
                map.getOrDefault("side", ""),
                map.getOrDefault("ticker", ""),
                price,
                quantity,
                map.getOrDefault("sourceSystem", "")
        );
    }

    public static void writeRejects(Path path, List<OrderRecord> invalidRecords, List<String> reasons) throws IOException {
        ensureParent(path);
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8);
             CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
                     .setHeader("client", "side", "ticker", "price", "quantity", "sourceSystem", "reason")
                     .build())) {
            for (int i = 0; i < invalidRecords.size(); i++) {
                OrderRecord record = invalidRecords.get(i);
                printer.printRecord(record.getClient(), record.getSide(), record.getTicker(),
                        record.getPrice(), record.getQuantity(), record.getSourceSystem(), reasons.get(i));
            }
        }
    }

    public static void writeAggregates(Path path, List<AggregatedOrder> aggregates) throws IOException {
        ensureParent(path);
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8);
             CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
                     .setHeader("dataDate", "client", "side", "ticker", "totalQuantity", "vwap")
                     .build())) {
            for (AggregatedOrder aggregate : aggregates) {
                printer.printRecord(
                        aggregate.getDataDate(),
                        aggregate.getClient(),
                        aggregate.getSide(),
                        aggregate.getTicker(),
                        aggregate.getTotalQuantity(),
                        aggregate.getVwap().setScale(8, java.math.RoundingMode.HALF_UP)
                );
            }
        }
    }

    private static void ensureParent(Path path) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }
}
