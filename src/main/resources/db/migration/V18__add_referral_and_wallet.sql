ALTER TABLE users ADD COLUMN referral_code VARCHAR(10) UNIQUE;
ALTER TABLE users ADD COLUMN referred_by BIGINT REFERENCES users(id);
ALTER TABLE users ADD COLUMN wallet_balance DECIMAL(10,2) NOT NULL DEFAULT 0;

CREATE TABLE wallet_transactions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    amount DECIMAL(10,2) NOT NULL,          -- positive = credit, negative = debit
    type VARCHAR(30) NOT NULL,              -- REFERRAL_BONUS, WELCOME_BONUS, ORDER_PAYMENT, REFUND
    description VARCHAR(255),
    order_id BIGINT REFERENCES orders(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);