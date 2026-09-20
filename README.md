model/      → Account, LedgerEntry, EntryType (DEBIT/CREDIT)
dto/        → AmountRequest, BalanceChangeResponse, TransferRequest, TransferResponse
exceptions/ → AccountNotFoundException, InsufficientFoundsException


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


docker compose up -d --build


curl -X POST http://localhost:8080/accounts \
  -H "Content-Type: application/json" \
  -d '{"owner_name": "Ivan", "balance": 1000.00}'

curl -X PUT http://localhost:8080/accounts/1/transfer \
  -H "Content-Type: application/json" \
  -d '{"toAccountId": 2, "amount": 100}'


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


mvn test




https://bankservice-ivia.onrender.com


