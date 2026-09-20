# BankService (`double-ledger` branch)

An evolution of the base version (`master`): instead of storing the balance as a single field on the `accounts` table, the balance is computed from a journal of postings (**double-entry ledger**), the same way traditional bookkeeping works. A transfer operation between accounts has also been added.

## How this differs from `master`

| | master | double-ledger |
|---|---|---|
| Balance storage | `balance` field on `accounts`, updated directly | Computed as the sum of entries in `ledger_entries` |
| Operation history | Not stored | Every operation is a journal entry; nothing is ever overwritten |
| Transfers between accounts | Not implemented | Implemented as a single atomic operation with two linked entries |

## The double-entry principle

Every operation results in one or more rows in the `ledger_entries` table. A withdrawal (`DEBIT`) and a deposit (`CREDIT`) are never stored as a ready-made number on the account — the balance is always recalculated on the fly:

```
balance(account) = SUM(CREDIT) - SUM(DEBIT) across all entries for that account
```

A transfer between two accounts creates **two** entries sharing the same `transaction_id`: a `DEBIT` for the sender and a `CREDIT` for the recipient — this guarantees the operation is either fully reflected or not reflected at all.

## Architecture

```
servlet/    → AccountServlet — HTTP layer, structurally unchanged
service/    → AccountServiceImpl — computes the balance via LedgerRepository, creates a LedgerEntry instead of an UPDATE
repository/ → AccountRepository (account owner data) + LedgerRepository (operations journal)
model/      → Account, LedgerEntry, EntryType (DEBIT/CREDIT)
dto/        → AmountRequest, BalanceChangeResponse, TransferRequest, TransferResponse
exceptions/ → AccountNotFoundException, InsufficientFoundsException
```

`AccountServlet` did not need to change when the ledger architecture was introduced — a demonstration of the **Open/Closed** principle: how the balance is calculated changed completely, but the HTTP layer, which talks to `AccountService` through an interface, required no edits.

## Data model

```sql
CREATE TABLE accounts (
    id SERIAL PRIMARY KEY,
    owner_name VARCHAR(100) NOT NULL
);

CREATE TABLE ledger_entries (
    id SERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL REFERENCES accounts(id),
    amount NUMERIC(15,2) NOT NULL,
    type VARCHAR(10) NOT NULL,          -- DEBIT / CREDIT
    transaction_id VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);
```

`accounts` holds only reference data about the account owner; `ledger_entries` is a growing, append-only journal of all operations.

## Running locally

```bash
docker compose up -d --build
```

Environment variables are the same as in `master` (`DB_URL`, `DB_USER`, `DB_PASSWORD`), but the tables need to be recreated using the schema above (the `balance` column no longer exists on `accounts`).

## REST API

| Method | Path                        | Description                          |
|--------|------------------------------|---------------------------------------|
| GET    | `/accounts/{id}`              | Get an account (balance computed from the ledger) |
| POST   | `/accounts`                    | Create an account (with an initial balance) |
| PUT    | `/accounts/{id}/deposit`      | Deposit into an account               |
| PUT    | `/accounts/{id}/withdraw`     | Withdraw from an account              |
| PUT    | `/accounts/{id}/transfer`     | Transfer to another account           |

### Examples

```bash
curl -X POST http://localhost:8080/accounts \
  -H "Content-Type: application/json" \
  -d '{"owner_name": "Ivan", "balance": 1000.00}'

curl -X PUT http://localhost:8080/accounts/1/transfer \
  -H "Content-Type: application/json" \
  -d '{"toAccountId": 2, "amount": 100}'
```

Transfer response:

```json
{
  "fromAccountId": 1,
  "toAccountId": 2,
  "fromOwnerName": "Ivan",
  "toOwnerName": "Alex",
  "amount": 100,
  "fromBalanceBefore": 1000.00,
  "fromBalanceAfter": 900.00,
  "toBalanceBefore": 0.00,
  "toBalanceAfter": 100.00
}
```

## Thread safety

`deposit`, `withdraw`, and `transfer` are synchronized (`synchronized`) at the service level, preventing race conditions during concurrent operations on the same account. Verified by a unit test that runs multiple threads in parallel via `ExecutorService` + `CountDownLatch`.

## Tests

```bash
mvn test
```

Fake implementations of both repositories are used (`FakeAccountRepository`, `FakeLedgerRepository`) — tests don't require a running database.

## Deployment

The application is deployed on [Render](https://render.com) (a Web Service built from the Dockerfile + a managed PostgreSQL instance, free tier):

```
https://bankservice-ivia.onrender.com
```

⚠️ Render's free tier spins down on inactivity — the first request after a period of inactivity may take up to ~50 seconds.

## CI/CD

- **GitHub Actions** (`.github/workflows/ci.yml`) — automatically builds the project and runs tests on every push
- **Render** — automatically redeploys on every push to the connected branch
