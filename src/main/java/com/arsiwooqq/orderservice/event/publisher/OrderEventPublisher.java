package com.arsiwooqq.orderservice.event.publisher;

import com.arsiwooqq.orderservice.event.OrderCreatedEvent;

public interface OrderEventPublisher {
    void publishOrderCreated(OrderCreatedEvent event);
}
