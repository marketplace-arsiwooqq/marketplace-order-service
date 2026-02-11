package com.arsiwooqq.orderservice.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record OrderUpdateRequest(
        @NotNull(message = "Order items is required")
        List<OrderItemRequest> orderItems
) {
}
