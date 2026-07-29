ALTER TABLE shops ADD COLUMN tenant_id BIGINT;
UPDATE shops SET tenant_id = (SELECT id FROM tenants WHERE slug = 'default');
ALTER TABLE shops ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE shops ADD CONSTRAINT fk_shops_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);