package com.cartservice;

import com.cartservice.dto.AddCartItemRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
