package com.paasmart.backend.wishlist;

import com.paasmart.backend.auth.User;
import com.paasmart.backend.auth.UserRepository;
import com.paasmart.backend.exception.ResourceNotFoundException;
import com.paasmart.backend.product.Product;
import com.paasmart.backend.product.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WishlistServiceImpl implements WishlistService {

    @Autowired private WishlistRepository wishlistRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ProductRepository productRepository;

    @Override
    public Wishlist addToWishlist(Long customerId, AddWishlistRequest request) {

        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer Not Found"));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product Not Found"));

        wishlistRepository.findByCustomerAndProduct(customer, product)
                .ifPresent(w -> {
                    throw new RuntimeException("Product already exists in wishlist");
                });

        Wishlist wishlist = new Wishlist();

        wishlist.setCustomer(customer);
        wishlist.setProduct(product);

        return wishlistRepository.save(wishlist);
    }

    @Override
    public List<Wishlist> getWishlist(Long customerId) {

        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer Not Found"));

        return wishlistRepository.findByCustomer(customer);
    }

    @Override
    public void removeWishlist(Long customerId, Long wishlistId) {

        Wishlist wishlist = wishlistRepository.findById(wishlistId)
                .orElseThrow(() -> new ResourceNotFoundException("Wishlist Item Not Found"));

        if (!wishlist.getCustomer().getId().equals(customerId)) {
            throw new com.paasmart.backend.exception.UnauthorizedException("This is not your wishlist item");
        }

        wishlistRepository.delete(wishlist);
    }
}