package com.cartservice.dto;

public record CartItem(Long productId, String name, Double price, Integer quantity) {
}
