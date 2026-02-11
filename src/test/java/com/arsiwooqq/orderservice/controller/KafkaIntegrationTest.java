package com.arsiwooqq.orderservice.controller;

import com.arsiwooqq.orderservice.entity.Item;
import com.arsiwooqq.orderservice.entity.Order;
import com.arsiwooqq.orderservice.entity.OrderItem;
import com.arsiwooqq.orderservice.enums.OrderStatus;
import com.arsiwooqq.orderservice.enums.PaymentStatus;
import com.arsiwooqq.orderservice.event.PaymentCreatedEvent;
import com.arsiwooqq.orderservice.repository.ItemRepository;
import com.arsiwooqq.orderservice.repository.OrderRepository;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(KafkaIntegrationTest.TestKafkaConfig.class)
public class KafkaIntegrationTest extends AbstractIntegrationTest {
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private ConsumerFactory<String, Object> consumerFactory;

    @Test
    @DisplayName("Should handle PAYMENT_CREATED event")
    void givenOrderCreatedEvent_whenHandle_thenCreatePayment() {
        String topic = "PAYMENT_CREATED";
        var order = createOrderWithItems();
        kafkaTemplate.send(topic, new PaymentCreatedEvent(order.getId(), PaymentStatus.PAID));

        Awaitility.await()
                .atMost(10, TimeUnit.SECONDS)
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    var entity = orderRepository.findById(order.getId()).orElseThrow();
                    assertThat(entity).isNotNull();
                    assertThat(entity.getId()).isEqualTo(order.getId());
                    assertThat(entity.getStatus()).isEqualTo(OrderStatus.PAID);
                });
    }

    @Test
    @DisplayName("Should sent event to DLT topic when exception occurs")
    void givenNotExistingOrder_whenHandle_thenSendToDLTTopic() {
        String topic = "PAYMENT_CREATED";
        String dltTopic = "PAYMENT_CREATED-dlt";

        var orderId = UUID.randomUUID();

        Consumer<String, Object> dltConsumer = consumerFactory.createConsumer("TEST-GROUP", "TEST-CLIENT");
        dltConsumer.subscribe(Collections.singleton(dltTopic));
        kafkaTemplate.send(topic, new PaymentCreatedEvent(orderId, PaymentStatus.PAID));

        ConsumerRecord<String, Object> dltRecord = KafkaTestUtils.getSingleRecord(dltConsumer, dltTopic, Duration.ofSeconds(10));

        var event = (PaymentCreatedEvent) dltRecord.value();
        assertThat(event.orderId()).isEqualTo(orderId);
    }

    private Order createOrderWithItems() {
        var item1 = createItem(1);
        var item2 = createItem(2);

        var orderItem1 = new OrderItem();
        orderItem1.setItem(item1);
        orderItem1.setQuantity(1);

        var orderItem2 = new OrderItem();
        orderItem2.setItem(item2);
        orderItem2.setQuantity(2);

        var order = new Order();
        order.setUserId(UUID.randomUUID().toString());
        order.setStatus(OrderStatus.CREATED);
        order.setCreationDate(LocalDate.now());
        order.setOrderItems(new ArrayList<>(List.of(orderItem1, orderItem2)));

        orderItem1.setOrder(order);
        orderItem2.setOrder(order);

        return orderRepository.save(order);
    }

    private Item createItem(int i) {
        var item = new Item();
        item.setName("TEST_ITEM_" + i);
        item.setPrice(100L * i);
        return itemRepository.save(item);
    }

    @TestConfiguration
    static class TestKafkaConfig {
        @Bean
        public NewTopic paymentCreatedDltTopic() {
            return TopicBuilder.name("PAYMENT_CREATED-dlt")
                    .partitions(1)
                    .replicas(1)
                    .build();
        }
    }
}
