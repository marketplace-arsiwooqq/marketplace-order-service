package com.arsiwooqq.orderservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ItemCreateRequest(
        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 255, message = "Name must be between 2 and 255 characters")
        String name,
        @NotNull(message = "Price is required")
        @Min(value = 0, message = "Price must be positive")
        Long price
) {
}
