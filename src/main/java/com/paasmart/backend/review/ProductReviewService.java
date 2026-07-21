package com.paasmart.backend.review;

import com.paasmart.backend.common.CloudinaryService;
import com.paasmart.backend.exception.BadRequestExceprion;
import com.paasmart.backend.exception.ResourceNotFoundException;
import com.paasmart.backend.exception.UnauthorizedException;
import com.paasmart.backend.order.Order;
import com.paasmart.backend.order.OrderItem;
import com.paasmart.backend.order.OrderItemRepository;
import com.paasmart.backend.order.OrderRepository;
import com.paasmart.backend.review.dto.ProductReviewSummary;
import com.paasmart.backend.review.dto.ReviewRequest;
import com.paasmart.backend.review.dto.SellerReplyRequest;
import com.paasmart.backend.seller.Shop;
import com.paasmart.backend.seller.ShopRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProductReviewService {

    @Autowired private ProductReviewRepository reviewRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderItemRepository orderItemRepository;
    @Autowired private ShopRepository shopRepository;
    @Autowired private CloudinaryService cloudinaryService;

    public ProductReview addReview(Long customerId, Long productId, Long orderId,
                                   ReviewRequest req, List<MultipartFile> photos) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.getCustomerId().equals(customerId)) {
            throw new UnauthorizedException("This is not your order");
        }
        if (order.getStatus() != Order.Status.DELIVERED && order.getStatus() != Order.Status.COMPLETED) {
            throw new BadRequestExceprion("You can review a product only after it has been delivered");
        }

        // Confirm product actually was part of this order
        boolean productInOrder = orderItemRepository.findByOrderId(orderId).stream()
                .anyMatch(item -> item.getProductId().equals(productId));
        if (!productInOrder) {
            throw new BadRequestExceprion("This product was not part of this order");
        }

        if (reviewRepository.existsByProductIdAndOrderId(productId, orderId)) {
            throw new BadRequestExceprion("You have already reviewed this product for this order");
        }

        String imageUrls = null;
        if (photos != null && !photos.isEmpty()) {
            imageUrls = photos.stream()
                    .filter(f -> f != null && !f.isEmpty())
                    .map(f -> cloudinaryService.uploadImage(f, "review_photos"))
                    .collect(Collectors.joining(","));
        }

        ProductReview review = new ProductReview();
        review.setProductId(productId);
        review.setCustomerId(customerId);
        review.setOrderId(orderId);
        review.setRating(req.getRating());
        review.setReviewText(req.getReviewText());
        review.setImages(imageUrls);

        return reviewRepository.save(review);
    }

    public ProductReviewSummary getProductReviews(Long productId) {
        List<ProductReview> reviews = reviewRepository.findByProductIdOrderByCreatedAtDesc(productId);
        Double avg = reviewRepository.getAverageRatingForProduct(productId);
        long total = reviewRepository.countByProductId(productId);

        Map<Integer, Long> breakdown = new HashMap<>();
        for (int star = 1; star <= 5; star++) {
            breakdown.put(star, reviewRepository.countByProductIdAndRating(productId, star));
        }

        return new ProductReviewSummary(
                avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0,
                total,
                breakdown,
                reviews
        );
    }

    public List<ProductReview> getMyReviews(Long customerId) {
        return reviewRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    // Seller kisi review ka reply de sakta hai
    public ProductReview replyToReview(Long sellerId, Long reviewId, SellerReplyRequest req) {
        ProductReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        // Verify: ye product isi seller ki shop ka hai
        com.paasmart.backend.order.Order order = orderRepository.findById(review.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        Shop shop = shopRepository.findById(order.getShopId())
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found"));

        if (!shop.getSellerId().equals(sellerId)) {
            throw new UnauthorizedException("This review is not for your shop's product");
        }

        review.setSellerReply(req.getReply());
        review.setSellerRepliedAt(java.time.LocalDateTime.now());
        return reviewRepository.save(review);
    }

    // "Helpful" button — koi bhi logged-in user click kar sakta hai
    public ProductReview markHelpful(Long reviewId) {
        ProductReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        review.setHelpfulCount(review.getHelpfulCount() + 1);
        return reviewRepository.save(review);
    }
}