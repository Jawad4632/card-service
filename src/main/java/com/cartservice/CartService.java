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
}
