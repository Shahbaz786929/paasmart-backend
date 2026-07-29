ALTER TABLE orders ADD COLUMN tenant_id BIGINT;
UPDATE orders SET tenant_id = (SELECT id FROM tenants WHERE slug = 'default');
ALTER TABLE orders ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE orders ADD CONSTRAINT fk_orders_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);