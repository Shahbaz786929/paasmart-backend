CREATE TABLE addresses (

    id BIGSERIAL PRIMARY KEY,

    customer_id BIGINT NOT NULL,

    full_name VARCHAR(100),

    mobile_number VARCHAR(15),

    house_no VARCHAR(100),

    area VARCHAR(200),

    landmark VARCHAR(200),

    city VARCHAR(100),

    state VARCHAR(100),

    pincode VARCHAR(10),

    default_address BOOLEAN DEFAULT FALSE,

    CONSTRAINT fk_address_customer
        FOREIGN KEY(customer_id)
        REFERENCES users(id)

);