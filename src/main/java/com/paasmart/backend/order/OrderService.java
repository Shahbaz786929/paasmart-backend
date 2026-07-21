package com.paasmart.backend.order;

import com.paasmart.backend.auth.User;
import com.paasmart.backend.auth.UserRepository;
import com.paasmart.backend.exception.BadRequestExceprion;
import com.paasmart.backend.exception.ResourceNotFoundException;
import com.paasmart.backend.exception.UnauthorizedException;
import com.paasmart.backend.notification.PushNotificationService;
import com.paasmart.backend.order.dto.OrderItemRequest;
import com.paasmart.backend.order.dto.PlaceOrderRequest;
import com.paasmart.backend.product.Product;
import com.paasmart.backend.product.ProductRepository;
import com.paasmart.backend.seller.Shop;
import com.paasmart.backend.seller.ShopRepository;
import com.paasmart.backend.wallet.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
public class OrderService {

    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderItemRepository orderItemRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private ShopRepository shopRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PushNotificationService pushNotificationService;
    @Autowired private WalletService walletService;

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

        // Pehle validate karo: sab products ek hi shop se hone chahiye, stock available ho
        for (OrderItemRequest item : req.getItems()) {
            Product product = productRepository.findById(item.getProductId())
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
        order.setDeliveryAddress(req.getDeliveryAddress());
        order.setDeliveryLat(req.getDeliveryLat());
        order.setDeliveryLng(req.getDeliveryLng());
        order.setPaymentMode(Order.PaymentMode.valueOf(req.getPaymentMode().toUpperCase()));
        order.setStatus(Order.Status.PLACED);

        // Delivery OTP generate karo — Chapter 5.4 ke delivery flow mein use hoga
        order.setOtp(String.format("%04d", new java.util.Random().nextInt(9999)));

        order = orderRepository.save(order);

        for (OrderItemRequest item : req.getItems()) {
            Product product = productRepository.findById(item.getProductId()).get();

            BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            total = total.add(subtotal);

            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(order.getId());
            orderItem.setProductId(product.getId());
            orderItem.setQuantity(item.getQuantity());
            orderItem.setUnitPrice(product.getPrice());
            orderItem.setSubtotal(subtotal);
            orderItemRepository.save(orderItem);

            // Stock decrease
            product.setStockQty(product.getStockQty() - item.getQuantity());
            productRepository.save(product);
        }

        order.setTotalAmount(total);

        final Order finalOrder = order;
        Shop shopForNotify = shopRepository.findById(shopId).orElse(null);
        if (shopForNotify != null) {
            userRepository.findById(shopForNotify.getSellerId()).ifPresent(seller ->
                    pushNotificationService.send(
                            seller.getPushToken(),
                            "New Order!",
                            "You have received a new order — ₹" + finalOrder.getTotalAmount()
                    )
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
            return orderRepository.save(order);
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
        notifyCustomer(order, "Order Update", "Your order #" + order.getId() + " is now " + newStatus.name());

        if (newStatus == Order.Status.DELIVERED) {
            handleReferralBonus(order.getCustomerId());
        }

        return order;
    }

    private void handleReferralBonus(Long customerId) {
        try {
            long deliveredOrdersCount = orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                    .filter(o -> o.getStatus() == Order.Status.DELIVERED || o.getStatus() == Order.Status.COMPLETED)
                    .count();

            if (deliveredOrdersCount != 1) return;   // sirf pehle hi order pe bonus milega

            User customer = userRepository.findById(customerId).orElse(null);
            if (customer == null || customer.getReferredBy() == null) return;

            walletService.credit(customer.getReferredBy(), java.math.BigDecimal.valueOf(50),
                    com.paasmart.backend.wallet.WalletTransaction.Type.REFERRAL_BONUS,
                    "Referral bonus — your friend placed their first order", null);

            walletService.credit(customerId, java.math.BigDecimal.valueOf(25),
                    com.paasmart.backend.wallet.WalletTransaction.Type.WELCOME_BONUS,
                    "Welcome bonus for your first order", null);
        } catch (Exception e) {
            System.out.println("Referral bonus failed: " + e.getMessage());
        }
    }

    public Map<String, String> getCustomerBasicInfo(Long orderId, Long requesterId) {
        Order order = getOrderById(orderId, requesterId);
        User customer = userRepository.findById(order.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        return Map.of("name", customer.getName(), "phone", customer.getPhone());
    }

    // == DELIVERY BOY METHODS ==

    // Nearby-ready orders jo abhi tak kisi delivery boy ko assign nahi hue
    public List<Order> getAvailableOrdersForDelivery() {
        return orderRepository.findByStatusAndDeliveryBoyIdIsNull(Order.Status.READY_FOR_PICKUP);
    }

    // Delivery boy order accept karta hai
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

    // All order for delivery boy (active + history)
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

    // pick order on seller shop
    public Order markPickedUp(Long deliveryBoyId, Long orderId) {
        Order order = getOwnedDeliveryOrder(deliveryBoyId, orderId);
        if (order.getStatus() != Order.Status.READY_FOR_PICKUP) {
            throw new BadRequestExceprion("Order must be ready for pickup first");
        }
        order.setStatus(Order.Status.PICKED_UP);
        order = orderRepository.save(order);
        notifyCustomer(order, "Order Picked Up", "Your order #" + order.getId() + " has been picked up and is on its way!");
        return order;
    }

    // on the way for customer
    public Order markInTransit(Long deliveryBoyId, Long orderId) {
        Order order = getOwnedDeliveryOrder(deliveryBoyId, orderId);
        if (order.getStatus() != Order.Status.PICKED_UP) {
            throw new BadRequestExceprion("Order must be picked up first");
        }
        order.setStatus(Order.Status.IN_TRANSIT);
        order = orderRepository.save(order);
        notifyCustomer(order, "Out for Delivery", "Your order #" + order.getId() + " is out for delivery!");
        return order;
    }

    // Customer order status update notification
    private void notifyCustomer(Order order, String title, String body) {
        try {
            com.paasmart.backend.auth.User customer = userRepository.findById(order.getCustomerId()).orElse(null);
            if (customer != null && customer.getPushToken() != null) {
                pushNotificationService.send(customer.getPushToken(), title, body);
            }
        } catch (Exception e) {
            System.out.println("Customer notification failed: " + e.getMessage());
        }
    }

    // verify OTP to conform Delivery
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
        notifyCustomer(order, "Order Delivered", "Your order #" + order.getId() + " has been delivered. Enjoy!");
        return order;
    }
}