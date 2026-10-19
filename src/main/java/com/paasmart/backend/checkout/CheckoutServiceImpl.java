package com.paasmart.backend.checkout;

import com.paasmart.backend.address.Address;
import com.paasmart.backend.address.AddressRepository;
import com.paasmart.backend.auth.User;
import com.paasmart.backend.auth.UserRepository;
import com.paasmart.backend.cart.Cart;
import com.paasmart.backend.cart.CartRepository;
import com.paasmart.backend.coupon.CouponService;
import com.paasmart.backend.exception.BadRequestExceprion;
import com.paasmart.backend.exception.ResourceNotFoundException;
import com.paasmart.backend.flashsale.FlashSaleService;
import com.paasmart.backend.order.*;
import com.paasmart.backend.product.Product;
import com.paasmart.backend.product.ProductRepository;
import com.paasmart.backend.seller.ShopRepository;
import com.paasmart.backend.seller.ShopService;
import com.paasmart.backend.settings.SettingsService;
import com.paasmart.backend.wallet.WalletService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.paasmart.backend.seller.GroUtils;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

@Service
public class CheckoutServiceImpl implements CheckoutService {

    private static final SecureRandom secureRandom = new SecureRandom();

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CouponService couponService;
    private final WalletService walletService;
    private final ShopRepository shopRepository;
    private final ShopService shopService;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final FlashSaleService flashSaleService;
    private final SettingsService settingsService;

    public CheckoutServiceImpl(UserRepository userRepository,
                               AddressRepository addressRepository,
                               CartRepository cartRepository,
                               ProductRepository productRepository,
                               OrderRepository orderRepository,
                               OrderItemRepository orderItemRepository,
                               CouponService couponService,
                               WalletService walletService,
                               ShopRepository shopRepository,
                               ShopService shopService,
                               OrderStatusHistoryRepository orderStatusHistoryRepository,
                               FlashSaleService flashSaleService,
                               SettingsService settingsService) {

        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.couponService = couponService;
        this.walletService = walletService;
        this.shopRepository = shopRepository;
        this.shopService = shopService;
        this.orderStatusHistoryRepository = orderStatusHistoryRepository;
        this.flashSaleService = flashSaleService;
        this.settingsService = settingsService;
    }

    private static class LockedItem {
        final Long productId;
        final Integer quantity;
        final BigDecimal unitPrice;
        final BigDecimal subtotal;
        final String productName;
        final String productImageUrl;

        LockedItem(Long productId, Integer quantity, BigDecimal unitPrice, BigDecimal subtotal, String productName, String productImageUrl) {
            this.productId = productId;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
            this.subtotal = subtotal;
            this.productName = productName;
            this.productImageUrl = productImageUrl;
        }
    }

    @Override
    @Transactional // pura checkout ek hi DB transaction me — beech me kahi bhi fail hua to sab kuch (stock decrement, wallet debit, coupon usage) automatically rollback ho jayega
    public CheckoutResponse placeOrder(Long customerId, CheckoutRequest request) {

        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer Not Found"));

        Address address = addressRepository.findById(request.getAddressId())
                .orElseThrow(() -> new ResourceNotFoundException("Address Not Found"));

        // Security check: ye address isi customer ki honi chahiye
        if (!address.getCustomer().getId().equals(customerId)) {
            throw new com.paasmart.backend.exception.UnauthorizedException("This is not your address");
        }

        List<Cart> cartItems = cartRepository.findByCustomer(customer);
        if (cartItems.isEmpty()) {
            throw new BadRequestExceprion("Your cart is empty");
        }

        Product firstProduct = productRepository.findById(cartItems.get(0).getProduct().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Product Not Found"));

        // Delivery zone check
        com.paasmart.backend.seller.Shop shop = shopRepository.findById(firstProduct.getShopId())
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found"));

        if (shop.getStatus() != com.paasmart.backend.seller.Shop.Status.APPROVED) {
            throw new com.paasmart.backend.exception.BadRequestExceprion("This shop is currently not accepting orders.");
        }
        // Ek order me ek hi shop ka saaman
        for (Cart c : cartItems) {
            if (!java.util.Objects.equals(c.getProduct().getShopId(), shop.getId())) {
                throw new com.paasmart.backend.exception.BadRequestExceprion(
                        "Your cart has items from more than one shop. Please order from one shop at a time.");
            }
        }

        if (!shopService.canDeliverTo(shop, address.getLatitude(), address.getLongitude())) {
            throw new com.paasmart.backend.exception.BadRequestExceprion(
                    "Sorry, this shop only delivers within " + shop.getDeliveryRadiusKm() + " km. Your address is outside their delivery range.");
        }

        Order order = new Order();
        order.setCustomerId(customer.getId());
        order.setShopId(firstProduct.getShopId());
        order.setTenantId(shop.getTenant().getId());
        order.setAddress(address);
        order.setDeliveryAddress(
                address.getHouseNo() + ", " + address.getArea() + ", "
                        + address.getCity() + ", " + address.getState() + "-" + address.getPincode()
        );
        double distanceKm = GroUtils.distanceKmOrNull(shop.getLatitude(), shop.getLongitude(), address.getLatitude(), address.getLongitude()) != null
                ? GroUtils.distanceKm(shop.getLatitude(), shop.getLongitude(), address.getLatitude(), address.getLongitude())
                : 1.0; // coordinates missing -> charge only the base fee
        order.setDeliveryFee(settingsService.calculateDeliveryFee(distanceKm));
        String method = request.getPaymentMethod() == null ? "" : request.getPaymentMethod().trim().toUpperCase();
        if (!method.equals("COD")) {
            throw new com.paasmart.backend.exception.BadRequestExceprion(
                    "Online payment is not available yet. Please choose Cash on Delivery.");
        }
        order.setPaymentMode(Order.PaymentMode.COD);
        order.setStatus(Order.Status.PLACED);

        order.setOtp(String.valueOf(100000 + secureRandom.nextInt(900000)));

        // Sab products ko row-lock karke, stock validate + decrement ek hi jagah karte hai.
        // Isse do customers same last item ek saath order nahi kar payenge (pehle ka TOCTOU race-condition bug fix),
        // aur flash-sale ka effectivePrice ab actually use hota hai total me (pehle ye calculate hoke bhi ignore ho raha tha).
        BigDecimal grandTotal = BigDecimal.ZERO;
        List<LockedItem> lockedItems = new ArrayList<>();
        for (Cart cart : cartItems) {
            Product product = productRepository.findByIdForUpdate(cart.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product Not Found"));

            if (!Boolean.TRUE.equals(product.getIsAvailable())) {
                throw new com.paasmart.backend.exception.BadRequestExceprion(product.getName() + " is no longer available");
            }

            if (product.getStockQty() < cart.getQuantity()) {
                throw new com.paasmart.backend.exception.BadRequestExceprion(product.getName() + " is out of stock (only " + product.getStockQty() + " left)");
            }

            BigDecimal effectivePrice = flashSaleService.getEffectivePrice(product);
            BigDecimal subtotal = effectivePrice.multiply(BigDecimal.valueOf(cart.getQuantity()));
            grandTotal = grandTotal.add(subtotal);

            product.setStockQty(product.getStockQty() - cart.getQuantity());
            productRepository.save(product);

            String firstImage = product.getImages() != null && !product.getImages().isBlank()
                    ? product.getImages().split(",")[0].trim()
                    : null;
            lockedItems.add(new LockedItem(product.getId(), cart.getQuantity(), effectivePrice, subtotal, product.getName(), firstImage));
        }

        BigDecimal totalBeforeDiscount = grandTotal.add(order.getDeliveryFee());
        order.setTotalAmount(totalBeforeDiscount);
        order.setSubtotalAmount(grandTotal);

        order = orderRepository.save(order);   // pehle save karo taaki orderId mile

        // Order status history — hamesha log hoga, coupon use ho ya na ho
        OrderStatusHistory placeHistory = new OrderStatusHistory();
        placeHistory.setOrderId(order.getId());
        placeHistory.setStatus(Order.Status.PLACED);
        placeHistory.setChangedById(customerId);
        placeHistory.setNote("Order placed by customer");
        orderStatusHistoryRepository.save(placeHistory);

        // Agar coupon diya gaya hai to redeem karo aur totalAmount adjust karo
        if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
            BigDecimal discount = couponService.redeemCoupon(
                    customer.getId(), request.getCouponCode(), totalBeforeDiscount, order.getId());
            order.setTotalAmount(totalBeforeDiscount.subtract(discount));
            order.setDiscountAmount(discount);
            order = orderRepository.save(order);
        }

        // Agar customer wallet use karna chahta hai
        if (Boolean.TRUE.equals(request.getUseWallet())) {
            BigDecimal walletBalance = customer.getWalletBalance();
            BigDecimal amountToUse = walletBalance.min(order.getTotalAmount());   // jitna hai ya jitna order ka total, jo kam ho

            if (amountToUse.compareTo(BigDecimal.ZERO) > 0) {
                walletService.debit(customer.getId(), amountToUse, "Used for order #" + order.getId(), order.getId());
                order.setTotalAmount(order.getTotalAmount().subtract(amountToUse));
                order.setWalletUsed(amountToUse);
                order = orderRepository.save(order);
            }
        }

        for (LockedItem locked : lockedItems) {
            OrderItem item = new OrderItem();
            item.setOrderId(order.getId());
            item.setProductId(locked.productId);
            item.setQuantity(locked.quantity);
            item.setUnitPrice(locked.unitPrice);
            item.setSubtotal(locked.subtotal);
            item.setProductName(locked.productName);
            item.setProductImageUrl(locked.productImageUrl);
            orderItemRepository.save(item);
        }

        cartRepository.deleteAll(cartItems);

        CheckoutResponse response = new CheckoutResponse();
        response.setOrderId(order.getId());
        response.setTotalAmount(order.getTotalAmount());
        response.setOrderStatus(order.getStatus().name());
        response.setPaymentStatus("PENDING");
        return response;
    }
}