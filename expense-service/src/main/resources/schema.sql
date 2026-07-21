CREATE TABLE IF NOT EXISTS groups (
    id              UUID PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    created_at      TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS group_members (
    group_id        UUID NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    user_id         VARCHAR(64) NOT NULL,
    PRIMARY KEY (group_id, user_id)
);

CREATE TABLE IF NOT EXISTS expenses (
    id              UUID PRIMARY KEY,
    group_id        UUID NOT NULL REFERENCES groups(id),
    description     VARCHAR(255) NOT NULL,
    paid_by_user_id VARCHAR(64) NOT NULL,
    amount          NUMERIC(12, 2) NOT NULL,
    currency        VARCHAR(3) NOT NULL DEFAULT 'INR',
    split_type      VARCHAR(20) NOT NULL,
    created_at      TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS expense_splits (
    id              UUID PRIMARY KEY,
    expense_id      UUID NOT NULL REFERENCES expenses(id) ON DELETE CASCADE,
    user_id         VARCHAR(64) NOT NULL,
    share_amount    NUMERIC(12, 2) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_expenses_group_id ON expenses(group_id);
CREATE INDEX IF NOT EXISTS idx_expense_splits_expense_id ON expense_splits(expense_id);
