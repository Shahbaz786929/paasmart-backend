CREATE TABLE coupons (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    description VARCHAR(255),
    discount_type VARCHAR(20) NOT NULL,      -- PERCENTAGE or FLAT
    discount_value DECIMAL(10,2) NOT NULL,
    max_discount_amount DECIMAL(10,2),       -- percentage type ke liye cap (optional)
    min_order_amount DECIMAL(10,2) DEFAULT 0,
    usage_limit_per_user INT DEFAULT 1,
    valid_from TIMESTAMP,
    valid_until TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE coupon_usages (
    id BIGSERIAL PRIMARY KEY,
    coupon_id BIGINT NOT NULL REFERENCES coupons(id),
    customer_id BIGINT NOT NULL REFERENCES users(id),
    order_id BIGINT REFERENCES orders(id),
    used_at TIMESTAMP NOT NULL DEFAULT NOW()
);