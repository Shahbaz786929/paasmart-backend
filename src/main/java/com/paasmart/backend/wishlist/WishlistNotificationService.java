package com.paasmart.backend.wishlist;

import com.paasmart.backend.auth.User;
import com.paasmart.backend.notification.PushNotificationService;
import com.paasmart.backend.product.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WishlistNotificationService {

    @Autowired private WishlistRepository wishlistRepository;
    @Autowired private PushNotificationService pushNotificationService;

    // Jab bhi seller stock 0 se upar badhaye, ise call karo
    public void notifyBackInStock(Product product) {
        try {
            for (Wishlist wishlist : wishlistRepository.findByProductId(product.getId())) {
                User customer = wishlist.getCustomer();
                if (customer != null && customer.getPushToken() != null) {
                    pushNotificationService.send(
                            customer.getPushToken(),
                            "Back in Stock!",
                            product.getName() + " is back in stock. Order now before it's gone!"
                    );
                }
            }
        } catch (Exception e) {
            System.out.println("Wishlist notification failed: " + e.getMessage());
        }
    }
}