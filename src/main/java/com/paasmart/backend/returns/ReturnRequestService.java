package com.paasmart.backend.returns;

import com.paasmart.backend.common.CloudinaryService;
import com.paasmart.backend.exception.BadRequestExceprion;
import com.paasmart.backend.exception.ResourceNotFoundException;
import com.paasmart.backend.exception.UnauthorizedException;
import com.paasmart.backend.notification.Notification;
import com.paasmart.backend.notification.NotificationService;
import com.paasmart.backend.notification.PushNotificationService;
import com.paasmart.backend.order.Order;
import com.paasmart.backend.order.OrderItem;
import com.paasmart.backend.order.OrderItemRepository;
import com.paasmart.backend.order.OrderRepository;
import com.paasmart.backend.returns.dto.ResolveReturnDto;
import com.paasmart.backend.returns.dto.ReturnRequestDto;
import com.paasmart.backend.returns.dto.SellerReturnResponse;
import com.paasmart.backend.seller.Shop;
import com.paasmart.backend.seller.ShopRepository;
import com.paasmart.backend.wallet.WalletService;
import com.paasmart.backend.wallet.WalletTransaction;
import com.paasmart.backend.auth.User;
import com.paasmart.backend.auth.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReturnRequestService {

    @Autowired private ReturnRequestRepository returnRequestRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderItemRepository orderItemRepository;
    @Autowired private ShopRepository shopRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private CloudinaryService cloudinaryService;
    @Autowired private WalletService walletService;
    @Autowired private PushNotificationService pushNotificationService;
    @Autowired private NotificationService notificationService;

    private static final int RETURN_WINDOW_DAYS = 7;
    // REFUNDED bhi shamil hai: ek item ka refund ek hi baar mil sakta hai
    private static final List<ReturnRequest.Status> ACTIVE_STATUSES =
            List.of(ReturnRequest.Status.PENDING, ReturnRequest.Status.APPROVED, ReturnRequest.Status.REFUNDED);

    public ReturnRequest createReturnRequest(Long customerId, ReturnRequestDto dto, List<MultipartFile> photos) {
        OrderItem orderItem = orderItemRepository.findById(dto.getOrderItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Order item not found"));

        Order order = orderRepository.findById(orderItem.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.getCustomerId().equals(customerId)) {
            throw new UnauthorizedException("This is not your order");
        }
        if (order.getStatus() != Order.Status.DELIVERED && order.getStatus() != Order.Status.COMPLETED) {
            throw new BadRequestExceprion("You can only request a return after the order has been delivered");
        }
        if (order.getDeliveredAt() == null ||
                order.getDeliveredAt().plusDays(RETURN_WINDOW_DAYS).isBefore(java.time.LocalDateTime.now())) {
            throw new BadRequestExceprion("The return window (" + RETURN_WINDOW_DAYS + " days) for this order has expired");
        }
        if (returnRequestRepository.existsByOrderItemIdAndStatusIn(orderItem.getId(), ACTIVE_STATUSES)) {
            throw new BadRequestExceprion("A return request for this item is already in progress or has been refunded");
        }

        String imageUrls = null;
        if (photos != null && !photos.isEmpty()) {
            imageUrls = photos.stream()
                    .filter(f -> f != null && !f.isEmpty())
                    .map(f -> cloudinaryService.uploadImage(f, "return_request_photos"))
                    .collect(Collectors.joining(","));
        }

        ReturnRequest request = new ReturnRequest();
        request.setOrderId(order.getId());
        request.setOrderItemId(orderItem.getId());
        request.setCustomerId(customerId);
        request.setReason(dto.getReason());
        request.setDescription(dto.getDescription());
        request.setImages(imageUrls);

        request = returnRequestRepository.save(request);

        // Seller ko notify karo
        notifySeller(order, "New Return Request", "A customer has requested a return for order #" + order.getId());

        return request;
    }

    public List<ReturnRequest> getMyReturnRequests(Long customerId) {
        return returnRequestRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    public List<ReturnRequest> getReturnRequestsForOrder(Long requesterId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        Shop shop = shopRepository.findById(order.getShopId())
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found"));

        boolean isCustomer = order.getCustomerId().equals(requesterId);
        boolean isSeller = shop.getSellerId().equals(requesterId);
        if (!isCustomer && !isSeller) {
            throw new UnauthorizedException("You are not part of this order");
        }

        return returnRequestRepository.findByOrderIdOrderByCreatedAtDesc(orderId);
    }

    // Seller approve/reject karta hai
    @Transactional // refund credit aur status update ek saath, ya kuch nahi
    public ReturnRequest resolveReturnRequest(Long sellerId, Long returnRequestId, ResolveReturnDto dto) {
        ReturnRequest request = returnRequestRepository.findByIdForUpdate(returnRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Return request not found"));

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        Shop shop = shopRepository.findById(order.getShopId())
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found"));

        if (!shop.getSellerId().equals(sellerId)) {
            throw new UnauthorizedException("This return request is not for your shop");
        }
        if (request.getStatus() != ReturnRequest.Status.PENDING) {
            throw new BadRequestExceprion("This return request has already been resolved");
        }

        if (dto.isApproved()) {
            OrderItem item = orderItemRepository.findById(request.getOrderItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("Order item not found"));

            java.math.BigDecimal maxRefund = maxRefundFor(order, item);
            java.math.BigDecimal refundAmount = dto.getRefundAmount() != null
                    ? dto.getRefundAmount()
                    : maxRefund;

            // Refund 0 se zyada aur item ke paise se zyada nahi ho sakta
            if (refundAmount.signum() <= 0 || refundAmount.compareTo(maxRefund) > 0) {
                throw new BadRequestExceprion("Refund amount must be more than 0 and at most Rs. " + maxRefund);
            }

            request.setStatus(ReturnRequest.Status.APPROVED);
            request.setRefundAmount(refundAmount);
            request.setResolvedAt(java.time.LocalDateTime.now());
            request = returnRequestRepository.save(request);

            // Wallet me refund credit karo
            walletService.credit(
                    request.getCustomerId(),
                    refundAmount,
                    WalletTransaction.Type.REFUND,
                    "Refund for returned item (Order #" + order.getId() + ")",
                    order.getId()
            );

            request.setStatus(ReturnRequest.Status.REFUNDED);
            request = returnRequestRepository.save(request);

            notifyCustomer(request.getCustomerId(), order.getId(), "Return Approved & Refunded",
                    "Your return has been approved. Rs. " + refundAmount + " has been credited to your wallet.");
        } else {
            String reason = (dto.getRejectionReason() == null || dto.getRejectionReason().isBlank())
                    ? "No reason given"
                    : dto.getRejectionReason().trim();

            request.setStatus(ReturnRequest.Status.REJECTED);
            request.setRejectionReason(reason);
            request.setResolvedAt(java.time.LocalDateTime.now());
            request = returnRequestRepository.save(request);

            notifyCustomer(request.getCustomerId(), order.getId(), "Return Rejected",
                    "Your return request was rejected. Reason: " + reason);
        }

        return request;
    }

    private void notifySeller(Order order, String title, String body) {
        Shop shop = shopRepository.findById(order.getShopId()).orElse(null);
        if (shop == null) return;
        notificationService.notify(shop.getSellerId(), Notification.Type.RETURN_REQUEST, title, body, order.getId());
    }

    private void notifyCustomer(Long customerId, Long orderId, String title, String body) {
        notificationService.notify(customerId, Notification.Type.RETURN_REQUEST, title, body, orderId);
    }

    // Seller ki shop ke saare return requests, product aur customer ke naam ke saath
    public List<SellerReturnResponse> getReturnsForSeller(Long sellerId) {
        Shop shop = shopRepository.findBySellerId(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Your shop is not registered"));

        List<ReturnRequest> requests = returnRequestRepository.findAllForShop(shop.getId());
        if (requests.isEmpty()) {
            return List.of();
        }

        java.util.Map<Long, OrderItem> items = new java.util.HashMap<>();
        java.util.Map<Long, Order> orders = new java.util.HashMap<>();
        orderRepository.findAllById(
                requests.stream().map(ReturnRequest::getOrderId).distinct().collect(Collectors.toList())
        ).forEach(o -> orders.put(o.getId(), o));
        orderItemRepository.findAllById(
                requests.stream().map(ReturnRequest::getOrderItemId).distinct().collect(Collectors.toList())
        ).forEach(i -> items.put(i.getId(), i));

        java.util.Map<Long, String> names = new java.util.HashMap<>();
        userRepository.findAllById(
                requests.stream().map(ReturnRequest::getCustomerId).distinct().collect(Collectors.toList())
        ).forEach(u -> names.put(u.getId(), u.getName() == null ? "Customer" : u.getName()));

        return requests.stream()
                .map(r -> {
                    OrderItem it = items.get(r.getOrderItemId());
                    Order o = orders.get(r.getOrderId());
                    java.math.BigDecimal maxRefund = (it != null && o != null) ? maxRefundFor(o, it) : null;
                    return new SellerReturnResponse(r, it, names.getOrDefault(r.getCustomerId(), "Customer"), maxRefund);
                })
                .collect(Collectors.toList());
    }

    // Item ka maximum refund: coupon discount ko item ke hisse me se ghata ke
    private java.math.BigDecimal maxRefundFor(Order order, OrderItem item) {
        java.math.BigDecimal max = item.getSubtotal();
        java.math.BigDecimal subtotal = order.getSubtotalAmount();
        java.math.BigDecimal discount = order.getDiscountAmount();
        if (subtotal != null && subtotal.signum() > 0 && discount != null && discount.signum() > 0) {
            java.math.BigDecimal ratio = discount.divide(subtotal, 6, java.math.RoundingMode.HALF_UP);
            max = max.subtract(max.multiply(ratio)).setScale(2, java.math.RoundingMode.HALF_UP);
        }
        return max;
    }
}