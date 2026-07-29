ALTER TABLE products ADD COLUMN tenant_id BIGINT;
UPDATE products SET tenant_id = (SELECT id FROM tenants WHERE slug = 'default');
ALTER TABLE products ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE products ADD CONSTRAINT fk_products_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);