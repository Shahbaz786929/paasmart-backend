package com.paasmart.backend.rating;

import com.paasmart.backend.exception.BadRequestExceprion;
import com.paasmart.backend.exception.ResourceNotFoundException;
import com.paasmart.backend.exception.UnauthorizedException;
import com.paasmart.backend.order.Order;
import com.paasmart.backend.order.OrderRepository;
import com.paasmart.backend.seller.Shop;
import com.paasmart.backend.seller.ShopRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RatingService {

    @Autowired private RatingRepository ratingRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private ShopRepository shopRepository;

    public Rating rateOrder(Long customerId, Long orderId, RatingRequest req) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (!order.getCustomerId().equals(customerId)) {
            throw new UnauthorizedException("This is not you order");
        }

        if (order.getStatus() != Order.Status.DELIVERED && order.getStatus() != Order.Status.COMPLETED) {
            throw new BadRequestExceprion("You can rate an order only after it has been delivered");
        }

        if (ratingRepository.existsByOrderId(orderId)) {
            throw new BadRequestExceprion("This order has already been rated");
        }

        Shop shop = shopRepository.findById(order.getShopId())
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found"));

        Rating rating = new Rating();
        rating.setOrderId(orderId);
        rating.setCustomerId(customerId);
        rating.setSellerId(shop.getSellerId());
        rating.setProductRating(req.getProductRating());
        rating.setDeliveryRating(req.getDeliveryRating());
        rating.setComment(req.getComment());

        return ratingRepository.save(rating);
    }

    public List<Rating> getShopRating (Long shopId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found"));
        return ratingRepository.findBySellerIdOrderByCreatedAtDesc(shop.getSellerId());
    }
}
