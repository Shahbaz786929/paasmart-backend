package com.paasmart.backend.cart;

import com.paasmart.backend.auth.User;
import com.paasmart.backend.auth.UserRepository;
import com.paasmart.backend.exception.BadRequestExceprion;
import com.paasmart.backend.exception.ResourceNotFoundException;
import com.paasmart.backend.exception.UnauthorizedException;
import com.paasmart.backend.product.Product;
import com.paasmart.backend.product.ProductRepository;
import com.paasmart.backend.tenant.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements CartService {

    @Autowired private CartRepository cartRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private com.paasmart.backend.flashsale.FlashSaleService flashSaleService;

    private CartResponse toResponse(Cart cart) {
        CartResponse response = new CartResponse();
        response.setCartId(cart.getId());
        response.setProductId(cart.getProduct().getId());
        response.setProductName(cart.getProduct().getName());
        response.setQuantity(cart.getQuantity());
        response.setPrice(cart.getPrice());
        response.setTotalPrice(cart.getTotalPrice());
        return response;
    }

    @Override
    public CartResponse addToCart(Long customerId, AddToCartRequest request) {

        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer Not Found"));

        Product product = productRepository.findByIdAndTenantId(request.getProductId(), TenantContext.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Product Not Found"));

        int qty = request.getQuantity() == null ? 0 : request.getQuantity();
        if (qty < 1) {
            throw new BadRequestExceprion("Quantity must be at least 1");
        }
        if (!Boolean.TRUE.equals(product.getIsAvailable())) {
            throw new BadRequestExceprion("This product is currently unavailable");
        }

        List<Cart> existingItems = cartRepository.findByCustomer(customer);

        // Ek order = ek shop. Dusri shop ka item cart me ho to add nahi hoga.
        boolean hasOtherShop = existingItems.stream()
                .anyMatch(c -> !java.util.Objects.equals(c.getProduct().getShopId(), product.getShopId()));
        if (hasOtherShop) {
            throw new BadRequestExceprion(
                    "Your cart has items from another shop. Please clear your cart or place that order first.");
        }

        Cart cart = existingItems.stream()
                .filter(c -> c.getProduct().getId().equals(product.getId()))
                .findFirst()
                .orElse(null);

        int newQty = (cart == null ? 0 : cart.getQuantity()) + qty;
        if (product.getStockQty() != null && newQty > product.getStockQty()) {
            throw new BadRequestExceprion("Only " + product.getStockQty() + " item(s) available in stock");
        }

        if (cart == null) {
            cart = new Cart();
            cart.setCustomer(customer);
            cart.setProduct(product);
        }
        cart.setQuantity(newQty);

        BigDecimal price = flashSaleService.getEffectivePrice(product);
        cart.setPrice(price);
        cart.setTotalPrice(price.multiply(BigDecimal.valueOf(newQty)));

        return toResponse(cartRepository.save(cart));
    }

    @Override
    public List<CartResponse> getCustomerCart(Long customerId) {
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        return cartRepository.findByCustomer(customer).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public CartResponse updateQuantity(Long customerId, Long cartId, Integer quantity) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart Item Not Found"));

        if (!cart.getCustomer().getId().equals(customerId)) {
            throw new UnauthorizedException("This is not your cart item");
        }
        if (quantity == null || quantity < 1) {
            throw new BadRequestExceprion("Quantity must be at least 1");
        }
        Integer stock = cart.getProduct().getStockQty();
        if (stock != null && quantity > stock) {
            throw new BadRequestExceprion("Only " + stock + " item(s) available in stock");
        }
        cart.setQuantity(quantity);
        cart.setTotalPrice(cart.getPrice().multiply(BigDecimal.valueOf(quantity)));
        return toResponse(cartRepository.save(cart));
    }

    @Override
    public void removeFromCart(Long customerId, Long cartId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart Item Not Found"));

        if (!cart.getCustomer().getId().equals(customerId)) {
            throw new UnauthorizedException("This is not your cart item");
        }
        cartRepository.delete(cart);
    }

    @Override
    public void clearCart(Long customerId) {
        User user = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer Not Found"));
        cartRepository.deleteAll(cartRepository.findByCustomer(user));
    }
}