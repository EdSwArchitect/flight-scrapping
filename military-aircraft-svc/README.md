# Military Aircraft Service

Fetches military aircraft data from the ADS-B API and publishes to Kafka.

## Overview

- Fetches from https://api.adsb.lol/v2/mil
- Publishes JSON records to `military_flights` topic
- Exposes Prometheus metrics on port 8081
- Designed to run as a scheduled job (CronJob) or one-shot

## Build & Run

```bash
# From project root
mvn -pl military-aircraft-svc package
java -jar target/military-aircraft-svc-1.0.0-SNAPSHOT.jar
```

## Configuration

| Property | Default | Description |
|----------|---------|-------------|
| `adsb.api.url` | https://api.adsb.lol/v2/mil | ADS-B API endpoint |
| `kafka.bootstrap.servers` | localhost:9092 | Kafka brokers |
| `kafka.topic` | military_flights | Topic to produce to |
| `run.once` | true | Exit after one fetch (for CronJob) |

## Tests

Uses Testcontainers for Kafka. Run from project root:

```bash
./scripts/run-tests.sh -pl military-aircraft-svc
```
