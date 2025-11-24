package com.cartservice.dto;

import java.util.List;

public record OrderCreateRequest(Long userId, Double total, List<OrderItemRequest> items) {
}
