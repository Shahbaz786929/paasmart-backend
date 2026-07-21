package com.paasmart.backend.checkout;

import com.paasmart.backend.address.Address;
import com.paasmart.backend.address.AddressRepository;
import com.paasmart.backend.auth.User;
import com.paasmart.backend.auth.UserRepository;
import com.paasmart.backend.cart.Cart;
import com.paasmart.backend.cart.CartRepository;
import com.paasmart.backend.coupon.CouponService;
import com.paasmart.backend.exception.ResourceNotFoundException;
import com.paasmart.backend.order.Order;
import com.paasmart.backend.order.OrderItem;
import com.paasmart.backend.order.OrderItemRepository;
import com.paasmart.backend.order.OrderRepository;
import com.paasmart.backend.product.Product;
import com.paasmart.backend.product.ProductRepository;
import com.paasmart.backend.seller.ShopRepository;
import com.paasmart.backend.seller.ShopService;
import com.paasmart.backend.wallet.WalletService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;

@Service
public class CheckoutServiceImpl implements CheckoutService {

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

    public CheckoutServiceImpl(UserRepository userRepository,
                               AddressRepository addressRepository,
                               CartRepository cartRepository,
                               ProductRepository productRepository,
                               OrderRepository orderRepository,
                               OrderItemRepository orderItemRepository,
                               CouponService couponService,
                               WalletService walletService,
                               ShopRepository shopRepository,
                               ShopService shopService) {

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
    }

    @Override
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
            throw new RuntimeException("Cart Is Empty");
        }

        Product firstProduct = productRepository.findById(cartItems.get(0).getProduct().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Product Not Found"));

        // Delivery zone check
        com.paasmart.backend.seller.Shop shop = shopRepository.findById(firstProduct.getShopId())
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found"));

        if (!shopService.canDeliverTo(shop, address.getLatitude(), address.getLongitude())) {
            throw new com.paasmart.backend.exception.BadRequestExceprion(
                    "Sorry, this shop only delivers within " + shop.getDeliveryRadiusKm() + " km. Your address is outside their delivery range.");
        }

        Order order = new Order();
        order.setCustomerId(customer.getId());
        order.setShopId(firstProduct.getShopId());
        order.setAddress(address);
        order.setDeliveryAddress(
                address.getHouseNo() + ", " + address.getArea() + ", "
                        + address.getCity() + ", " + address.getState() + "-" + address.getPincode()
        );
        order.setDeliveryFee(new BigDecimal("40"));
        order.setPaymentMode(
                request.getPaymentMethod().equalsIgnoreCase("COD")
                        ? Order.PaymentMode.COD
                        : Order.PaymentMode.ONLINE
        );
        order.setStatus(Order.Status.PLACED);

        Random random = new Random();
        order.setOtp(String.valueOf(100000 + random.nextInt(900000)));

        BigDecimal grandTotal = BigDecimal.ZERO;
        for (Cart cart : cartItems) {
            Product product = productRepository.findById(cart.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product Not Found"));
            if (product.getStockQty() < cart.getQuantity()) {
                throw new RuntimeException(product.getName() + " Out Of Stock");
            }
            BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(cart.getQuantity()));
            grandTotal = grandTotal.add(subtotal);
        }

        BigDecimal totalBeforeDiscount = grandTotal.add(order.getDeliveryFee());
        order.setTotalAmount(totalBeforeDiscount);

        order = orderRepository.save(order);   // pehle save karo taaki orderId mile

        // Agar coupon diya gaya hai to redeem karo aur totalAmount adjust karo
        if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
            BigDecimal discount = couponService.redeemCoupon(
                    customer.getId(), request.getCouponCode(), totalBeforeDiscount, order.getId());
            order.setTotalAmount(totalBeforeDiscount.subtract(discount));
            order = orderRepository.save(order);
        }

        // Agar customer wallet use karna chahta hai
        if (Boolean.TRUE.equals(request.getUseWallet())) {
            BigDecimal walletBalance = customer.getWalletBalance();
            BigDecimal amountToUse = walletBalance.min(order.getTotalAmount());   // jitna hai ya jitna order ka total, jo kam ho

            if (amountToUse.compareTo(BigDecimal.ZERO) > 0) {
                walletService.debit(customer.getId(), amountToUse, "Used for order #" + order.getId(), order.getId());
                order.setTotalAmount(order.getTotalAmount().subtract(amountToUse));
                order = orderRepository.save(order);
            }
        }

        // create order items and decrease stock
        for (Cart cart : cartItems) {
            Product product = productRepository.findById(cart.getProduct().getId()).get();

            OrderItem item = new OrderItem();
            item.setOrderId(order.getId());
            item.setProductId(product.getId());
            item.setQuantity(cart.getQuantity());
            item.setUnitPrice(product.getPrice());
            item.setSubtotal(product.getPrice().multiply(BigDecimal.valueOf(cart.getQuantity())));
            orderItemRepository.save(item);

            product.setStockQty(product.getStockQty() - cart.getQuantity());
            productRepository.save(product);
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