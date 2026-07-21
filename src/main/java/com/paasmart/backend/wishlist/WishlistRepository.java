package com.paasmart.backend.wishlist;

import com.paasmart.backend.auth.User;
import com.paasmart.backend.product.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    List<Wishlist> findByCustomer(User customer);

    Optional<Wishlist> findByCustomerAndProduct(User customer, Product product);
    List<Wishlist> findByProductId(Long productId);

}