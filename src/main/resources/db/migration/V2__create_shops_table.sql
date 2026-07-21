CREATE TABLE shops (
    id BIGSERIAL PRIMARY KEY,
    seller_id BIGINT NOT NULL REFERENCES users(id),
    shop_name VARCHAR(150) NOT NULL,
    category VARCHAR(20) NOT NULL CHECK (category IN ('CLOTHING','FOOD','GENERAL','MULTI')),
    address TEXT NOT NULL,
    city VARCHAR(100),
    latitude DECIMAL(9,6),
    longitude DECIMAL(9,6),
    documents_url TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','APPROVED','REJECTED','SUSPENDED')),
    rejection_reason TEXT,
    store_slug VARCHAR(150) UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);