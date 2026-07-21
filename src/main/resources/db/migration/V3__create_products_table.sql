CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    shop_id BIGINT NOT NULL REFERENCES shops(id),
    name VARCHAR(200) NOT NULL,
    category VARCHAR(100),
    sub_category VARCHAR(100),
    price DECIMAL(10,2) NOT NULL,
    discount_percent INT DEFAULT 0,
    stock_qty INT DEFAULT 0,
    images TEXT,
    description TEXT,
    is_available BOOLEAN DEFAULT TRUE,
    try_on_enabled BOOLEAN DEFAULT FALSE,
    voice_description_url VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);