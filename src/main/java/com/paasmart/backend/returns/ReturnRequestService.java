package com.paasmart.backend.returns;

import com.paasmart.backend.common.CloudinaryService;
import com.paasmart.backend.exception.BadRequestExceprion;
import com.paasmart.backend.exception.ResourceNotFoundException;
import com.paasmart.backend.exception.UnauthorizedException;
import com.paasmart.backend.notification.PushNotificationService;
import com.paasmart.backend.order.Order;
import com.paasmart.backend.order.OrderItem;
import com.paasmart.backend.order.OrderItemRepository;
import com.paasmart.backend.order.OrderRepository;
import com.paasmart.backend.returns.dto.ResolveReturnDto;
import com.paasmart.backend.returns.dto.ReturnRequestDto;
import com.paasmart.backend.seller.Shop;
import com.paasmart.backend.seller.ShopRepository;
import com.paasmart.backend.wallet.WalletService;
import com.paasmart.backend.wallet.WalletTransaction;
import com.paasmart.backend.auth.User;
import com.paasmart.backend.auth.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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

    private static final int RETURN_WINDOW_DAYS = 7;
    private static final List<ReturnRequest.Status> ACTIVE_STATUSES =
            List.of(ReturnRequest.Status.PENDING, ReturnRequest.Status.APPROVED);

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
            throw new BadRequestExceprion("A return request for this item is already in progress");
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
    public ReturnRequest resolveReturnRequest(Long sellerId, Long returnRequestId, ResolveReturnDto dto) {
        ReturnRequest request = returnRequestRepository.findById(returnRequestId)
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

            java.math.BigDecimal refundAmount = dto.getRefundAmount() != null
                    ? dto.getRefundAmount()
                    : item.getSubtotal();

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

            notifyCustomer(request.getCustomerId(), "Return Approved & Refunded",
                    "Your return has been approved. Rs. " + refundAmount + " has been credited to your wallet.");
        } else {
            request.setStatus(ReturnRequest.Status.REJECTED);
            request.setRejectionReason(dto.getRejectionReason());
            request.setResolvedAt(java.time.LocalDateTime.now());
            request = returnRequestRepository.save(request);

            notifyCustomer(request.getCustomerId(), "Return Rejected",
                    "Your return request was rejected. Reason: " + dto.getRejectionReason());
        }

        return request;
    }

    private void notifySeller(Order order, String title, String body) {
        try {
            Shop shop = shopRepository.findById(order.getShopId()).orElse(null);
            if (shop == null) return;
            User seller = userRepository.findById(shop.getSellerId()).orElse(null);
            if (seller != null && seller.getPushToken() != null) {
                pushNotificationService.send(seller.getPushToken(), title, body);
            }
        } catch (Exception e) {
            System.out.println("Seller notification failed: " + e.getMessage());
        }
    }

    private void notifyCustomer(Long customerId, String title, String body) {
        try {
            User customer = userRepository.findById(customerId).orElse(null);
            if (customer != null && customer.getPushToken() != null) {
                pushNotificationService.send(customer.getPushToken(), title, body);
            }
        } catch (Exception e) {
            System.out.println("Customer notification failed: " + e.getMessage());
        }
    }
}