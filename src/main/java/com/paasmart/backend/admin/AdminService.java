package com.paasmart.backend.admin;

import com.paasmart.backend.admin.dto.DashboardStats;
import com.paasmart.backend.admin.dto.ShopRejectRequest;
import com.paasmart.backend.auth.User;
import com.paasmart.backend.exception.BadRequestExceprion;
import com.paasmart.backend.exception.ResourceNotFoundException;
import com.paasmart.backend.notification.PushNotificationService;
import com.paasmart.backend.order.Order;
import com.paasmart.backend.order.OrderRepository;
import com.paasmart.backend.auth.UserRepository;
import com.paasmart.backend.seller.Shop;
import com.paasmart.backend.seller.ShopRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdminService {

    @Autowired private ShopRepository shopRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private PushNotificationService pushNotificationService;

    // ---- Seller/Shop approval ----

    public List<Shop> getPendingShops() {
        return shopRepository.findByStatus(Shop.Status.PENDING);
    }

    public List<Shop> getAllShops() {
        return shopRepository.findAll();
    }

    public Shop approveShop(Long shopId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found"));

        if (shop.getStatus() != Shop.Status.PENDING) {
            throw new BadRequestExceprion("Only pending shops can be approved");
        }

        shop.setStatus(Shop.Status.APPROVED);
        shop.setRejectionReason(null);
        Shop saved = shopRepository.save(shop);

        notifySeller(shop.getSellerId(), "Shop Approved!",
                "Congratulations! Your shop \"" + shop.getShopName() + "\" has been approved. You can now start adding products.");

        return saved;
    }

    public Shop rejectShop(Long shopId, ShopRejectRequest request) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found"));

        if (shop.getStatus() != Shop.Status.PENDING) {
            throw new BadRequestExceprion("Only pending shops can be rejected");
        }

        shop.setStatus(Shop.Status.REJECTED);
        shop.setRejectionReason(request.getReason());
        Shop saved = shopRepository.save(shop);

        notifySeller(shop.getSellerId(), "Shop Application Rejected",
                "Your shop application was rejected. Reason: " + request.getReason() + ". You can update your documents and re-apply.");

        return saved;
    }

    public Shop suspendShop(Long shopId, ShopRejectRequest request) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found"));

        if (shop.getStatus() != Shop.Status.APPROVED) {
            throw new BadRequestExceprion("Only approved shops can be suspended");
        }

        shop.setStatus(Shop.Status.SUSPENDED);
        shop.setRejectionReason(request.getReason());
        Shop saved = shopRepository.save(shop);

        notifySeller(shop.getSellerId(), "Shop Suspended",
                "Your shop has been suspended. Reason: " + request.getReason() + ". Please contact support.");

        return saved;
    }

    private void notifySeller(Long sellerId, String title, String body) {
        try {
            User seller = userRepository.findById(sellerId).orElse(null);
            if (seller != null && seller.getPushToken() != null) {
                pushNotificationService.send(seller.getPushToken(), title, body);
            }
        } catch (Exception e) {
            System.out.println("Seller notification failed: " + e.getMessage());
        }
    }

    // ---- User management ----

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User blockUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setStatus(User.Status.BANNED);
        return userRepository.save(user);
    }

    public User unblockUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setStatus(User.Status.ACTIVE);
        return userRepository.save(user);
    }

    // ---- Orders monitor ----

    public List<Order> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc();
    }

    // ---- Dashboard ----

    public DashboardStats getDashboardStats() {
        DashboardStats stats = new DashboardStats();

        stats.setTotalUsers(userRepository.count());
        stats.setTotalCustomers(userRepository.countByRole(User.Role.CUSTOMER));
        stats.setTotalSellers(userRepository.countByRole(User.Role.SELLER));
        stats.setTotalDeliveryBoys(userRepository.countByRole(User.Role.DELIVERY));

        stats.setPendingShops(shopRepository.findByStatus(Shop.Status.PENDING).size());
        stats.setApprovedShops(shopRepository.findByStatus(Shop.Status.APPROVED).size());

        stats.setTotalOrders(orderRepository.count());

        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        stats.setOrdersToday(orderRepository.countByCreatedAtAfter(startOfToday));

        stats.setTotalRevenue(orderRepository.getTotalRevenue());

        return stats;
    }
}