# Aircraft DB Ingestor

Consumes military flight records from Kafka and inserts them into PostgreSQL in batches.

## Overview

- Subscribes to `military_flights` topic
- Maps JSON to `FlightRecord`, batches inserts
- Exposes Prometheus metrics on port 8082

## Build & Run

```bash
# From project root
mvn -pl aircraft-db-ingestor package
java -jar target/aircraft-db-ingestor-1.0.0-SNAPSHOT.jar
```

## Configuration

| Property | Default | Description |
|----------|---------|-------------|
| `kafka.bootstrap.servers` | localhost:9092 | Kafka brokers |
| `kafka.topic` | military_flights | Topic to consume |
| `db.write.url` | jdbc:postgresql://localhost:5432/military_watcher | JDBC URL |
| `db.write.username` | military | DB user |
| `db.write.password` | military | DB password |
| `ingestor.batch.size` | 100 | Records per batch |

## Tests

Uses Testcontainers for Kafka and PostgreSQL. Run from project root:

```bash
./scripts/run-tests.sh -pl aircraft-db-ingestor
```

## JDBC vs ORM Tradeoff

**JDBC (chosen):**
- Lower overhead and explicit control over SQL
- Faster for bulk/batch inserts
- No N+1 query issues
- Simpler for high-volume ingestion
- Direct PreparedStatement batching

**ORM (e.g., Hibernate):**
- Reduces boilerplate for CRUD
- Built-in migrations (Flyway/Liquibase)
- Relationship mapping
- Adds latency and complexity for high-volume ingestion
- Batch inserts require careful configuration

For this use case (Kafka consumer with batch inserts), JDBC is recommended.
