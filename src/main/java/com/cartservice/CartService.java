package com.cartservice;

import com.cartservice.client.OrderClient;
import com.cartservice.client.ProductClient;
import com.cartservice.dto.*;
import com.cartservice.exception.BadRequestException;
import com.cartservice.exception.RemoteServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {
    private final RedisTemplate<String, String> redisTemplate;
    private final ProductClient productClient;
    private final OrderClient orderClient;
    private final ObjectMapper mapper = new ObjectMapper();

    private String cartKey(Long userId) {
        return "cart:user:" + userId;
    }

    public void addItem(Long userId, AddCartItemRequest req) {

        if (req.quantity() == null || req.quantity() <= 0) {
            throw new BadRequestException("Quantity must be > 0");
        }

        ProductDto product = productClient.getProduct(req.productId());

        if (!"ACTIVE".equalsIgnoreCase(product.status())) {
            throw new BadRequestException("Product is not active");
        }

        List<CartItem> cart = getCart(userId);

        cart.removeIf(i -> i.productId().equals(req.productId()));

        cart.add(new CartItem(
                product.id(),
                product.name(),
                product.price(),
                req.quantity()
        ));

        saveCart(userId, cart);
    }


    public List<CartItem> getCart(Long userId) {
        try {
            String json = redisTemplate.opsForValue().get(cartKey(userId));
            if (json == null) return new ArrayList<>();

            CartItem[] arr = mapper.readValue(json, CartItem[].class);
            return new ArrayList<>(Arrays.asList(arr));
        } catch (Exception e) {
            throw new RuntimeException("JSON_PARSE_ERROR", e);
        }
    }


    private void saveCart(Long userId, List<CartItem> cart) {
        try {
            String json = mapper.writeValueAsString(cart);
            redisTemplate.opsForValue().set(cartKey(userId), json, Duration.ofHours(2));
        } catch (Exception e) {
            throw new RuntimeException("Error saving cart", e);
        }
    }

    public void removeItem(Long userId, Long productId) {
        List<CartItem> cart = getCart(userId);
        if (cart.isEmpty()) {
            throw new BadRequestException("Cart is empty");
        }
        boolean remove = cart.removeIf(i -> i.productId().equals(productId));
        if (!remove) {
            throw new BadRequestException("Item not found in cart");
        }

        if (cart.isEmpty()) {
            // Cart becomes empty → delete the key
            redisTemplate.delete(cartKey(userId));
        }

        saveCart(userId, cart);
    }


    private String couponKey(Long userId) {
            return "cart:coupon:" + userId;
    }

    public void applyCoupon(Long userId, String coupon) {
        if (coupon == null || coupon.isBlank()) {
            throw new BadRequestException("Invalid coupon");
        }

        if (!isValidCoupon(coupon)) {
            throw new BadRequestException("Invalid or expired coupon");
        }

        redisTemplate.opsForValue().set(couponKey(userId), coupon, Duration.ofHours(2));
    }

    private boolean isValidCoupon(String coupon) {
        return switch (coupon.toUpperCase()) {
            case "SAVE10", "WELCOME50", "NEWUSER", "FLAT100" -> true;
            default -> false;
        };
    }

    public void removeCoupon(Long userId) {
        redisTemplate.delete(couponKey(userId));
    }

    public CartResponse getCartWithTotal(Long userId) {

        List<CartItem> items = getCart(userId);

        double subTotal = items.stream()
                .mapToDouble(i -> i.price() * i.quantity())
                .sum();

        double itemDiscount = items.stream()
                .mapToDouble(this::calculateProductDiscount)
                .sum();

        double cartDiscount = calculateCartDiscount(subTotal);

        String coupon = redisTemplate.opsForValue().get(couponKey(userId));
        double couponDiscount = 0;

        if (coupon != null) {
            couponDiscount = applyCouponDiscount(coupon, subTotal - itemDiscount - cartDiscount);
        }

        double totalDiscount = itemDiscount + cartDiscount + couponDiscount;
        double grandTotal = subTotal - totalDiscount;

        return new CartResponse(
                items,
                subTotal,
                totalDiscount,
                grandTotal,
                coupon
        );
    }

    private double applyCouponDiscount(String coupon, double amount) {
        return switch (coupon.toUpperCase()) {
            case "SAVE10" -> amount * 0.10;      // 10% off
            case "WELCOME50" -> 50.0;            // flat ₹50 off
            case "NEWUSER" -> amount * 0.15;     // 15% off
            case "FLAT100" -> 100.0;             // flat ₹100
            default -> 0;
        };
    }

    private double calculateProductDiscount(CartItem item) {
        if (item.price() > 1000) {
            return item.price() * item.quantity() * 0.10;
        }
        return 0;
    }

    private double calculateCartDiscount(double subTotal) {
        if (subTotal > 5000) {
            return 200;
        }
        return 0;
    }

    public Long checkout(Long userId) {

        // 1) fetch cart
        CartResponse cart = getCartWithTotal(userId);
        if (cart.items().isEmpty()) {
            throw new BadRequestException("Cart is empty");
        }

        // 2) build OrderCreateRequest
        List<OrderItemRequest> orderItems = cart.items().stream()
                .map(i -> new OrderItemRequest(
                        i.productId(),
                        i.name(),
                        i.price(),
                        i.quantity(),
                        i.price() * i.quantity()
                ))
                .toList();

        OrderCreateRequest req = new OrderCreateRequest(
                userId,
                cart.grandTotal(),
                orderItems
        );

        Long orderId = orderClient.createOrder(req);

        if (orderId == null) {
            throw new RemoteServiceException("Order Service returned null");
        }

        redisTemplate.delete("cart:user:" + userId);
        redisTemplate.delete("cart:coupon:user:" + userId);

        return orderId;
    }

}
