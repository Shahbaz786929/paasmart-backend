CREATE TABLE flash_sales (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id),
    shop_id BIGINT NOT NULL REFERENCES shops(id),
    discount_percent INT NOT NULL,
    starts_at TIMESTAMP NOT NULL,
    ends_at TIMESTAMP NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_flash_sales_product ON flash_sales(product_id);
CREATE INDEX idx_flash_sales_window ON flash_sales(starts_at, ends_at);