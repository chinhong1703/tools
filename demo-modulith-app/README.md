# demomodulithapp

Spring Modulith sample application that ingests daily order CSV files, produces aggregates, persists them to MariaDB, and exposes admin APIs to manage the ingestion job.

## Requirements

- Java 17
- MariaDB 11+
- Maven 3.9+

## Running locally

1. Configure a MariaDB database and update `spring.datasource` values in `application.yml` or override via environment variables.
2. Ensure input CSV files follow the pattern `/data/in/orders_YYYY-MM-DD.csv` (configurable via `app.io.*` properties).
3. Build and run:

```bash
./mvnw spring-boot:run
```

The ingestion job runs daily at 8 PM Asia/Singapore and can be triggered manually:

```bash
curl -u admin:admin123 -X POST "http://localhost:8080/admin/jobs/orderIngest/run?dataDate=2024-01-01"
```

List recent executions:

```bash
curl -u admin:admin123 "http://localhost:8080/admin/jobs/orderIngest/executions?limit=10"
```

## Sample CSV

```
client,side,ticker,price,quantity,sourceSystem
Acme,BUY,AAPL,150.50,100,colocated
Acme,SELL,AAPL,151.00,50,colocated
Beta,SELL,GOOG,101.00,300,colocated
```

## Testing

```bash
./mvnw test
```

Integration tests run against Testcontainers MariaDB and verify CSV output and database persistence.

## Modulith documentation

Run `./mvnw -Pdocumentation verify` or check the generated docs under `target/spring-modulith-docs` after tests execute.

## Docker

Build a container image:

```bash
./mvnw -DskipTests package
docker build -t demomodulithapp:latest .
```

Run:

```bash
docker run -e SPRING_DATASOURCE_URL=jdbc:mariadb://host.docker.internal:3306/orders \
  -e SPRING_DATASOURCE_USERNAME=orders_user \
  -e SPRING_DATASOURCE_PASSWORD=orders_pass \
  -p 8080:8080 demomodulithapp:latest
```
