# Military Aircraft Watcher

System for tracking military aircraft data from ADS-B API, ingesting via Kafka, and exposing through a REST API and React UI.

## Architecture

- **military-aircraft-svc**: Fetches from https://api.adsb.lol/v2/mil, publishes to Kafka (scheduled/CronJob)
- **aircraft-db-ingestor**: Consumes from Kafka, batch inserts into PostgreSQL
- **military-watcher-api**: REST API (Armeria/Netty) reading from PostgreSQL
- **ui**: React app with list flights, flight detail, geobox search

## Project Structure

Multi-module Maven project:

```
├── pom.xml                    # Parent POM
├── military-aircraft-svc/     # ADS-B fetcher → Kafka
├── aircraft-db-ingestor/     # Kafka → PostgreSQL
├── military-watcher-api/      # REST API
├── ui/                        # React frontend
├── db/init/                   # PostgreSQL schema
├── grafana/dashboards/        # Grafana dashboards
├── helm/                      # Kubernetes Helm charts
└── scripts/                   # Certificate & test helpers
```

## Quick Start (Docker Compose)

```bash
# Start infrastructure (Postgres, Kafka, Prometheus, Grafana)
docker compose up -d

# Build and run application services
docker compose --profile app build
docker compose --profile app up -d

# Trigger a fetch (run military-aircraft-svc once)
docker compose --profile app run --rm military-aircraft-svc
```

- **API**: http://localhost:8080
- **UI**: http://localhost:5173
- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000 (admin/admin)

## Build

```bash
# Build all Java modules from root
mvn clean package

# Build single module
mvn -pl military-watcher-api package

# Skip tests
mvn clean package -DskipTests
```

## API Endpoints

- `GET /list-flights?page=0&size=50` - List all flights (paginated)
- `GET /list-flights/{id}` - Get flight by hex or id
- `POST /geobox-list-flight` - Body: `{"minLat":30,"maxLat":40,"minLon":-120,"maxLon":-100}`

## Grafana Dashboards

Pre-provisioned dashboards (in **Military Watcher** folder):

- **Kafka** - Consumer rate, producer estimate, topic metrics
- **REST API** - Endpoint success/failure, call rate
- **Military Aircraft Service** - ADS-B API calls, duration, items per response
- **Aircraft DB Ingestor** - Batch insert rate, duration

## Running Tests

Integration tests use Testcontainers for Kafka and PostgreSQL. **Docker must be running.**

```bash
# Recommended: use helper script
./scripts/run-tests.sh

# Or set DOCKER_HOST manually (Docker Desktop 4.x on macOS)
export DOCKER_HOST=unix://$HOME/.docker/run/docker.sock
mvn test
```

### Docker Desktop 4.x on macOS

If you see `Could not find a valid Docker environment` or `BadRequestException (Status 400)`:

1. Use `./scripts/run-tests.sh` or set `DOCKER_HOST=unix://$HOME/.docker/run/docker.sock`
2. Create symlink if needed: `sudo ln -sf $HOME/.docker/run/docker.sock /var/run/docker.sock`
3. Docker Desktop → Settings → Advanced → enable "Allow the default Docker socket to be used"
4. This project uses Testcontainers 1.21.4+ (Docker API 1.44 compatible)

## Local Development

```bash
# Java services (require Postgres + Kafka running, e.g. docker compose up -d)
mvn -pl military-aircraft-svc package && java -jar military-aircraft-svc/target/military-aircraft-svc-1.0.0-SNAPSHOT.jar
mvn -pl aircraft-db-ingestor package && java -jar aircraft-db-ingestor/target/aircraft-db-ingestor-1.0.0-SNAPSHOT.jar
mvn -pl military-watcher-api package && java -jar military-watcher-api/target/military-watcher-api-1.0.0-SNAPSHOT.jar

# UI
cd ui && npm run dev
```

## Certificates (SASL/TLS)

```bash
./scripts/gen-certs.sh      # JKS and PEM
./scripts/gen-certs-pem.sh  # PEM only
```

## Helm

```bash
helm install military-watcher ./helm/military-watcher
```

Requires Kafka and PostgreSQL. For full stack, use Strimzi and Bitnami Postgres charts.

## Skaffold

```bash
skaffold dev
```
