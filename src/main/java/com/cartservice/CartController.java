package com.cartservice;

import com.cartservice.dto.AddCartItemRequest;
import com.cartservice.dto.CartResponse;
import com.cartservice.dto.CouponRequest;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    private Long getUserId(HttpServletRequest request) {
        return Long.valueOf(request.getHeader("X-User-Id")); // Extracted from Gateway
    }

    @PostMapping("/items")
    public ResponseEntity<Void> addItem(HttpServletRequest request,
                                        @RequestBody AddCartItemRequest req) {
        cartService.addItem(getUserId(request), req);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/items/{productId}")
    public ResponseEntity<Void> removeItem(HttpServletRequest request,
                                           @PathVariable Long productId) {
        cartService.removeItem(getUserId(request), productId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<?>> getCart(HttpServletRequest request) {
        return ResponseEntity.ok(cartService.getCart(getUserId(request)));
    }

    @PostMapping("/apply-coupon")
    public ResponseEntity<Void> applyCoupon(HttpServletRequest request,
                                            @RequestBody CouponRequest req) {
        cartService.applyCoupon(getUserId(request), req.coupon());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/coupon")
    public ResponseEntity<Void> removeCoupon(HttpServletRequest request) {
        cartService.removeCoupon(getUserId(request));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/discount")
    public ResponseEntity<CartResponse> getCartWithTotal(HttpServletRequest request) {
        return ResponseEntity.ok(cartService.getCartWithTotal(getUserId(request)));
    }

    @PostMapping("/checkout")
    public ResponseEntity<Long> checkout(HttpServletRequest request) {
        Long orderId = cartService.checkout(getUserId(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(orderId);
    }
}
