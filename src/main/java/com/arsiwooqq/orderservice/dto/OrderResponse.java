package com.arsiwooqq.orderservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.arsiwooqq.orderservice.enums.OrderStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrderResponse(
        UUID id,
        String userId,
        OrderStatus status,
        LocalDate creationDate,
        List<OrderItemResponse> orderItems,
        @JsonProperty("userData")
        UserResponse userResponse
) {
}
