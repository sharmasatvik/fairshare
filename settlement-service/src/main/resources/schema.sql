-- A settlement record is deliberately simple: who paid whom, how much, when.
-- No splitting logic needed here (that's expense-service's job), a
-- settlement is always a single direct payment between two people.
CREATE TABLE IF NOT EXISTS settlements (
    id              UUID PRIMARY KEY,
    group_id        UUID NOT NULL,
    paid_by_user_id VARCHAR(64) NOT NULL,
    paid_to_user_id VARCHAR(64) NOT NULL,
    amount          NUMERIC(12, 2) NOT NULL,
    currency        VARCHAR(3) NOT NULL DEFAULT 'INR',
    created_at      TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_settlements_group_id ON settlements(group_id);
