package com.arsiwooqq.orderservice.dto;

import com.arsiwooqq.orderservice.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeOrderStatusRequest(
        @NotNull
        OrderStatus status
) {
}
