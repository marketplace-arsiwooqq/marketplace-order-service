package com.arsiwooqq.orderservice.event;

import com.arsiwooqq.orderservice.enums.PaymentStatus;

import java.util.UUID;

public record PaymentCreatedEvent(
        UUID orderId,
        PaymentStatus status
) {}
