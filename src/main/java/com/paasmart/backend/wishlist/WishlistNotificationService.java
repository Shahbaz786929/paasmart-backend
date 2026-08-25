package com.paasmart.backend.wishlist;

import com.paasmart.backend.auth.User;
import com.paasmart.backend.notification.Notification;
import com.paasmart.backend.notification.NotificationService;
import com.paasmart.backend.product.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class WishlistNotificationService {

    private static final Logger log = LoggerFactory.getLogger(WishlistNotificationService.class);

    @Autowired private WishlistRepository wishlistRepository;
    @Autowired private NotificationService notificationService;

    // Jab bhi seller stock 0 se upar badhaye, ise call karo
    public void notifyBackInStock(Product product) {
        try {
            for (Wishlist wishlist : wishlistRepository.findByProductId(product.getId())) {
                User customer = wishlist.getCustomer();
                if (customer != null) {
                    notificationService.notify(
                            customer.getId(),
                            Notification.Type.WISHLIST,
                            "Back in Stock!",
                            product.getName() + " is back in stock. Order now before it's gone!",
                            product.getId()
                    );
                }
            }
        } catch (Exception e) {
            log.warn("Wishlist notification failed", e);
        }
    }
}