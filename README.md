# FairShare: SplitWise Clone

A backend-only SplitWise-style expense splitting system, built to demonstrate
microservices + Kafka patterns.

## Why this exists

Most "Kafka demo" projects use Kafka decoratively, a producer and a consumer
that could just as easily have been a REST call. This project tries to use
Kafka where it actually earns its place: the write path (recording an expense)
and the read path (computing balances) genuinely have different scaling and
consistency needs, so splitting them across an event boundary is a real
design decision, not a contrived one.

## Services

| Service             | Responsibility                                                                        | Owns                       |
|---------------------|---------------------------------------------------------------------------------------|----------------------------|
| `expense-service`   | Groups, expenses, splits. Publishes `expense-events`.                                 | Postgres (`expense_db`)    |
| `ledger-service`    | Consumes `expense-events`, maintains pairwise balances, computes debt simplification. | Postgres (`ledger_db`)     |
| `settlement-service | Records settle-up payments between two users. Publishes`settlement-events`            | Postgres (`settlement_db`) |

## Event flow

```
POST /groups/{id}/expenses
        |
        ˅
 expense-service -- publishes -->  Kafka topic: expense-events
        |                              (key = groupId, so all events
        |                               for a group are ordered)
        |
        ˅
 ledger-service (consumer group: ledger-service)
        |
        |- dedupe via processed_events table (idempotency)
        |- update pairwise balances
        |- (settlement = a debt in the reverse direction, same applyDebt() logic handles both)
        |- on repeated failure -> expense-events.DLT
```

```
Note: ordering is only guaranteed *within* a topic's partition, not
*across* the two topics. In practice this doesn't matter for correctness
here, balance updates are purely additive, so the final state is the same
regardless of which order an expense and a settlement for the same group
are applied in. It would only matter for a system that needed to reject a
settlement that overpays a debt that hasn't been recorded yet, which this
project doesn't attempt.
```

## Design decisions

**Partitioning by `groupId`, not `expenseId`.** Kafka only guarantees
ordering within a partition. Balance updates are not commutative in a way
that's safe to reorder across a group (imagine an expense followed by a
correction to that expense arriving out of order), so every event for a
given group must land on the same partition and be processed in order.
Cross-group parallelism is preserved since different groups can land on
different partitions.

**Idempotency via a processed-events table, not "exactly-once" Kafka
config.** Consumer rebalances and retries mean at-least-once delivery is
the realistic baseline. Rather than lean on Kafka transactions, the ledger
service tracks processed `eventId`s and no-ops on replay. This is simpler
to reason about and is the same pattern you'd need even with a different
message broker.

**Dead-letter topic for poison messages.** If a balance update fails
repeatedly (e.g. malformed event, referential issue), it's moved to a
dead-letter topic (`expense-events.DLT` or `settlement-events.DLT`,
following Spring Kafka's default "source topic + .DLT" convention) after
retry exhaustion, instead of blocking the partition indefinitely for
every group sharing it. One error handler in `KafkaConsumerConfig`
covers both topics.

**Settlement reuses the expense debt-update logic instead of adding new
logic.** A settlement (X pays Y) is mathematically a debt in the reverse
direction, it's applied as "Y now owes X" via the same `applyDebt()`
method `ExpenseCreatedEvent` handling uses, just with the roles swapped.
No separate settlement-specific balance math exists.

## Running locally

```bash
docker compose up -d                              # Kafka (KRaft) + 3x Postgres + Kafka UI
cd expense-service && ./mvnw spring-boot:run      # port:8081
cd ledger-service && ./mvnw spring-boot:run       # port:8082
cd settlement-service && ./mvnw spring-boot:run   # port:8083
```

Kafka UI available at `localhost:8090` for inspecting topics/messages.

Each service is an independent Maven project (no shared parent), so each
needs its own `./mvnw`, there's no top-level build that compiles both at
once. `mvn -N io.takari:maven:wrapper` (or your IDE) generates the wrapper
if it's missing.

## Try it out

```bash
# 1. Create a group
curl -s -X POST localhost:8081/groups \
  -H "Content-Type: application/json" \
  -d '{"name": "Goa Trip", "memberUserIds": ["alice", "bob", "carol"]}'

# 2. Alice pays 900 for a hotel, split equally three ways
curl -X POST localhost:8081/groups/$GROUP_ID/expenses \
  -H "Content-Type: application/json" \
  -d '{
        "description": "Hotel",
        "paidByUserId": "alice",
        "amount": 900.00,
        "splitType": "EQUAL",
        "participantUserIds": ["alice", "bob", "carol"]
      }'

# 3. Bob pays 150 for dinner, split exactly (he skips his own dessert)
curl -X POST localhost:8081/groups/$GROUP_ID/expenses \
  -H "Content-Type: application/json" \
  -d '{
        "description": "Dinner",
        "paidByUserId": "bob",
        "amount": 150.00,
        "splitType": "EXACT",
        "participantUserIds": ["alice", "carol"],
        "explicitValues": {"alice": 75.00, "carol": 75.00}
      }'

# 4. Give ledger-service a moment to consume both events, then check balances.
# Note bob<->alice nets to 300 - 75 = 225 (same pair, opposite directions
# from the two expenses); carol's debts stay separate since they're to
# two different people (alice, bob).
curl localhost:8082/groups/$GROUP_ID/balances

# [{"owedByUserId":"bob","owedToUserId":"alice","amount":225.00},
#  {"owedByUserId":"carol","owedToUserId":"alice","amount":300.00},
#  {"owedByUserId":"carol","owedToUserId":"bob","amount":75.00}]

# 5. See the minimal settle-up plan. Net positions: alice +525, bob -150,
# carol -375 -> greedy matching settles carol against alice first (largest
# amounts), then bob's remainder against alice.
curl localhost:8082/groups/$GROUP_ID/simplify
# [{"fromUserId":"carol","toUserId":"alice","amount":375.00},
#  {"fromUserId":"bob","toUserId":"alice","amount":150.00}]

# Note this plan doesn't match the raw pairwise balances 1:1 - that's
# expected. It's the minimum-transaction settlement for the *group's net
# position*, not a payoff of each individual pairwise debt from step 4.

# 6. Settle the raw pairwise debts directly instead (simpler to verify by
# hand than following the simplify plan, since each call closes exactly
# one balances row).
# Bob settles his 225 debt to alice
curl -X POST localhost:8083/groups/$GROUP_ID/settlements \
  -H "Content-Type: application/json" \
  -d '{"paidByUserId": "bob", "paidToUserId": "alice", "amount": 225.00}'

# Carol settles her 300 debt to alice
curl -X POST localhost:8083/groups/$GROUP_ID/settlements \
  -H "Content-Type: application/json" \
  -d '{"paidByUserId": "carol", "paidToUserId": "alice", "amount": 300.00}'

# Carol settles her 75 debt to bob
curl -X POST localhost:8083/groups/$GROUP_ID/settlements \
  -H "Content-Type: application/json" \
  -d '{"paidByUserId": "carol", "paidToUserId": "bob", "amount": 75.00}'

# 7. Everything should now be fully settled
curl localhost:8082/groups/$GROUP_ID/balances
```

Watch the `expense-events` topic fill up in Kafka UI (`localhost:8090`) as
step 2 and 3 run, the message key will be `$GROUP_ID` for both, which is
what pins them to the same partition.

## Testing

```bash
cd expense-service && ./mvnw test   # SplitCalculator: rounding edge cases for EQUAL/EXACT/PERCENTAGE
cd ledger-service && ./mvnw test    # Balance: sign-convention correctness; DebtSimplifier: settlement correctness
```

## Known limitations

- **No transactional outbox.** `ExpenseService` persists the expense and
  publishes the Kafka event in the same method, not atomically. A crash
  between the two would leave an expense with no corresponding balance
  update. The fix is a transactional outbox table + poller/CDC, noted in
  `ExpenseService`'s Javadoc, left unbuilt so the demo's complexity budget
  stays on the consumer-side idempotency/ordering story instead.
- **Debt simplification is a heuristic, not a proven minimum.** The
  greedy largest-creditor/largest-debtor matching in `DebtSimplifier`
  terminates in at most `n-1` transactions and matches what production
  apps like SplitWise actually ship, but the *provably minimum* transaction
  count is an NP-hard problem in general. Worth saying explicitly rather
  than overclaiming "optimal."
- **Two copies of each event contract.** Since the services are
  independent Maven projects, `ExpenseCreatedEvent` and
  `SettlementRecordedEvent` are each duplicated (once in their producing
  service, once in `ledger-service`) rather than shared via a library
  module. Keeping them in sync is manual, a schema registry
  (Avro/Protobuf) is the real fix once this needs to scale past one
  person maintaining all three services.
- **`settlement-service` has no unit tests yet.** `Settlement.create()` has
  one validation rule (can't settle with yourself) that's simple enough it
  was written without a test first, unlike `SplitCalculator` and
  `DebtSimplifier` which had genuinely fiddly edge cases worth locking
  down. Worth adding before extending this service further, not because
  the current logic is complex, but because untested code is where the
  next change is most likely to introduce a regression.
- **No membership validation in `settlement-service`.** It doesn't check
  that `paidByUserId`/`paidToUserId` are actually members of the group,
  that data lives in expense-service's database, and this service doesn't
  call across to verify it (documented further in `SettlementService`'s
  Javadoc, along with the event-carried-state-transfer pattern that would
  close the gap).

## Repo layout

```
fairshare/
|-- docker-compose.yml
|-- expense-service/        (independent Maven project)
|-- ledger-service/         (independent Maven project)
|-- settlement-service/     (independent Maven project)
```
