package com.cartservice.dto;

public record ProductDto(Long id, String name, String description, Double price, Integer stock, String status) {
}
