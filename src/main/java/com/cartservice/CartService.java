package com.cartservice;

import com.cartservice.client.ProductClient;
import com.cartservice.dto.AddCartItemRequest;
import com.cartservice.dto.CartItem;
import com.cartservice.dto.ProductDto;
import com.cartservice.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartService {
    private final RedisTemplate<String, String> redisTemplate;
    private final ProductClient productClient;
    private final ObjectMapper mapper = new ObjectMapper();

    private String cartKey(Long userId) {
        return "cart:user:" + userId;
    }

    public void addItem(Long userId, AddCartItemRequest req) {
        if (req.quantity() == null || req.quantity() <= 0) {
            throw new BadRequestException("Quantity must be > 0");
        }

        // Get product from Product Service
        ProductDto product = productClient.getProduct(req.productId());

        if (!"ACTIVE".equalsIgnoreCase(product.status())) {
            throw new BadRequestException("Product is not active");
        }

        // Load existing cart
        List<CartItem> cart = getCart(userId);

        // Check if product already exists in cart
        Optional<CartItem> existing = cart.stream()
                .filter(i -> i.productId().equals(req.productId()))
                .findFirst();

        if (existing.isPresent()) {
            // Update quantity
            cart.remove(existing.get());
            cart.add(new CartItem(
                    existing.get().productId(),
                    product.name(),
                    product.price(),
                    req.quantity()
            ));
        } else {
            // Add new item
            cart.add(new CartItem(
                    product.id(),
                    product.name(),
                    product.price(),
                    req.quantity()
            ));
        }

        saveCart(userId, cart);
    }

    private List<CartItem> getCart(Long userId) {
        String json = redisTemplate.opsForValue().get(cartKey(userId));
        if (json == null) return new ArrayList<>();
        try {
            return Arrays.asList(mapper.readValue(json, CartItem[].class));
        } catch (Exception e) {
            throw new RuntimeException("Error reading cart", e);
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
}
