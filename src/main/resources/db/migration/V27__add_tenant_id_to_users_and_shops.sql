ALTER TABLE users ADD COLUMN tenant_id BIGINT;
UPDATE users SET tenant_id = (SELECT id FROM tenants WHERE slug = 'default');
ALTER TABLE users ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE users ADD CONSTRAINT fk_users_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);