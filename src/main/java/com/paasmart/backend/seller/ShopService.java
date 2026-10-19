package com.paasmart.backend.seller;

import com.paasmart.backend.exception.BadRequestExceprion;
import com.paasmart.backend.exception.ResourceNotFoundException;
import com.paasmart.backend.seller.dto.ShopRegisterRequest;
import com.paasmart.backend.seller.dto.ShopSummaryResponse;
import com.paasmart.backend.tenant.Tenant;
import com.paasmart.backend.tenant.TenantContext;
import com.paasmart.backend.tenant.TenantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.paasmart.backend.rating.RatingRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ShopService {

    @Autowired
    private ShopRepository shopRepository;
    @Autowired
    private TenantRepository tenantRepository;
    @Autowired
    private RatingRepository ratingRepository;

    public Shop registerShop(Long sellerId, ShopRegisterRequest req) {
        if (shopRepository.existsBySellerId(sellerId)) {
            throw new BadRequestExceprion("This shop is already registered");
        }

        Long tenantId = TenantContext.getTenantId();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new BadRequestExceprion("Invalid tenant context"));

        Shop shop = new Shop();
        shop.setTenant(tenant);
        shop.setSellerId(sellerId);
        shop.setShopName(req.getShopName());
        shop.setCategory(Shop.Category.valueOf(req.getCategory().toUpperCase()));
        shop.setAddress(req.getAddress());
        shop.setCity(req.getCity());
        shop.setLatitude(req.getLatitude());
        shop.setLongitude(req.getLongitude());
        shop.setDocumentsUrl(req.getDocumentsUrl());
        shop.setLogoUrl(req.getLogoUrl());
        shop.setStoreSlug(generateSlug(req.getShopName(), sellerId));
        shop.setStatus(Shop.Status.PENDING);
        shop.setDeliveryRadiusKm(req.getDeliveryRadiusKm() != null ? req.getDeliveryRadiusKm() : 5.0);

        return shopRepository.save(shop);
    }

    public Shop getMyShop(Long sellerId) {
        return shopRepository.findBySellerId(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Your Shop is not registered!!! please registered your shop"));
    }

    private String generateSlug(String shopName, Long sellerId) {
        String base = shopName.toLowerCase().replaceAll("[^a-z0-9]+", "-");
        return base + "-" + sellerId;
    }

    public Shop updateShop(Long sellerId, ShopRegisterRequest req) {
        Shop shop = getMyShop(sellerId);
        shop.setShopName(req.getShopName());
        shop.setCategory(Shop.Category.valueOf(req.getCategory().toUpperCase()));
        shop.setAddress(req.getAddress());
        shop.setCity(req.getCity());
        shop.setLogoUrl(req.getLogoUrl());
        if (req.getLatitude() != null && req.getLongitude() != null) {
            shop.setLatitude(req.getLatitude());
            shop.setLongitude(req.getLongitude());
        }
        // Documents dobara bheje to update karo
        if (req.getDocumentsUrl() != null && !req.getDocumentsUrl().isBlank()) {
            shop.setDocumentsUrl(req.getDocumentsUrl());
        }
        // Rejected shop details sudhaarke save kare to dobara admin approval ke liye PENDING
        if (shop.getStatus() == Shop.Status.REJECTED) {
            shop.setStatus(Shop.Status.PENDING);
            shop.setRejectionReason(null);
        }
        return shopRepository.save(shop);
    }

    // Seller can update their own delivery radius separately
    public Shop updateDeliveryRadius(Long sellerId, Double radiusKm) {
        if (radiusKm == null || radiusKm <= 0) {
            throw new BadRequestExceprion("Delivery radius must be a positive number");
        }
        Shop shop = getMyShop(sellerId);
        shop.setDeliveryRadiusKm(radiusKm);
        return shopRepository.save(shop);
    }

    // ---- Customer-facing (public) methods ----

    public List<ShopSummaryResponse> getNearbyShops(String category, String city, Double lat, Double lng) {
        Long tenantId = TenantContext.getTenantId();
        List<Shop> shops;

        if (category != null && !category.isBlank()) {
            shops = shopRepository.findByStatusAndCategoryAndTenantId(Shop.Status.APPROVED, Shop.Category.valueOf(category.toUpperCase()), tenantId);
        } else if (city != null && !city.isBlank()) {
            shops = shopRepository.findByStatusAndCityIgnoreCaseAndTenantId(Shop.Status.APPROVED, city, tenantId);
        } else {
            shops = shopRepository.findByStatusAndTenantId(Shop.Status.APPROVED, tenantId);
        }

        Map<Long, double[]> ratingStats = loadRatingStats(shops);

        List<ShopSummaryResponse> result = shops.stream()
                .map(shop -> {
                    Double distance = GroUtils.distanceKmOrNull(shop.getLatitude(), shop.getLongitude(), lat, lng);
                    boolean canDeliver = distance == null || distance <= shop.getDeliveryRadiusKm();
                    ShopSummaryResponse summary = new ShopSummaryResponse(shop, distance, canDeliver);
                    applyStatsAndEta(summary, shop, ratingStats, distance);
                    return summary;
                })
                .collect(Collectors.toList());

        if (lat != null && lng != null) {
            result.sort(Comparator.comparing(
                    ShopSummaryResponse::getDistanceKm,
                    Comparator.nullsLast(Comparator.naturalOrder())
            ));
        }

        if (lat == null || lng == null) {
            result.sort(Comparator.comparing(ShopSummaryResponse::getId));
        }

        return result;
    }

    // Public shop detail: raw Shop nahi (usme KYC documents hote hain), sirf safe summary
    public ShopSummaryResponse getPublicShopDetail(Long id) {
        Shop shop = getApprovedShopById(id);
        ShopSummaryResponse summary = new ShopSummaryResponse(shop, null, true);
        applyStatsAndEta(summary, shop, loadRatingStats(List.of(shop)), null);
        return summary;
    }

    // sellerId -> [averageRating, totalReviews]
    private Map<Long, double[]> loadRatingStats(List<Shop> shops) {
        Map<Long, double[]> stats = new HashMap<>();
        List<Long> sellerIds = shops.stream()
                .map(Shop::getSellerId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (sellerIds.isEmpty()) return stats;

        for (Object[] row : ratingRepository.getRatingStatsBySellerIds(sellerIds)) {
            Long sellerId = ((Number) row[0]).longValue();
            double avg = row[1] == null ? 0.0 : ((Number) row[1]).doubleValue();
            long count = ((Number) row[2]).longValue();
            stats.put(sellerId, new double[]{avg, count});
        }
        return stats;
    }

    private void applyStatsAndEta(ShopSummaryResponse summary, Shop shop, Map<Long, double[]> stats, Double distanceKm) {
        double[] st = stats.get(shop.getSellerId());
        if (st != null) {
            summary.setRatingStats(Math.round(st[0] * 10) / 10.0, (long) st[1]);
        }
        // Andaza: 15 min taiyari + 4 min per km. Distance na ho to 25-40 min.
        int min = (distanceKm != null) ? 15 + (int) Math.round(distanceKm * 4) : 25;
        summary.setEstimatedDelivery(min, min + (distanceKm != null ? 10 : 15));
    }

    public Shop getApprovedShopById(Long id) {
        Shop shop = shopRepository.findByIdAndTenantId(id, TenantContext.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found"));
        if (shop.getStatus() != Shop.Status.APPROVED) {
            throw new ResourceNotFoundException("Shop not found");
        }
        return shop;
    }

    // Used at checkout time — can this shop deliver to this customer address?
    public boolean canDeliverTo(Shop shop, Double customerLat, Double customerLng) {
        if (shop.getLatitude() == null || shop.getLongitude() == null) {
            // Fail closed instead of silently allowing delivery: a shop without a
            // location can't have its delivery radius enforced at all.
            throw new BadRequestExceprion("This shop hasn't set up its delivery location yet. Please contact the seller, or try a different shop.");
        }
        if (customerLat == null || customerLng == null) {
            throw new BadRequestExceprion("We couldn't determine your delivery address location. Please re-select your address on the map and try again.");
        }
        double distance = GroUtils.distanceKm(shop.getLatitude(), shop.getLongitude(), customerLat, customerLng);
        return distance <= shop.getDeliveryRadiusKm();
    }
}