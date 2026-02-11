package com.arsiwooqq.orderservice.event;

import java.util.UUID;

public record OrderCreatedEvent(
        UUID orderId,
        String userId,
        Long paymentAmount
) {
}
