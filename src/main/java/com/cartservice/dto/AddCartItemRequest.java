package com.cartservice.dto;

public record AddCartItemRequest(Long productId, Integer quantity) {
}
