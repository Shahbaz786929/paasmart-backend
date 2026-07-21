package com.paasmart.backend.seller;

import com.paasmart.backend.exception.BadRequestExceprion;
import com.paasmart.backend.exception.ResourceNotFoundException;
import com.paasmart.backend.seller.dto.ShopRegisterRequest;
import com.paasmart.backend.seller.dto.ShopSummaryResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ShopService {

    @Autowired
    private ShopRepository shopRepository;

    public Shop registerShop(Long sellerId, ShopRegisterRequest req) {
        if (shopRepository.existsBySellerId(sellerId)) {
            throw new BadRequestExceprion("This shop is already registered");
        }

        Shop shop = new Shop();
        shop.setSellerId(sellerId);
        shop.setShopName(req.getShopName());
        shop.setCategory(Shop.Category.valueOf(req.getCategory().toUpperCase()));
        shop.setAddress(req.getAddress());
        shop.setCity(req.getCity());
        shop.setLatitude(req.getLatitude());
        shop.setLongitude(req.getLongitude());
        shop.setDocumentsUrl(req.getDocumentsUrl());
        shop.setStoreSlug(generateSlug(req.getShopName(), sellerId));
        shop.setStatus(Shop.Status.PENDING);

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
        return shopRepository.save(shop);
    }

    // ---- Customer-facing (public) methods ----

    public List<ShopSummaryResponse> getNearbyShops(String category, String city, Double lat, Double lng) {
        List<Shop> shops;

        if (category != null && !category.isBlank()) {
            shops = shopRepository.findByStatusAndCategory(Shop.Status.APPROVED, Shop.Category.valueOf(category.toUpperCase()));
        } else if (city != null && !city.isBlank()) {
            shops = shopRepository.findByStatusAndCityIgnoreCase(Shop.Status.APPROVED, city);
        } else {
            shops = shopRepository.findByStatus(Shop.Status.APPROVED);
        }

        List<ShopSummaryResponse> result = shops.stream()
                .map(shop -> new ShopSummaryResponse(shop, GroUtils.distanceKmOrNull(shop.getLatitude(), shop.getLongitude(), lat, lng)))
                .collect(Collectors.toList());

        if (lat != null && lng != null) {
            result.sort(Comparator.comparing(
                    ShopSummaryResponse::getDistanceKm,
                    Comparator.nullsLast(Comparator.naturalOrder())
            ));
        }

        return result;
    }

    public Shop getApprovedShopById(Long id) {
        Shop shop = shopRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found"));
        if (shop.getStatus() != Shop.Status.APPROVED) {
            throw new ResourceNotFoundException("Shop not found");
        }
        return shop;
    }
}