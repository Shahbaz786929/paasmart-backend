CREATE TABLE product_reviews (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id),
    customer_id BIGINT NOT NULL REFERENCES users(id),
    order_id BIGINT NOT NULL REFERENCES orders(id),
    rating SMALLINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    review_text TEXT,
    images TEXT,                              -- comma-separated image URLs, jaisa products.images me hai
    seller_reply TEXT,
    seller_replied_at TIMESTAMP,
    helpful_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (product_id, order_id)             -- ek order me ek product ka ek hi review
);

CREATE INDEX idx_product_reviews_product ON product_reviews(product_id);