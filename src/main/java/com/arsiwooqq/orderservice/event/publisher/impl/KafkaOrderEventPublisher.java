package com.arsiwooqq.orderservice.event.publisher.impl;

import com.arsiwooqq.orderservice.event.OrderCreatedEvent;
import com.arsiwooqq.orderservice.event.publisher.OrderEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaOrderEventPublisher implements OrderEventPublisher {

    @Value("${kafka.producer.topics.order-created.name}")
    private String topicName;

    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;

    @Override
    public void publishOrderCreated(OrderCreatedEvent event) {
        var future = kafkaTemplate.send(topicName, event.orderId().toString(), event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Error sending message to Kafka: {}", ex.getMessage());
            } else {
                log.debug("Message sent to Kafka: {}", result);
            }
        });
    }
}
