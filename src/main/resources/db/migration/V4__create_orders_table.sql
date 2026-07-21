CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES users(id),
    shop_id BIGINT NOT NULL REFERENCES shops(id),
    delivery_boy_id BIGINT REFERENCES users(id),
    status VARCHAR(30) NOT NULL DEFAULT 'PLACED'
        CHECK (status IN ('PLACED','CONFIRMED','PREPARING','READY_FOR_PICKUP',
                           'PICKED_UP','IN_TRANSIT','DELIVERED','COMPLETED','CANCELLED')),
    payment_mode VARCHAR(10) NOT NULL DEFAULT 'COD' CHECK (payment_mode IN ('COD','ONLINE')),
    total_amount DECIMAL(10,2) NOT NULL,
    delivery_fee DECIMAL(6,2) DEFAULT 0,
    delivery_address TEXT NOT NULL,
    delivery_lat DECIMAL(9,6),
    delivery_lng DECIMAL(9,6),
    otp VARCHAR(6),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    delivered_at TIMESTAMP
);