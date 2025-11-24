package com.cartservice;

import com.cartservice.dto.AddCartItemRequest;
import com.cartservice.dto.CartItem;
import com.cartservice.dto.CartResponse;
import com.cartservice.dto.CouponRequest;
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

    @PostMapping("/{userId}/items")
    public ResponseEntity<Void> addItem(@PathVariable Long userId, @RequestBody AddCartItemRequest req) {
        cartService.addItem(userId, req);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{userId}/items/{productId}")
    public ResponseEntity<Void> removeItem(@PathVariable Long userId, @PathVariable Long productId) {
        cartService.removeItem(userId, productId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<CartItem>> getCart(@PathVariable Long userId) {
        return ResponseEntity.ok(cartService.getCart(userId));
    }

    @PostMapping("/{userId}/apply-coupon")
    public ResponseEntity<Void> applyCoupon(@PathVariable Long userId, @RequestBody CouponRequest req) {
        cartService.applyCoupon(userId, req.coupon());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{userId}/coupon")
    public ResponseEntity<Void> removeCoupon(@PathVariable Long userId) {
        cartService.removeCoupon(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/discount/{userId}")
    public ResponseEntity<CartResponse> getCartWithTotal(@PathVariable Long userId) {
        return ResponseEntity.ok(cartService.getCartWithTotal(userId));
    }

    @PostMapping("/{userId}/checkout")
    public ResponseEntity<Long> checkout(@PathVariable Long userId) {
        Long orderId = cartService.checkout(userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(orderId);
    }

}
