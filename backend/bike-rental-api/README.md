# UGE Bike Rental API

A minimal CRUD REST API for **Bike** entities (fields: `id`, `description`) built with:

- **Jersey (JAX-RS)** for REST
- **Hibernate (JPA)** for persistence
- **MySQL** for database
- **Tomcat** as the application server
- **Docker + Docker Compose** for running MySQL and Tomcat
- **Maven (WAR)** build
- **JUnit 5** (+ Testcontainers for integration tests)

---

## Prerequisites

Install the following on your machine:

- **JDK 21** (or the Java version configured in `pom.xml`)
- **Maven 3.9+**
- **Docker Engine**
- **Docker Compose** (v2, usually included with Docker Desktop)
- **curl**
- (Optional) **Git**

> Note: Tests use **Testcontainers**, so Docker must be running to execute `mvn test`.

---

## Project Overview

- REST base path: `http://localhost:8080/api`
- Bikes resource: `http://localhost:8080/api/bikes`
- MySQL exposed on: `localhost:3306`
- Application exposed on: `localhost:8080`

---

## Configuration

The application reads database settings from environment variables (recommended for Docker):

- `DB_URL`
- `DB_USER`
- `DB_PASSWORD`

These are already provided in `docker-compose.yml`.

---

## Build the WAR locally (optional)

This builds `target/uge-bike-api.war`:

```bash
mvn clean package
```

# Run with Docker Compose (recommended)

From the project root (where docker-compose.yml is located):

```bash
docker compose up --build
```

## Stop and remove containers:

```bash
docker compose down
```
## If you want to remove the DB volume too (THIS DELETES DATA):
```bash
docker compose down -v
```
## Rebuild and restart:
```bash
docker compose down
docker compose up --build --force-recreate
```
## Run Tests (JUnit + Testcontainers)

Docker must be running.
```bash
mvn test
```



