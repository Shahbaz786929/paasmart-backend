ALTER TABLE coupons ADD COLUMN tenant_id BIGINT;
UPDATE coupons SET tenant_id = (SELECT id FROM tenants WHERE slug = 'default');
ALTER TABLE coupons ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE coupons ADD CONSTRAINT fk_coupons_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);