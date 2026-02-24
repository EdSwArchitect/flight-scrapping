# Military Watcher REST API

REST API for querying military flight data from PostgreSQL. Built with Armeria (Netty).

## Overview

- Reads from PostgreSQL (supports read replica)
- Exposes Prometheus metrics at `/metrics` on port 8080

## Build & Run

```bash
# From project root
mvn -pl military-watcher-api package
java -jar target/military-watcher-api-1.0.0-SNAPSHOT.jar
```

## Configuration

| Property | Default | Description |
|----------|---------|-------------|
| `server.port` | 8080 | HTTP port |
| `db.read.url` | jdbc:postgresql://localhost:5432/military_watcher | JDBC URL |
| `db.read.username` | military | DB user |
| `db.read.password` | military | DB password |

## Endpoints

- `GET /list-flights?page=0&size=50` - List flights (paginated)
- `GET /list-flights/{id}` - Get flight by hex or id
- `POST /geobox-list-flight` - Body: `{"minLat":30,"maxLat":40,"minLon":-120,"maxLon":-100}`
- `GET /metrics` - Prometheus metrics

## Tests

Uses Testcontainers for PostgreSQL. Run from project root:

```bash
./scripts/run-tests.sh -pl military-watcher-api
```
