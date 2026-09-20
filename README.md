BankService (ветка double-ledger)

Развитие базовой версии (master) — вместо хранения баланса как одного поля в таблице accounts, баланс вычисляется из журнала проводок (double-entry ledger), как в классическом бухгалтерском учёте. Добавлена операция перевода между счетами.

Чем отличается от master
	master	double-ledger
Хранение баланса	Поле balance в таблице accounts, меняется напрямую	Вычисляется как сумма записей в ledger_entries
История операций	Не хранится	Каждая операция — запись в журнале, ничего не перезаписывается
Перевод между счетами	Не реализован	Реализован как одна атомарная операция с двумя связанными записями
Принцип double-entry

Каждая операция — это одна или несколько записей в таблице ledger_entries. Списание (DEBIT) и зачисление (CREDIT) никогда не хранятся как готовое число в счёте — баланс всегда пересчитывается на лету:

balance(account) = SUM(CREDIT) - SUM(DEBIT) по всем записям этого счёта

Перевод между двумя счетами создаёт две записи с одним и тем же transaction_id: DEBIT у отправителя и CREDIT у получателя — это гарантирует, что операция либо отражена полностью, либо не отражена вовсе.

Архитектура
servlet/    → AccountServlet — HTTP-слой, без изменений в самой структуре
service/    → AccountServiceImpl — считает баланс через LedgerRepository, создаёт LedgerEntry вместо UPDATE
repository/ → AccountRepository (данные о владельце счёта) + LedgerRepository (журнал операций)
model/      → Account, LedgerEntry, EntryType (DEBIT/CREDIT)
dto/        → AmountRequest, BalanceChangeResponse, TransferRequest, TransferResponse
exceptions/ → AccountNotFoundException, InsufficientFoundsException

AccountServlet не изменился при переходе на ledger-архитектуру — это демонстрирует принцип Open/Closed: способ подсчёта баланса поменялся полностью, но HTTP-слой, работающий через интерфейс AccountService, не потребовал правок.

Модель данных
sql
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

accounts хранит только справочные данные о владельце счёта; ledger_entries — растущий, неизменяемый журнал всех операций.

Запуск локально
bash
docker compose up -d --build

Переменные окружения — как в master (DB_URL, DB_USER, DB_PASSWORD), но таблицы нужно создать заново по схеме выше (колонки balance в accounts больше нет).

REST API
Метод	Путь	Описание
GET	/accounts/{id}	Получить счёт (баланс считается из ledger)
POST	/accounts	Создать счёт (с начальным балансом)
PUT	/accounts/{id}/deposit	Пополнить счёт
PUT	/accounts/{id}/withdraw	Снять со счёта
PUT	/accounts/{id}/transfer	Перевести на другой счёт
Примеры
bash
curl -X POST http://localhost:8080/accounts \
  -H "Content-Type: application/json" \
  -d '{"owner_name": "Ivan", "balance": 1000.00}'

curl -X PUT http://localhost:8080/accounts/1/transfer \
  -H "Content-Type: application/json" \
  -d '{"toAccountId": 2, "amount": 100}'

Ответ на transfer:

json
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
Потокобезопасность

deposit, withdraw и transfer синхронизированы (synchronized) на уровне сервиса, что предотвращает гонку потоков при параллельных операциях с одним счётом. Подтверждено unit-тестом с параллельным запуском нескольких потоков через ExecutorService + CountDownLatch.

Тесты
bash
mvn test

Используются fake-реализации обоих репозиториев (FakeAccountRepository, FakeLedgerRepository) — тесты не требуют поднятой БД.

Деплой

Приложение развёрнуто на Render (Web Service из Dockerfile + управляемый PostgreSQL, бесплатный тариф):

https://bankservice-ivia.onrender.com

⚠️ Бесплатный тариф Render «засыпает» при неактивности — первый запрос после паузы может занять до ~50 секунд.

CI/CD
GitHub Actions (.github/workflows/ci.yml) — при каждом push автоматически собирает проект и прогоняет тесты
Render — автоматический деплой при push в подключённую ветку
