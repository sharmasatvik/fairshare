-- Pairwise balances within a group. Convention: owed_by owes owed_to.
-- Only one row exists per (group_id, low_user, high_user) pair, user_a/user_b
-- are stored in a canonical (lexicographically sorted) order so a debt from
-- Alice to Bob and a later payment from Bob to Alice net into the same row
-- instead of creating two rows that both need reconciling.
CREATE TABLE IF NOT EXISTS balances (
    id              UUID PRIMARY KEY,
    group_id        UUID NOT NULL,
    user_a          VARCHAR(64) NOT NULL,
    user_b          VARCHAR(64) NOT NULL,
    -- positive net_amount means user_b owes user_a; negative means user_a owes user_b
    net_amount      NUMERIC(12, 2) NOT NULL DEFAULT 0,
    updated_at      TIMESTAMP NOT NULL,
    UNIQUE (group_id, user_a, user_b)
);

CREATE INDEX IF NOT EXISTS idx_balances_group_id ON balances(group_id);

-- Idempotency ledger: every processed event's ID is recorded here so a
-- redelivered message (consumer rebalance, retry after a transient error,
-- etc.) is a safe no-op instead of double-counting a balance update.
CREATE TABLE IF NOT EXISTS processed_events (
    event_id        UUID PRIMARY KEY,
    processed_at    TIMESTAMP NOT NULL
);
