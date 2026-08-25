CREATE TABLE platform_settings (
    setting_key VARCHAR(100) PRIMARY KEY,
    setting_value VARCHAR(255) NOT NULL
);

INSERT INTO platform_settings (setting_key, setting_value) VALUES ('delivery_fee', '40');