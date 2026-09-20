# BankService (`master` branch)

A learning project: a REST service for a bank account built on plain Java (no frameworks like Spring), with a thread-safe servlet and a PostgreSQL connection via JDBC.

## What's implemented

- REST API on top of `HttpServlet` (embedded Jetty)
- PostgreSQL connection via `DriverManager` (plain JDBC, no ORM)
- CRUD operations on a bank account: create, view, deposit, withdraw
- Balance-changing responses include the state **before** and **after** the operation
- Thread-safe deposit/withdraw operations via `synchronized`
- Docker + Docker Compose for local runs (app + PostgreSQL)
- Unit tests for the business logic (JUnit 5 + AssertJ), including a concurrency test

## Architecture

```
servlet/    → HTTP layer (AccountServlet): parses the request, calls the service, builds the response
service/    → business logic (AccountService / AccountServiceImpl)
repository/ → database access (AccountRepository / JdbcAccountRepository)
model/      → data model (Account)
dto/        → request/response objects (AmountRequest, BalanceChangeResponse)
exceptions/ → domain exceptions (AccountNotFoundException, InsufficientFoundsException)
config/     → database connection (DBConnection)
```

Layers are isolated from each other through interfaces (`AccountRepository`, `AccountService`), which allows swapping implementations (e.g. a fake repository in tests) without touching the calling code.

## Data model

The `accounts` table stores the balance directly:

```sql
CREATE TABLE accounts (
    id SERIAL PRIMARY KEY,
    owner_name VARCHAR(100) NOT NULL,
    balance NUMERIC(15,2) NOT NULL
);
```

## Running locally

### 1. Start the environment

```bash
docker compose up -d --build
```

Starts PostgreSQL and the application (port `8080`).

### 2. Environment variables

The application reads DB connection parameters from environment variables:

| Variable       | Example                                      |
|----------------|-----------------------------------------------|
| `DB_URL`       | `jdbc:postgresql://db:5432/bankdb`            |
| `DB_USER`      | `postgres`                                    |
| `DB_PASSWORD`  | `123`                                         |

When running via Docker Compose, these are already set in `docker-compose.yml`.

## REST API

| Method | Path                      | Description             |
|--------|---------------------------|--------------------------|
| GET    | `/accounts/{id}`          | Get an account           |
| POST   | `/accounts`                | Create an account        |
| PUT    | `/accounts/{id}/deposit`  | Deposit into an account  |
| PUT    | `/accounts/{id}/withdraw` | Withdraw from an account |

### Example requests

```bash
curl -X GET http://localhost:8080/accounts/1

curl -X POST http://localhost:8080/accounts \
  -H "Content-Type: application/json" \
  -d '{"owner_name": "Ivan", "balance": 1000.00}'

curl -X PUT http://localhost:8080/accounts/1/deposit \
  -H "Content-Type: application/json" \
  -d '{"amount": 100}'

curl -X PUT http://localhost:8080/accounts/1/withdraw \
  -H "Content-Type: application/json" \
  -d '{"amount": 50}'
```

Deposit/withdraw response:

```json
{"account_id": 1, "balanceBefore": 1000.00, "balanceAfter": 1100.00}
```

## Thread safety

The `deposit` and `withdraw` methods in `AccountServiceImpl` are marked `synchronized`, which rules out race conditions when concurrent requests hit the same account. This is verified by a unit test that runs multiple threads in parallel and checks the final balance.

## Tests

```bash
mvn test
```

Tests use a fake implementation of `AccountRepository` (no real database involved), which keeps them fast and independent of the environment.

## CI

The repository is set up to automatically build and run tests via GitHub Actions on every push (`.github/workflows/ci.yml`).
