package com.arsiwooqq.orderservice.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record OrderCreateRequest(
        @NotNull(message = "User ID is required")
        String userId,
        @NotNull(message = "Order items is required")
        List<OrderItemRequest> orderItems
) {
}
