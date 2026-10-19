package com.paasmart.backend.flashsale;

import com.paasmart.backend.exception.BadRequestExceprion;
import com.paasmart.backend.exception.ResourceNotFoundException;
import com.paasmart.backend.exception.UnauthorizedException;
import com.paasmart.backend.flashsale.dto.ActiveDealResponse;
import com.paasmart.backend.flashsale.dto.FlashSaleRequest;
import com.paasmart.backend.product.Product;
import com.paasmart.backend.product.ProductRepository;
import com.paasmart.backend.seller.Shop;
import com.paasmart.backend.seller.ShopRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FlashSaleService {

    @Autowired private FlashSaleRepository flashSaleRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private ShopRepository shopRepository;

    public FlashSale createFlashSale(Long sellerId, FlashSaleRequest req) {
        Shop shop = shopRepository.findBySellerId(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Your shop is not registered"));

        Product product = productRepository.findById(req.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (!product.getShopId().equals(shop.getId())) {
            throw new UnauthorizedException("This product is not in your shop");
        }
        if (req.getEndsAt().isBefore(req.getStartsAt()) || req.getEndsAt().isEqual(req.getStartsAt())) {
            throw new BadRequestExceprion("End time must be after start time");
        }

        if (shop.getStatus() != Shop.Status.APPROVED) {
            throw new BadRequestExceprion("Your shop is not approved yet.");
        }
        if (!Boolean.TRUE.equals(product.getIsAvailable())) {
            throw new BadRequestExceprion("This product is hidden. Show it first to run a flash sale.");
        }
        if (flashSaleRepository.existsOverlapping(product.getId(), req.getStartsAt(), req.getEndsAt())) {
            throw new BadRequestExceprion("This product already has a flash sale during that time.");
        }

        if (req.getEndsAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestExceprion("End time cannot be in the past");
        }

        FlashSale sale = new FlashSale();
        sale.setProductId(product.getId());
        sale.setShopId(shop.getId());
        sale.setTenantId(shop.getTenant().getId());
        sale.setDiscountPercent(req.getDiscountPercent());
        sale.setStartsAt(req.getStartsAt());
        sale.setEndsAt(req.getEndsAt());

        return flashSaleRepository.save(sale);
    }

    public List<FlashSale> getMyFlashSales(Long sellerId) {
        Shop shop = shopRepository.findBySellerId(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Your shop is not registered"));
        return flashSaleRepository.findByShopIdOrderByCreatedAtDesc(shop.getId());
    }

    public FlashSale deactivate(Long sellerId, Long flashSaleId) {
        FlashSale sale = flashSaleRepository.findById(flashSaleId)
                .orElseThrow(() -> new ResourceNotFoundException("Flash sale not found"));

        Shop shop = shopRepository.findBySellerId(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Your shop is not registered"));

        if (!sale.getShopId().equals(shop.getId())) {
            throw new UnauthorizedException("This flash sale is not for your shop");
        }

        sale.setActive(false);
        return flashSaleRepository.save(sale);
    }

    // ---- Customer-facing ----

    // Ek product ka effective price (agar active flash sale hai to discounted, warna normal)
    public BigDecimal getEffectivePrice(Product product) {
        BigDecimal base = product.getPrice();
        int productDiscount = product.getDiscountPercent() == null ? 0 : product.getDiscountPercent();
        // Whole rupee tak round (customer app bhi Math.round use karta hai, to dono jagah same price dikhega)
        BigDecimal regular = productDiscount > 0
                ? applyDiscount(base, productDiscount).setScale(0, RoundingMode.HALF_UP)
                : base;

        return flashSaleRepository.findActiveForProduct(product.getId(), LocalDateTime.now())
                .stream()
                .findFirst()
                .map(sale -> applyDiscount(base, sale.getDiscountPercent()))
                .map(flashPrice -> flashPrice.min(regular))
                .orElse(regular);
    }

    private BigDecimal applyDiscount(BigDecimal price, Integer percent) {
        BigDecimal discount = price.multiply(BigDecimal.valueOf(percent)).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return price.subtract(discount);
    }

    // Sabhi live deals — customer app ke "Live Deals" section ke liye
    public List<ActiveDealResponse> getAllActiveDeals() {
        List<FlashSale> activeSales = flashSaleRepository.findAllCurrentlyActive(
                com.paasmart.backend.tenant.TenantContext.getTenantId(), LocalDateTime.now());

        return activeSales.stream()
                .map(sale -> {
                    Product product = productRepository.findById(sale.getProductId()).orElse(null);
                    if (product == null) return null;
                    if (!Boolean.TRUE.equals(product.getIsAvailable())
                            || product.getStockQty() == null || product.getStockQty() <= 0) return null;

                    Shop shop = shopRepository.findById(product.getShopId()).orElse(null);
                    if (shop == null || shop.getStatus() != Shop.Status.APPROVED) return null;

                    BigDecimal dealPrice = getEffectivePrice(product);
                    return new ActiveDealResponse(
                            sale.getId(), product.getId(), product.getName(), product.getImages(),
                            product.getPrice(), dealPrice, sale.getDiscountPercent(), sale.getEndsAt()
                    );
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
    }
}