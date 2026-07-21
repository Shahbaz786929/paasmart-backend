package com.paasmart.backend.seller;

import com.paasmart.backend.exception.ResourceNotFoundException;
import com.paasmart.backend.order.Order;
import com.paasmart.backend.order.OrderItemRepository;
import com.paasmart.backend.order.OrderRepository;
import com.paasmart.backend.product.ProductRepository;
import com.paasmart.backend.rating.RatingRepository;
import com.paasmart.backend.seller.dto.SellerDashboardResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class SellerDashboardService {

    @Autowired private ShopRepository shopRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderItemRepository orderItemRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private RatingRepository ratingRepository;

    public SellerDashboardResponse getDashboard(Long sellerId) {
        Shop shop = shopRepository.findBySellerId(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("You don't have any registered shops."));

        Long shopId = shop.getId();
        SellerDashboardResponse dashboard = new SellerDashboardResponse();

        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime startOfWeek = LocalDate.now().minusDays(7).atStartOfDay();
        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();

        // Orders count
        dashboard.setTotalOrders(orderRepository.countByShopId(shopId));
        dashboard.setOrdersToday(orderRepository.countByShopIdAndCreatedAtAfter(shopId, startOfToday));
        dashboard.setOrdersThisWeek(orderRepository.countByShopIdAndCreatedAtAfter(shopId, startOfWeek));
        dashboard.setOrdersThisMonth(orderRepository.countByShopIdAndCreatedAtAfter(shopId, startOfMonth));

        long pending = orderRepository.countByShopIdAndStatus(shopId, Order.Status.PLACED)
                + orderRepository.countByShopIdAndStatus(shopId, Order.Status.CONFIRMED)
                + orderRepository.countByShopIdAndStatus(shopId, Order.Status.PREPARING);
        dashboard.setPendingOrders(pending);
        dashboard.setCompletedOrders(orderRepository.countByShopIdAndStatus(shopId, Order.Status.COMPLETED));

        // Revenue
        dashboard.setTotalRevenue(orderRepository.getShopTotalRevenue(shopId));
        dashboard.setRevenueThisMonth(orderRepository.getShopRevenueSince(shopId, startOfMonth));

        // Products
        dashboard.setTotalProducts(productRepository.countByShopId(shopId));
        dashboard.setOutOfStockProducts(productRepository.countByShopIdAndStockQty(shopId, 0));

        // Rating
        Double avgRating = ratingRepository.getAverageRatingBySeller(sellerId);
        dashboard.setAverageRating(avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 0.0);

        // Top products
        List<Object[]> rows = orderItemRepository.findTopProductsByShop(shopId);
        List<SellerDashboardResponse.TopProduct> topProducts = new ArrayList<>();
        for (Object[] row : rows) {
            Long productId = ((Number) row[0]).longValue();
            String name = (String) row[1];
            Integer qty = ((Number) row[2]).intValue();
            BigDecimal revenue = row[3] instanceof BigDecimal ? (BigDecimal) row[3] : BigDecimal.valueOf(((Number) row[3]).doubleValue());
            topProducts.add(new SellerDashboardResponse.TopProduct(productId, name, qty, revenue));
        }
        dashboard.setTopProducts(topProducts);

        return dashboard;
    }
}