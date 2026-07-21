CREATE TABLE wishlist (

    id BIGSERIAL PRIMARY KEY,

    customer_id BIGINT NOT NULL,

    product_id BIGINT NOT NULL,

    created_at TIMESTAMP,

    CONSTRAINT fk_wishlist_customer
        FOREIGN KEY(customer_id)
        REFERENCES users(id),

    CONSTRAINT fk_wishlist_product
        FOREIGN KEY(product_id)
        REFERENCES products(id)

);