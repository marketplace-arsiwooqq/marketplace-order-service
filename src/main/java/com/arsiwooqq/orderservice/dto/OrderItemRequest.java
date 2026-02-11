package com.arsiwooqq.orderservice.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record OrderItemRequest(
        @NotNull(message = "Item ID is required")
        UUID itemId,
        @NotNull(message = "Quantity is required")
        Integer quantity
) {
}
