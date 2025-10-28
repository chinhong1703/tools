package com.example.demomodulithapp.ingestjob;

import com.example.demomodulithapp.ingestjob.entity.AggregatedOrderEntity;
import com.example.demomodulithapp.ingestjob.repo.AggregatedOrderRepository;
import com.example.demomodulithapp.ingestjob.service.OrderIngestJobRunner;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OrderIngestJobIntegrationTest {

    @Container
    static final MariaDBContainer<?> mariaDb = new MariaDBContainer<>("mariadb:11.3");

    private static final Path BASE_DIR = Paths.get(System.getProperty("java.io.tmpdir"),
            "orders-ingest-it-" + UUID.randomUUID());

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mariaDb::getJdbcUrl);
        registry.add("spring.datasource.username", mariaDb::getUsername);
        registry.add("spring.datasource.password", mariaDb::getPassword);
        registry.add("app.io.inputPattern", OrderIngestJobIntegrationTest::inputPathTemplate);
        registry.add("app.io.rejectsPattern", OrderIngestJobIntegrationTest::rejectsPathTemplate);
        registry.add("app.io.aggregatesPattern", OrderIngestJobIntegrationTest::aggregatesPathTemplate);
    }

    private static String inputPathTemplate() {
        return BASE_DIR.resolve("in/orders_{dataDate}.csv").toString();
    }

    private static String rejectsPathTemplate() {
        return BASE_DIR.resolve("out/{dataDate}/rejects.csv").toString();
    }

    private static String aggregatesPathTemplate() {
        return BASE_DIR.resolve("out/{dataDate}/aggregates.csv").toString();
    }

    @Autowired
    private OrderIngestJobRunner jobRunner;

    @Autowired
    private AggregatedOrderRepository repository;

    @BeforeEach
    void setup() throws IOException {
        Files.createDirectories(BASE_DIR.resolve("in"));
        repository.deleteAll();
    }

    @Test
    void jobProcessesCsvAndPersistsAggregates() throws Exception {
        LocalDate dataDate = LocalDate.of(2024, 1, 1);
        String csvContent = String.join("\n",
                "client,side,ticker,price,quantity,sourceSystem",
                "Acme,BUY,AAPL,150.50,100,colocated",
                "Acme,SELL,AAPL,151.00,50,colocated",
                "Acme,hold,AAPL,151.00,50,colocated",
                "Beta,BUY,GOOG,100.00,200,remote",
                "Beta,SELL,GOOG,101.00,300,colocated"
        );
        String inputFile = inputPathTemplate().replace("{dataDate}", dataDate.toString());
        Path input = Path.of(inputFile);
        Files.createDirectories(input.getParent());
        Files.writeString(input, csvContent, StandardCharsets.UTF_8);

        JobExecution execution = jobRunner.run(dataDate);
        while (execution.isRunning()) {
            Thread.sleep(100);
        }
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        Path rejects = Path.of(rejectsPathTemplate().replace("{dataDate}", dataDate.toString()));
        assertThat(Files.exists(rejects)).isTrue();
        try (CSVParser parser = CSVParser.parse(rejects, StandardCharsets.UTF_8,
                CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build())) {
            List<CSVRecord> records = parser.getRecords();
            assertThat(records).hasSize(1);
            assertThat(records.get(0).get("reason")).contains("side must be BUY or SELL");
        }

        Path aggregates = Path.of(aggregatesPathTemplate().replace("{dataDate}", dataDate.toString()));
        assertThat(Files.exists(aggregates)).isTrue();
        try (CSVParser parser = CSVParser.parse(aggregates, StandardCharsets.UTF_8,
                CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build())) {
            List<CSVRecord> records = parser.getRecords();
            assertThat(records).hasSize(3);
        }

        List<AggregatedOrderEntity> entities = repository.findAll();
        assertThat(entities).hasSize(3);
        AggregatedOrderEntity aaplBuy = entities.stream()
                .filter(e -> e.getClient().equals("Acme") && e.getSide().equals("BUY"))
                .findFirst()
                .orElseThrow();
        assertThat(aaplBuy.getTotalQuantity()).isEqualTo(100);
    }
}
