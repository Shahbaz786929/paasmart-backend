CREATE TABLE tryon_history (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES users(id),
    product_id BIGINT NOT NULL REFERENCES products(id),
    result_image_url VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'PROCESSING',   -- PROCESSING, COMPLETED, FAILED
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);