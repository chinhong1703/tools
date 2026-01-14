package com.example.csvgenerator;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class ExampleUsage {
    public static void main(String[] args) throws IOException {
        MetricsSink metricsSink = new NoopMetricsSink();
        StructuredLogger logger = new StructuredLogger();
        CsvGenerator generator = new CsvGenerator(metricsSink, logger);
        CsvGeneratorClient client = new CsvGeneratorClient(generator);

        Path outputRoot = Path.of("./csv-output");
        CsvGeneratorOptions generatorOptions = new CsvGeneratorOptions(
                outputRoot,
                true,
                5 * 1024 * 1024,
                4 * 1024 * 1024,
                25,
                Clock.systemUTC()
        );

        List<UserRecord> users = List.of(
                new UserRecord(1, "Ada Lovelace", "ada@example.com"),
                new UserRecord(2, "Grace Hopper", "grace@example.com"),
                new UserRecord(3, "Linus Torvalds", "linus@example.com")
        );

        CsvGenerationRequest<UserRecord> userRequest = new CsvGenerationRequest<>(CsvFileType.USERS, users);

        Iterator<OrderRecord> orderIterator = new StreamingOrderIterator(10_000);
        Iterable<OrderRecord> orderIterable = () -> orderIterator;
        CsvGenerationRequest<OrderRecord> orderRequest = new CsvGenerationRequest<>(CsvFileType.ORDERS, orderIterable);

        List<CsvGenerationRequest<?>> requests = new ArrayList<>();
        requests.add(userRequest);
        requests.add(orderRequest);

        CsvGeneratorClientOptions clientOptions = new CsvGeneratorClientOptions(2, 50);
        client.generateAll(requests, generatorOptions, clientOptions);
    }

    private static final class StreamingOrderIterator implements Iterator<OrderRecord> {
        private final int maxRows;
        private int current;

        private StreamingOrderIterator(int maxRows) {
            this.maxRows = maxRows;
        }

        @Override
        public boolean hasNext() {
            return current < maxRows;
        }

        @Override
        public OrderRecord next() {
            current++;
            return new OrderRecord(current, current % 1000, (current % 500) + 0.99);
        }
    }
}
