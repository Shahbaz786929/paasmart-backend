package com.paasmart.backend.settings;

import com.paasmart.backend.exception.BadRequestExceprion;
import com.paasmart.backend.tenant.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class SettingsService {

    @Autowired private PlatformSettingRepository settingRepository;

    private static final String BASE_FEE_KEY = "delivery_fee";           // reuses old key -- existing data becomes the "first km" base fee
    private static final String PER_KM_FEE_KEY = "delivery_per_km_fee";  // new key

    private static final BigDecimal BASE_FEE_FALLBACK = new BigDecimal("40");
    private static final BigDecimal PER_KM_FEE_FALLBACK = BigDecimal.ZERO; // 0 = old flat-fee behaviour until admin sets it

    // ---- Reads (used by checkout) ----

    public BigDecimal getDeliveryBaseFee() {
        Long tenantId = TenantContext.getTenantId();
        return settingRepository.findByTenantIdAndKey(tenantId, BASE_FEE_KEY)
                .map(s -> new BigDecimal(s.getValue()))
                .orElse(BASE_FEE_FALLBACK);
    }

    public BigDecimal getDeliveryPerKmFee() {
        Long tenantId = TenantContext.getTenantId();
        return settingRepository.findByTenantIdAndKey(tenantId, PER_KM_FEE_KEY)
                .map(s -> new BigDecimal(s.getValue()))
                .orElse(PER_KM_FEE_FALLBACK);
    }

    // Formula: base fee covers the first 1 km. Every km beyond that (rounded UP
    // to the next whole km) is charged at perKmFee.
    // e.g. base=20, perKm=5, distance=3.2km -> extra = ceil(2.2) = 3 -> 20 + 3*5 = 35
    public BigDecimal calculateDeliveryFee(double distanceKm) {
        BigDecimal baseFee = getDeliveryBaseFee();
        BigDecimal perKmFee = getDeliveryPerKmFee();

        double extraDistance = Math.max(0, distanceKm - 1.0);
        long extraKm = (long) Math.ceil(extraDistance);

        return baseFee.add(perKmFee.multiply(BigDecimal.valueOf(extraKm)));
    }

    // ---- Writes (used by TENANT_ADMIN from the admin app) ----

    public BigDecimal updateDeliveryBaseFee(BigDecimal newFee) {
        return saveSetting(BASE_FEE_KEY, newFee);
    }

    public BigDecimal updateDeliveryPerKmFee(BigDecimal newFee) {
        return saveSetting(PER_KM_FEE_KEY, newFee);
    }

    private BigDecimal saveSetting(String key, BigDecimal newValue) {
        if (newValue == null || newValue.signum() < 0) {
            throw new BadRequestExceprion("Value must be a valid non-negative amount");
        }

        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BadRequestExceprion("Delivery pricing must be set from a city-partner (TENANT_ADMIN) account");
        }

        PlatformSetting setting = settingRepository.findByTenantIdAndKey(tenantId, key)
                .orElseGet(() -> {
                    PlatformSetting s = new PlatformSetting();
                    s.setTenantId(tenantId);
                    s.setKey(key);
                    return s;
                });
        setting.setValue(newValue.toPlainString());
        settingRepository.save(setting);
        return newValue;
    }
}