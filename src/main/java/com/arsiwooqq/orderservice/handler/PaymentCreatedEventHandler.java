package com.arsiwooqq.orderservice.handler;

import com.arsiwooqq.orderservice.dto.ChangeOrderStatusRequest;
import com.arsiwooqq.orderservice.enums.OrderStatus;
import com.arsiwooqq.orderservice.enums.PaymentStatus;
import com.arsiwooqq.orderservice.event.PaymentCreatedEvent;
import com.arsiwooqq.orderservice.exception.NotRetryableException;
import com.arsiwooqq.orderservice.exception.OrderNotFoundException;
import com.arsiwooqq.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentCreatedEventHandler {

    private final OrderService orderService;

    @KafkaListener(topics = "PAYMENT_CREATED")
    public void handle(PaymentCreatedEvent event) {
        if (event.status() == PaymentStatus.PAID) {
            try {
                orderService.changeStatus(event.orderId(), new ChangeOrderStatusRequest(OrderStatus.PAID));
            } catch (OrderNotFoundException e) {
                throw new NotRetryableException(e);
            }
        }
    }
}
