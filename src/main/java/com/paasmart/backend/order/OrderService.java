package com.paasmart.backend.order;

import com.paasmart.backend.auth.User;
import com.paasmart.backend.auth.UserRepository;
import com.paasmart.backend.exception.BadRequestExceprion;
import com.paasmart.backend.exception.ResourceNotFoundException;
import com.paasmart.backend.exception.UnauthorizedException;
import com.paasmart.backend.notification.Notification;
import com.paasmart.backend.notification.NotificationService;
import com.paasmart.backend.notification.PushNotificationService;
import com.paasmart.backend.order.dto.DeliveryLocationResponse;
import com.paasmart.backend.order.dto.OrderItemRequest;
import com.paasmart.backend.order.dto.PlaceOrderRequest;
import com.paasmart.backend.product.Product;
import com.paasmart.backend.product.ProductRepository;
import com.paasmart.backend.seller.Shop;
import com.paasmart.backend.seller.ShopRepository;
import com.paasmart.backend.tenant.TenantContext;
// import com.paasmart.backend.telephony.TelephonyService; // Masked calling — baad me chahiye ho to uncomment karo
import com.paasmart.backend.wallet.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderItemRepository orderItemRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private ShopRepository shopRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PushNotificationService pushNotificationService;
    @Autowired private WalletService walletService;
    @Autowired private OrderStatusHistoryRepository orderStatusHistoryRepository;
    @Autowired private NotificationService notificationService;
    // @Autowired private TelephonyService telephonyService; // Masked calling — baad me chahiye ho to uncomment karo

    // forward-only status flow
    private static final List<Order.Status> FLOW = List.of(
            Order.Status.PLACED, Order.Status.CONFIRMED, Order.Status.PREPARING,
            Order.Status.READY_FOR_PICKUP, Order.Status.PICKED_UP, Order.Status.IN_TRANSIT,
            Order.Status.DELIVERED, Order.Status.COMPLETED
    );

    @Transactional
    public Order placeOrder(Long customerId, PlaceOrderRequest req) {
        Long shopId = null;
        BigDecimal total = BigDecimal.ZERO;

        for (OrderItemRequest item : req.getItems()) {
            Product product = productRepository.findByIdAndTenantId(item.getProductId(), TenantContext.getTenantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product ID " + item.getProductId() + " Not Found"));

            if (shopId == null) {
                shopId = product.getShopId();
            } else if (!shopId.equals(product.getShopId())) {
                throw new BadRequestExceprion("Only products from a single shop can be included in one order.");
            }

            if (!Boolean.TRUE.equals(product.getIsAvailable())) {
                throw new BadRequestExceprion(product.getName() + " It's not available right now");
            }
            if (product.getStockQty() < item.getQuantity()) {
                throw new BadRequestExceprion(product.getName() + " It's low in stock.");
            }
        }

        Order order = new Order();
        order.setCustomerId(customerId);
        order.setShopId(shopId);
        order.setTenantId(TenantContext.getTenantId());
        order.setDeliveryAddress(req.getDeliveryAddress());
        order.setDeliveryLat(req.getDeliveryLat());
        order.setDeliveryLng(req.getDeliveryLng());
        order.setPaymentMode(Order.PaymentMode.valueOf(req.getPaymentMode().toUpperCase()));
        order.setStatus(Order.Status.PLACED);

        order.setOtp(String.format("%04d", new java.util.Random().nextInt(9999)));

        order = orderRepository.save(order);
        logStatusHistory(order.getId(), Order.Status.PLACED, customerId, "Order placed by customer");

        for (OrderItemRequest item : req.getItems()) {
            Product product = productRepository.findByIdAndTenantId(item.getProductId(), TenantContext.getTenantId()).get();

            BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            total = total.add(subtotal);

            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(order.getId());
            orderItem.setProductId(product.getId());
            orderItem.setQuantity(item.getQuantity());
            orderItem.setUnitPrice(product.getPrice());
            orderItem.setSubtotal(subtotal);
            orderItemRepository.save(orderItem);

            product.setStockQty(product.getStockQty() - item.getQuantity());
            productRepository.save(product);
        }

        order.setTotalAmount(total);

        final Order finalOrder = order;
        Shop shopForNotify = shopRepository.findById(shopId).orElse(null);
        if (shopForNotify != null) {
            notificationService.notify(
                    shopForNotify.getSellerId(),
                    Notification.Type.NEW_ORDER,
                    "New Order!",
                    "You have received a new order — ₹" + finalOrder.getTotalAmount(),
                    finalOrder.getId()
            );
        }

        return orderRepository.save(order);
    }

    public List<Order> getMyOrders(Long customerId) {
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    public List<OrderItem> getOrderItems(Long orderId) {
        return orderItemRepository.findByOrderId(orderId);
    }

    public Order getOrderById(Long orderId, Long requesterId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found."));

        boolean isCustomer = order.getCustomerId().equals(requesterId);
        boolean isSellerOfShop = shopRepository.findById(order.getShopId())
                .map(shop -> shop.getSellerId().equals(requesterId))
                .orElse(false);

        if (!isCustomer && !isSellerOfShop) {
            throw new UnauthorizedException("This is not your order.");
        }
        return order;
    }

    public void cancelOrder(Long orderId, Long customerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.getCustomerId().equals(customerId)) {
            throw new UnauthorizedException("This is not your order");
        }
        if (order.getStatus() != Order.Status.PLACED && order.getStatus() != Order.Status.CONFIRMED) {
            throw new BadRequestExceprion("This order can no longer be canceled because it has already been packed");
        }
        order.setStatus(Order.Status.CANCELLED);
        orderRepository.save(order);
        logStatusHistory(orderId, Order.Status.CANCELLED, customerId, "Cancelled by customer");
    }

    // ---- Seller-side ----

    public List<Order> getShopOrders(Long sellerId) {
        Shop shop = shopRepository.findBySellerId(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("You don't have any registered shops."));
        return orderRepository.findByShopIdOrderByCreatedAtDesc(shop.getId());
    }

    public Order updateStatus(Long sellerId, Long orderId, String newStatusStr) {
        Shop shop = shopRepository.findBySellerId(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("You don't have any registered shops."));

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.getShopId().equals(shop.getId())) {
            throw new UnauthorizedException("This order doesn't belong to your shop.");
        }

        Order.Status newStatus;
        try {
            newStatus = Order.Status.valueOf(newStatusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestExceprion("This status is invalid.");
        }

        if (newStatus == Order.Status.CANCELLED) {
            order.setStatus(Order.Status.CANCELLED);
            order = orderRepository.save(order);
            logStatusHistory(orderId, Order.Status.CANCELLED, sellerId, "Cancelled by seller");
            return order;
        }

        int currentIndex = FLOW.indexOf(order.getStatus());
        int newIndex = FLOW.indexOf(newStatus);

        if (newIndex != currentIndex + 1) {
            throw new BadRequestExceprion("The order status must be updated sequentially. You cannot skip any status");
        }

        order.setStatus(newStatus);
        if (newStatus == Order.Status.DELIVERED) {
            order.setDeliveredAt(java.time.LocalDateTime.now());
        }
        order = orderRepository.save(order);
        logStatusHistory(orderId, newStatus, sellerId, "Updated by seller");
        notifyCustomer(order, "Order Update", "Your order #" + order.getId() + " is now " + newStatus.name());

        if (newStatus == Order.Status.READY_FOR_PICKUP) {
            notifyAvailableDeliveryBoys(order);
        }

        if (newStatus == Order.Status.DELIVERED) {
            handleReferralBonus(order.getCustomerId());
        }

        return order;
    }

    // Order pickup ke liye ready hote hi tenant ke saare on-duty delivery boys ko notify karo
    private void notifyAvailableDeliveryBoys(Order order) {
        List<User> deliveryBoys = userRepository.findByRoleAndTenant_IdAndOnDuty(
                User.Role.DELIVERY, order.getTenantId(), true);

        for (User boy : deliveryBoys) {
            notificationService.notify(
                    boy.getId(),
                    Notification.Type.NEW_DELIVERY,
                    "New Order Available!",
                    "Order #" + order.getId() + " ready for pickup — ₹" + order.getDeliveryFee() + " delivery fee",
                    order.getId()
            );
        }
    }

    private void handleReferralBonus(Long customerId) {
        try {
            long deliveredOrdersCount = orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                    .filter(o -> o.getStatus() == Order.Status.DELIVERED || o.getStatus() == Order.Status.COMPLETED)
                    .count();

            if (deliveredOrdersCount != 1) return;

            User customer = userRepository.findById(customerId).orElse(null);
            if (customer == null || customer.getReferredBy() == null) return;

            walletService.credit(customer.getReferredBy(), java.math.BigDecimal.valueOf(50),
                    com.paasmart.backend.wallet.WalletTransaction.Type.REFERRAL_BONUS,
                    "Referral bonus — your friend placed their first order", null);

            walletService.credit(customerId, java.math.BigDecimal.valueOf(25),
                    com.paasmart.backend.wallet.WalletTransaction.Type.WELCOME_BONUS,
                    "Welcome bonus for your first order", null);
        } catch (Exception e) {
            log.warn("Referral bonus failed", e);
        }
    }

    public Map<String, String> getCustomerBasicInfo(Long orderId, Long requesterId) {
        Order order = getOrderById(orderId, requesterId);
        User customer = userRepository.findById(order.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        return Map.of("name", customer.getName(), "phone", customer.getPhone());
    }

    // == DELIVERY BOY METHODS ==

    public List<Order> getAvailableOrdersForDelivery(Long deliveryBoyId) {
        User deliveryBoy = userRepository.findById(deliveryBoyId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery partner not found"));

        if (!Boolean.TRUE.equals(deliveryBoy.getOnDuty())) {
            return List.of();
        }

        Long tenantId = deliveryBoy.getTenant() != null ? deliveryBoy.getTenant().getId() : null;
        return orderRepository.findByStatusAndDeliveryBoyIdIsNullAndTenantId(Order.Status.READY_FOR_PICKUP, tenantId);
    }

    public User setDeliveryDuty(Long deliveryBoyId, boolean onDuty) {
        User deliveryBoy = userRepository.findById(deliveryBoyId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery partner not found"));
        deliveryBoy.setOnDuty(onDuty);
        return userRepository.save(deliveryBoy);
    }

    public Boolean getDeliveryDuty(Long deliveryBoyId) {
        User deliveryBoy = userRepository.findById(deliveryBoyId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery partner not found"));
        return deliveryBoy.getOnDuty();
    }

    public Order acceptOrderForDelivery(Long deliveryBoyId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getStatus() != Order.Status.READY_FOR_PICKUP) {
            throw new BadRequestExceprion("This order is not ready for pickup yet");
        }
        if (order.getDeliveryBoyId() != null) {
            throw new BadRequestExceprion("This order has already been accepted by another delivery partner");
        }

        order.setDeliveryBoyId(deliveryBoyId);
        return orderRepository.save(order);
    }

    public List<Order> getMyDeliveries(Long deliveryBoyId) {
        return orderRepository.findByDeliveryBoyIdOrderByCreatedAtDesc(deliveryBoyId);
    }

    private Order getOwnedDeliveryOrder(Long deliveryBoyId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (order.getDeliveryBoyId() == null || !order.getDeliveryBoyId().equals(deliveryBoyId)) {
            throw new UnauthorizedException("This order is not assigned to you");
        }
        return order;
    }

    public Order getDeliveryOrderDetail(Long deliveryBoyId, Long orderId) {
        return getOwnedDeliveryOrder(deliveryBoyId, orderId);
    }

    // Delivery boy ko customer ka naam + phone dikhana (sirf apne assigned order ke liye)
    public Map<String, String> getCustomerInfoForDelivery(Long deliveryBoyId, Long orderId) {
        Order order = getOwnedDeliveryOrder(deliveryBoyId, orderId);
        User customer = userRepository.findById(order.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        return Map.of("name", customer.getName(), "phone", customer.getPhone());
    }

    // Seller (shop owner) ka naam + shop ka naam + phone
    public Map<String, String> getSellerInfoForDelivery(Long deliveryBoyId, Long orderId) {
        Order order = getOwnedDeliveryOrder(deliveryBoyId, orderId);
        Shop shop = shopRepository.findById(order.getShopId())
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found"));
        User seller = userRepository.findById(shop.getSellerId())
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found"));
        return Map.of("name", seller.getName(), "shopName", shop.getShopName(), "phone", seller.getPhone());
    }

    /*
     * ===== MASKED CALLING (Exotel) — ABHI USE NAHI HO RAHA =====
     * Jab Exotel account ban jaaye (paid — Exophone/virtual number chahiye), to:
     *   1) Upar wale TelephonyService import + @Autowired field uncomment karo
     *   2) Neeche wale 2 methods uncomment karo
     *   3) getCustomerInfoForDelivery() se "phone" hata do
     *   4) getSellerInfoForDelivery() se "phone" hata do
     *   5) DeliveryController.java me call-customer/call-seller endpoints uncomment karo
     *   6) Frontend me dobara masked-call wala version use karo
     *
    public Map<String, String> callCustomerMasked(Long deliveryBoyId, Long orderId) {
        Order order = getOwnedDeliveryOrder(deliveryBoyId, orderId);
        User deliveryBoy = userRepository.findById(deliveryBoyId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery partner not found"));
        User customer = userRepository.findById(order.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        return telephonyService.bridgeCall(deliveryBoy.getPhone(), customer.getPhone());
    }

    public Map<String, String> callSellerMasked(Long deliveryBoyId, Long orderId) {
        Order order = getOwnedDeliveryOrder(deliveryBoyId, orderId);
        User deliveryBoy = userRepository.findById(deliveryBoyId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery partner not found"));
        Shop shop = shopRepository.findById(order.getShopId())
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found"));
        User seller = userRepository.findById(shop.getSellerId())
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found"));
        return telephonyService.bridgeCall(deliveryBoy.getPhone(), seller.getPhone());
    }
    */

    public Order markPickedUp(Long deliveryBoyId, Long orderId) {
        Order order = getOwnedDeliveryOrder(deliveryBoyId, orderId);
        if (order.getStatus() != Order.Status.READY_FOR_PICKUP) {
            throw new BadRequestExceprion("Order must be ready for pickup first");
        }
        order.setStatus(Order.Status.PICKED_UP);
        order = orderRepository.save(order);
        logStatusHistory(orderId, Order.Status.PICKED_UP, deliveryBoyId, "Picked up by delivery partner");
        notifyCustomer(order, "Order Picked Up", "Your order #" + order.getId() + " has been picked up and is on its way!");
        return order;
    }

    public Order markInTransit(Long deliveryBoyId, Long orderId) {
        Order order = getOwnedDeliveryOrder(deliveryBoyId, orderId);
        if (order.getStatus() != Order.Status.PICKED_UP) {
            throw new BadRequestExceprion("Order must be picked up first");
        }
        order.setStatus(Order.Status.IN_TRANSIT);
        order = orderRepository.save(order);
        logStatusHistory(orderId, Order.Status.IN_TRANSIT, deliveryBoyId, "Out for delivery");
        notifyCustomer(order, "Out for Delivery", "Your order #" + order.getId() + " is out for delivery!");
        return order;
    }

    private void notifyCustomer(Order order, String title, String body) {
        notificationService.notify(order.getCustomerId(), Notification.Type.ORDER_UPDATE, title, body, order.getId());
    }

    public Order confirmDelivery(Long deliveryBoyId, Long orderId, String otp) {
        Order order = getOwnedDeliveryOrder(deliveryBoyId, orderId);

        if (order.getStatus() != Order.Status.IN_TRANSIT && order.getStatus() != Order.Status.PICKED_UP) {
            throw new BadRequestExceprion("Order is not out for delivery yet");
        }
        if (order.getOtp() == null || !order.getOtp().equals(otp)) {
            throw new BadRequestExceprion("Invalid OTP");
        }

        order.setStatus(Order.Status.DELIVERED);
        order.setDeliveredAt(java.time.LocalDateTime.now());
        order = orderRepository.save(order);
        logStatusHistory(orderId, Order.Status.DELIVERED, deliveryBoyId, "Delivered successfully");
        notifyCustomer(order, "Order Delivered", "Your order #" + order.getId() + " has been delivered. Enjoy!");

        walletService.credit(
                deliveryBoyId,
                order.getDeliveryFee(),
                com.paasmart.backend.wallet.WalletTransaction.Type.DELIVERY_EARNING,
                "Delivery earning for order #" + order.getId(),
                order.getId()
        );
        return order;
    }

    private void logStatusHistory(Long orderId, Order.Status status, Long changedById, String note) {
        try {
            OrderStatusHistory history = new OrderStatusHistory();
            history.setOrderId(orderId);
            history.setStatus(status);
            history.setChangedById(changedById);
            history.setNote(note);
            orderStatusHistoryRepository.save(history);
        } catch (Exception e) {
            log.warn("Failed to log order status history", e);
        }
    }

    public List<OrderStatusHistory> getOrderTimeline(Long orderId, Long requesterId) {
        getOrderById(orderId, requesterId);
        return orderStatusHistoryRepository.findByOrderIdOrderByCreatedAtAsc(orderId);
    }

    // Customer ke liye — delivery boy ka current location, sirf tab jab order actually
// pickup ho chuka ho aur ek delivery boy assign ho (privacy: baaki states me kuch nahi dikhega)
    public DeliveryLocationResponse getDeliveryLocation(Order order) {
        boolean isTrackable = order.getDeliveryBoyId() != null
                && (order.getStatus() == Order.Status.PICKED_UP || order.getStatus() == Order.Status.IN_TRANSIT);

        if (!isTrackable) {
            return new DeliveryLocationResponse(null, null, null, false);
        }

        com.paasmart.backend.auth.User deliveryBoy = userRepository.findById(order.getDeliveryBoyId()).orElse(null);
        if (deliveryBoy == null || deliveryBoy.getCurrentLat() == null || deliveryBoy.getCurrentLng() == null) {
            return new DeliveryLocationResponse(null, null, null, false);
        }

        return new DeliveryLocationResponse(deliveryBoy.getCurrentLat(), deliveryBoy.getCurrentLng(), deliveryBoy.getLocationUpdatedAt(), true);
    }
}