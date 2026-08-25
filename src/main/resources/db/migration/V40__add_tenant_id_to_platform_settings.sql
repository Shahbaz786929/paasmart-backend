ALTER TABLE platform_settings ADD COLUMN id BIGSERIAL;
ALTER TABLE platform_settings ADD COLUMN tenant_id BIGINT;

UPDATE platform_settings SET tenant_id = (SELECT id FROM tenants WHERE slug = 'default');

ALTER TABLE platform_settings ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE platform_settings ADD CONSTRAINT fk_platform_settings_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);

ALTER TABLE platform_settings DROP CONSTRAINT platform_settings_pkey;
ALTER TABLE platform_settings ADD PRIMARY KEY (id);
ALTER TABLE platform_settings ADD CONSTRAINT uq_platform_settings_tenant_key UNIQUE (tenant_id, setting_key);