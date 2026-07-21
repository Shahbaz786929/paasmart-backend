CREATE TABLE cart (

    id BIGSERIAL PRIMARY KEY,

    customer_id BIGINT NOT NULL,

    product_id BIGINT NOT NULL,

    quantity INTEGER NOT NULL,

    price DECIMAL(10,2),

    total_price DECIMAL(10,2),

    created_at TIMESTAMP,

    CONSTRAINT fk_cart_customer
        FOREIGN KEY(customer_id)
        REFERENCES users(id),

    CONSTRAINT fk_cart_product
        FOREIGN KEY(product_id)
        REFERENCES products(id)

);