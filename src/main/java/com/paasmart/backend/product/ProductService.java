package com.paasmart.backend.product;

import com.paasmart.backend.exception.BadRequestExceprion;
import com.paasmart.backend.exception.ResourceNotFoundException;
import com.paasmart.backend.exception.UnauthorizedException;
import com.paasmart.backend.product.dto.ProductRequest;
import com.paasmart.backend.seller.Shop;
import com.paasmart.backend.seller.ShopRepository;
import com.paasmart.backend.wishlist.WishlistNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    @Autowired private ProductRepository productRepository;
    @Autowired private ShopRepository shopRepository;
    @Autowired private WishlistNotificationService wishlistNotificationService;

    private Shop getApprovedShop(Long sellerId) {
        Shop shop = shopRepository.findBySellerId(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Your shop is not registered"));
        if (shop.getStatus() != Shop.Status.APPROVED) {
            throw new BadRequestExceprion("your shop still not approved by admin");
        }
        return shop;
    }

    public Product addProduct(Long sellerId, ProductRequest req) {
        Shop shop = getApprovedShop(sellerId);

        Product product = new Product();
        product.setShopId(shop.getId());
        product.setName(req.getName());
        product.setCategory(req.getCategory());
        product.setSubCategory(req.getSubCategory());
        product.setPrice(req.getPrice());
        product.setDiscountPercent(req.getDiscountPercent());
        product.setStockQty(req.getStockQty());
        product.setImages(req.getImages());
        product.setDescription(req.getDescription());
        product.setTryOnEnabled(req.getTryOnEnabled());
        product.setVoiceDescriptionUrl(req.getVoiceDescriptionUrl());

        return productRepository.save(product);
    }

    public List<Product> getMyProducts(Long sellerId) {
        Shop shop = shopRepository.findBySellerId(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("your shop is not registered"));
        return productRepository.findByShopId(shop.getId());
    }

    public Product updateProduct(Long sellerId, Long productId, ProductRequest req) {
        Shop shop = getApprovedShop(sellerId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (!product.getShopId().equals(shop.getId())) {
            throw new UnauthorizedException("this product is not in your shop");
        }

        int oldStock = product.getStockQty();

        product.setName(req.getName());
        product.setCategory(req.getCategory());
        product.setSubCategory(req.getSubCategory());
        product.setPrice(req.getPrice());
        product.setDiscountPercent(req.getDiscountPercent());
        product.setStockQty(req.getStockQty());
        if (req.getImages() != null) product.setImages(req.getImages());
        product.setDescription(req.getDescription());
        product.setTryOnEnabled(req.getTryOnEnabled());

        Product updated = productRepository.save(product);

        if (oldStock == 0 && updated.getStockQty() > 0) {
            wishlistNotificationService.notifyBackInStock(updated);
        }

        return updated;
    }

    public void deleteProduct(Long sellerId, Long productId) {
        Shop shop = getApprovedShop(sellerId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (!product.getShopId().equals(shop.getId())) {
            throw new UnauthorizedException("This product is not in your shop");
        }
        productRepository.delete(product);
    }

    // ---- Customer-facing (public) methods ----

    public List<Product> getAllAvailableProducts(String category) {
        if (category != null && !category.isBlank()) {
            return productRepository.findByCategoryIgnoreCaseAndIsAvailableTrue(category);
        }
        return productRepository.findByIsAvailableTrue();
    }

    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
    }
}