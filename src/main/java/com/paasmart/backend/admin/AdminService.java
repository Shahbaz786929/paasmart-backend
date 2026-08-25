package com.paasmart.backend.admin;

import com.paasmart.backend.admin.dto.DashboardStats;
import com.paasmart.backend.admin.dto.ShopRejectRequest;
import com.paasmart.backend.auth.User;
import com.paasmart.backend.auth.UserRepository;
import com.paasmart.backend.exception.BadRequestExceprion;
import com.paasmart.backend.exception.ResourceNotFoundException;
import com.paasmart.backend.exception.UnauthorizedException;
import com.paasmart.backend.notification.Notification;
import com.paasmart.backend.notification.NotificationService;
import com.paasmart.backend.notification.PushNotificationService;
import com.paasmart.backend.order.Order;
import com.paasmart.backend.order.OrderRepository;
import com.paasmart.backend.seller.Shop;
import com.paasmart.backend.seller.ShopRepository;
import com.paasmart.backend.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminService.class);

    @Autowired private ShopRepository shopRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private PushNotificationService pushNotificationService;
    @Autowired private NotificationService notificationService;

    // ---- Seller/Shop approval ----
    // Note: PENDING shops need approval regardless of caller -- ADMIN sees all,
    // TENANT_ADMIN only sees/approves their own city's shops.

    public List<Shop> getPendingShops() {
        Long tenantId = TenantContext.getTenantId();
        return tenantId == null
                ? shopRepository.findByStatus(Shop.Status.PENDING)
                : shopRepository.findByStatusAndTenantId(Shop.Status.PENDING, tenantId);
    }

    public List<Shop> getAllShops() {
        Long tenantId = TenantContext.getTenantId();
        return tenantId == null ? shopRepository.findAll() : shopRepository.findByTenantId(tenantId);
    }

    public Shop approveShop(Long shopId) {
        Shop shop = getShopScoped(shopId);

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
        Shop shop = getShopScoped(shopId);

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
        Shop shop = getShopScoped(shopId);

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

    // Fetches a shop, but if caller is a TENANT_ADMIN, makes sure the shop
    // actually belongs to their own city -- prevents cross-city tampering.
    private Shop getShopScoped(Long shopId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !shop.getTenant().getId().equals(tenantId)) {
            throw new UnauthorizedException("This shop belongs to a different city");
        }
        return shop;
    }

    private void notifySeller(Long sellerId, String title, String body) {
        notificationService.notify(sellerId, Notification.Type.SHOP_STATUS, title, body, null);
    }

    // ---- User management ----

    public List<User> getAllUsers() {
        Long tenantId = TenantContext.getTenantId();
        return tenantId == null ? userRepository.findAll() : userRepository.findByTenantId(tenantId);
    }

    public User blockUser(Long userId) {
        User user = getUserScoped(userId);
        user.setStatus(User.Status.BANNED);
        return userRepository.save(user);
    }

    public User unblockUser(Long userId) {
        User user = getUserScoped(userId);
        user.setStatus(User.Status.ACTIVE);
        return userRepository.save(user);
    }

    private User getUserScoped(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !user.getTenant().getId().equals(tenantId)) {
            throw new UnauthorizedException("This user belongs to a different city");
        }
        return user;
    }

    // ---- Orders monitor ----

    public List<Order> getAllOrders() {
        Long tenantId = TenantContext.getTenantId();
        return tenantId == null
                ? orderRepository.findAllByOrderByCreatedAtDesc()
                : orderRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
    }

    // ---- Dashboard ----

    public DashboardStats getDashboardStats() {
        Long tenantId = TenantContext.getTenantId();
        DashboardStats stats = new DashboardStats();

        if (tenantId == null) {
            // ADMIN -- platform-wide numbers
            stats.setTotalUsers(userRepository.count());
            stats.setTotalCustomers(userRepository.countByRole(User.Role.CUSTOMER));
            stats.setTotalSellers(userRepository.countByRole(User.Role.SELLER));
            stats.setTotalDeliveryBoys(userRepository.countByRole(User.Role.DELIVERY));
            stats.setPendingShops(shopRepository.findByStatus(Shop.Status.PENDING).size());
            stats.setApprovedShops(shopRepository.findByStatus(Shop.Status.APPROVED).size());
            stats.setTotalOrders(orderRepository.count());
            stats.setOrdersToday(orderRepository.countByCreatedAtAfter(LocalDate.now().atStartOfDay()));
            stats.setTotalRevenue(orderRepository.getTotalRevenue());
        } else {
            // TENANT_ADMIN -- only their own city's numbers
            stats.setTotalUsers(userRepository.countByTenantId(tenantId));
            stats.setTotalCustomers(userRepository.countByTenantIdAndRole(tenantId, User.Role.CUSTOMER));
            stats.setTotalSellers(userRepository.countByTenantIdAndRole(tenantId, User.Role.SELLER));
            stats.setTotalDeliveryBoys(userRepository.countByTenantIdAndRole(tenantId, User.Role.DELIVERY));
            stats.setPendingShops(shopRepository.findByStatusAndTenantId(Shop.Status.PENDING, tenantId).size());
            stats.setApprovedShops(shopRepository.findByStatusAndTenantId(Shop.Status.APPROVED, tenantId).size());
            stats.setTotalOrders(orderRepository.countByTenantId(tenantId));
            stats.setOrdersToday(orderRepository.countByTenantIdAndCreatedAtAfter(tenantId, LocalDate.now().atStartOfDay()));
            stats.setTotalRevenue(orderRepository.getTenantTotalRevenue(tenantId));
        }

        return stats;
    }

    public Shop updateShopDeliveryRadius(Long shopId, Double radiusKm) {
        if (radiusKm == null || radiusKm <= 0) {
            throw new BadRequestExceprion("Delivery radius must be a positive number");
        }

        Shop shop = getShopScoped(shopId);
        shop.setDeliveryRadiusKm(radiusKm);
        return shopRepository.save(shop);
    }
}