package com.paasmart.backend.checkout;

import com.paasmart.backend.address.Address;
import com.paasmart.backend.address.AddressRepository;
import com.paasmart.backend.auth.User;
import com.paasmart.backend.auth.UserRepository;
import com.paasmart.backend.cart.Cart;
import com.paasmart.backend.cart.CartRepository;
import com.paasmart.backend.exception.ResourceNotFoundException;
import com.paasmart.backend.exception.TooManyRequestsException;
import com.paasmart.backend.product.Product;
import com.paasmart.backend.product.ProductRepository;
import com.paasmart.backend.ratelimit.RateLimitService;
import com.paasmart.backend.seller.GroUtils;
import com.paasmart.backend.seller.Shop;
import com.paasmart.backend.seller.ShopRepository;
import com.paasmart.backend.settings.SettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/checkout")
public class CheckoutController {

    @Autowired private CheckoutService checkoutService;
    @Autowired private AddressRepository addressRepository;
    @Autowired private CartRepository cartRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private ShopRepository shopRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private SettingsService settingsService;
    @Autowired private RateLimitService rateLimitService;

    private Long currentUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @PostMapping
    public ResponseEntity<CheckoutResponse> placeOrder(@RequestBody CheckoutRequest request) {
        Long uid = (Long) org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getPrincipal();
        if (!rateLimitService.tryConsume("checkout:" + uid, 20)) {
            throw new TooManyRequestsException("Too many order attempts. Please wait a few minutes and try again.");
        }
        return ResponseEntity.ok(checkoutService.placeOrder(currentUserId(), request));
    }

    // Cart/Checkout screen ke liye — order place karne se PEHLE actual delivery
// fee dikhane ke liye (bilkul wahi formula jo checkout charge karega)
    @GetMapping("/delivery-fee-preview")
    public ResponseEntity<?> previewDeliveryFee(@RequestParam Long addressId) {
        Long customerId = currentUserId();

        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        if (!address.getCustomer().getId().equals(customerId)) {
            throw new com.paasmart.backend.exception.UnauthorizedException("This is not your address");
        }

        List<Cart> cartItems = cartRepository.findByCustomer(customer);
        if (cartItems.isEmpty()) {
            return ResponseEntity.ok(Map.of("deliveryFee", BigDecimal.ZERO));
        }

        Product firstProduct = productRepository.findById(cartItems.get(0).getProduct().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        Shop shop = shopRepository.findById(firstProduct.getShopId())
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found"));

        Double distanceKm = GroUtils.distanceKmOrNull(shop.getLatitude(), shop.getLongitude(), address.getLatitude(), address.getLongitude());
        BigDecimal fee = settingsService.calculateDeliveryFee(distanceKm != null ? distanceKm : 1.0);

        return ResponseEntity.ok(Map.of("deliveryFee", fee, "distanceKm", distanceKm != null ? distanceKm : 0));
    }
}