package com.cartservice.dto;

import java.util.List;

public record CartResponse(List<CartItem> items, Double subTotal, Double discount, Double grandTotal,
                           String appliedCoupon) {
}
