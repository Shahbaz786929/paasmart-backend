package com.paasmart.backend.settings;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PlatformSettingRepository extends JpaRepository<PlatformSetting, Long> {
    Optional<PlatformSetting> findByTenantIdAndKey(Long tenantId, String key);
}