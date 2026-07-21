package com.paasmart.backend.cart;

import com.paasmart.backend.auth.User;
import com.paasmart.backend.auth.UserRepository;
import com.paasmart.backend.exception.ResourceNotFoundException;
import com.paasmart.backend.exception.UnauthorizedException;
import com.paasmart.backend.product.Product;
import com.paasmart.backend.product.ProductRepository;
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

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product Not Found"));

        Cart cart = cartRepository.findByCustomer(customer).stream()
                .filter(c -> c.getProduct().getId().equals(product.getId()))
                .findFirst()
                .orElse(null);

        if (cart == null) {
            cart = new Cart();
            cart.setCustomer(customer);
            cart.setProduct(product);
            cart.setQuantity(request.getQuantity());
        } else {
            cart.setQuantity(cart.getQuantity() + request.getQuantity());
        }

        BigDecimal price = product.getPrice();
        cart.setPrice(price);
        cart.setTotalPrice(price.multiply(BigDecimal.valueOf(cart.getQuantity())));

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