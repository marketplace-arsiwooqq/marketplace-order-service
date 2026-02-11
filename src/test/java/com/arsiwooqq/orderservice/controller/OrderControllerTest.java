package com.arsiwooqq.orderservice.controller;

import com.arsiwooqq.orderservice.dto.*;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.arsiwooqq.orderservice.entity.Item;
import com.arsiwooqq.orderservice.entity.Order;
import com.arsiwooqq.orderservice.entity.OrderItem;
import com.arsiwooqq.orderservice.enums.OrderStatus;
import com.arsiwooqq.orderservice.event.OrderCreatedEvent;
import com.arsiwooqq.orderservice.repository.ItemRepository;
import com.arsiwooqq.orderservice.repository.OrderItemRepository;
import com.arsiwooqq.orderservice.repository.OrderRepository;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc(addFilters = false)
@WireMockTest(httpPort = 51186)
class OrderControllerTest extends AbstractIntegrationTest {
    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private ConsumerFactory<String, Object> consumerFactory;

    @BeforeEach
    void clearRepositories() {
        orderRepository.deleteAll();
        itemRepository.deleteAll();
    }

    @DynamicPropertySource
    static void discoveryProps(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.discovery.client.simple.instances.user-service[0].instanceId", () -> "user1");
        registry.add("spring.cloud.discovery.client.simple.instances.user-service[0].serviceId", () -> "user-service");
        registry.add("spring.cloud.discovery.client.simple.instances.user-service[0].host", () -> "localhost");
        registry.add("spring.cloud.discovery.client.simple.instances.user-service[0].port", () -> 51186);
        registry.add("spring.cloud.discovery.client.simple.instances.user-service[0].secure", () -> false);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should create order when valid request provided")
    void givenValidRequest_whenCreate_thenSavesOrder() throws Exception {
        var item1 = createItem(1);
        var item2 = createItem(2);

        var orderItemRequest1 = new OrderItemRequest(item1.getId(), 2);
        var orderItemRequest2 = new OrderItemRequest(item2.getId(), 3);

        var request = new OrderCreateRequest(
                UUID.randomUUID().toString(),
                List.of(orderItemRequest1, orderItemRequest2)
        );

        stubFor(WireMock.get(urlMatching("/api/v1/users/.*")).willReturn(
                aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(
                                ApiResponse.success("User fetched", getUserResponse(request.userId()))))
        ));

        Consumer<String, Object> consumer = consumerFactory.createConsumer("TEST_GROUP", "TEST_CLIENT");
        consumer.subscribe(Collections.singleton("ORDER_CREATED"));

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpectAll(
                        jsonPath("$.success").value(true),
                        jsonPath("$.data.id").exists(),
                        jsonPath("$.data.userId").value(request.userId()),
                        jsonPath("$.data.status").value("CREATED"),
                        jsonPath("$.data.orderItems.length()").value(2),
                        jsonPath("$.data.orderItems[*].quantity",
                                Matchers.containsInAnyOrder(orderItemRequest1.quantity(), orderItemRequest2.quantity())
                        ),
                        jsonPath("$.data.userData.userId").value(request.userId()),
                        jsonPath("$.data.userData.name").value(getUserResponse(request.userId()).name())
                );

        var order = orderRepository.findAll().get(0);
        assertEquals(request.userId(), order.getUserId());
        assertEquals(OrderStatus.CREATED, order.getStatus());

        var orderItems = orderItemRepository.findAll();
        assertEquals(2, orderItems.size());

        var paymentAmount = orderItems.stream()
                .mapToLong(orderItem -> orderItem.getItem().getPrice() * orderItem.getQuantity())
                .sum();

        ConsumerRecord<String, Object> record =
                KafkaTestUtils.getSingleRecord(consumer, "ORDER_CREATED", Duration.ofSeconds(10));
        var event = (OrderCreatedEvent) record.value();

        assertEquals(order.getId(), event.orderId());
        assertEquals(order.getUserId(), event.userId());
        assertEquals(paymentAmount, event.paymentAmount());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return bad request when invalid request provided")
    void givenInvalidRequest_whenCreate_thenReturnsBadRequest() throws Exception {
        var request = new OrderCreateRequest(
                null,
                null
        );

        Consumer<String, Object> consumer = consumerFactory.createConsumer("TEST_GROUP", "TEST_CLIENT");
        consumer.subscribe(Collections.singleton("ORDER_CREATED"));

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        assertEquals(0, orderRepository.count());
        ConsumerRecords<String, Object> record = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(2));
        assertEquals(0, record.count());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should get order when valid id provided")
    void givenValidId_whenGetById_thenReturnsOrder() throws Exception {
        var order = createOrderWithItems();

        stubFor(WireMock.get(urlMatching("/api/v1/users/.*")).willReturn(
                aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(
                                ApiResponse.success("User fetched", getUserResponse(order.getUserId()))))
        ));

        mockMvc.perform(get("/api/v1/orders/{id}", order.getId()))
                .andExpect(status().isOk())
                .andExpectAll(
                        jsonPath("$.success").value(true),
                        jsonPath("$.data.id").value(order.getId().toString()),
                        jsonPath("$.data.userId").value(order.getUserId()),
                        jsonPath("$.data.status").value(order.getStatus().toString()),
                        jsonPath("$.data.orderItems.length()").value(2),
                        jsonPath("$.data.userData.userId").value(order.getUserId()),
                        jsonPath("$.data.userData.name").value(getUserResponse(order.getUserId()).name())
                );
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return not found when order does not exist")
    void givenNotExistingId_whenGetById_thenReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/orders/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return bad request when id not valid")
    void givenInvalidId_whenGetById_thenReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/orders/{id}", "123"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should get all orders by ids")
    void givenValidIds_whenGetAllByIds_thenReturnsOrders() throws Exception {
        var order1 = createOrderWithItems();
        var order2 = createOrderWithItems();

        stubFor(WireMock.get(urlMatching("/api/v1/users/" + order1.getUserId())).willReturn(
                aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(
                                ApiResponse.success("User fetched", getUserResponse(order1.getUserId()))))
        ));
        stubFor(WireMock.get(urlMatching("/api/v1/users/" + order2.getUserId())).willReturn(
                aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(
                                ApiResponse.success("User fetched", getUserResponse(order2.getUserId()))))
        ));

        mockMvc.perform(get("/api/v1/orders")
                        .param("ids", order1.getId().toString() + "," + order2.getId().toString()))
                .andExpect(status().isOk())
                .andExpectAll(
                        jsonPath("$.success").value(true),
                        jsonPath("$.data.length()").value(2),
                        jsonPath("$.data[*].userData.userId",
                                Matchers.containsInAnyOrder(order1.getUserId(),
                                        order2.getUserId())
                        )
                );
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return empty list when no orders found")
    void givenNotExistingIds_whenGetAllByIds_thenReturnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/v1/orders")
                        .param("ids", UUID.randomUUID() + "," + UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpectAll(
                        jsonPath("$.success").value(true),
                        jsonPath("$.data.length()").value(0)
                );
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should get all orders by statuses")
    void givenValidStatuses_whenGetAllByStatuses_thenReturnsOrders() throws Exception {
        var order1 = createOrderWithItems();
        var order2 = createOrderWithItems();

        stubFor(WireMock.get(urlMatching("/api/v1/users/" + order1.getUserId())).willReturn(
                aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(
                                ApiResponse.success("User fetched", getUserResponse(order1.getUserId()))))
        ));
        stubFor(WireMock.get(urlMatching("/api/v1/users/" + order2.getUserId())).willReturn(
                aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(
                                ApiResponse.success("User fetched", getUserResponse(order2.getUserId()))))
        ));

        mockMvc.perform(get("/api/v1/orders")
                        .param("statuses", "CREATED,CANCELED"))
                .andExpect(status().isOk())
                .andExpectAll(
                        jsonPath("$.success").value(true),
                        jsonPath("$.data.length()").value(2),
                        jsonPath("$.data[*].userData.userId",
                                Matchers.containsInAnyOrder(order1.getUserId(),
                                        order2.getUserId())
                        )
                );
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return empty list when no orders found")
    void givenNonExistingStatuses_whenGetAllByStatuses_thenReturnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/v1/orders")
                        .param("statuses", "NONEXISTENT_STATUS"))
                .andExpect(status().isOk())
                .andExpectAll(
                        jsonPath("$.success").value(true),
                        jsonPath("$.data.length()").value(0)
                );
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should update order when valid request provided")
    void givenValidRequest_whenUpdate_thenUpdatesOrder() throws Exception {
        var order = createOrderWithItems();
        var item1 = createItem(3);
        var item2 = createItem(4);

        var orderItemRequest1 = new OrderItemRequest(item1.getId(), 5);
        var orderItemRequest2 = new OrderItemRequest(item2.getId(), 6);

        var request = new OrderUpdateRequest(
                List.of(orderItemRequest1, orderItemRequest2)
        );

        stubFor(WireMock.get(urlMatching("/api/v1/users/" + order.getUserId())).willReturn(
                aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody(objectMapper.writeValueAsString(
                                ApiResponse.success("User fetched", getUserResponse(order.getUserId()))))
        ));

        mockMvc.perform(patch("/api/v1/orders/{id}", order.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpectAll(
                        jsonPath("$.success").value(true),
                        jsonPath("$.data.id").value(order.getId().toString()),
                        jsonPath("$.data.userId").value(order.getUserId()),
                        jsonPath("$.data.orderItems.length()").value(2),
                        jsonPath("$.data.orderItems[0].quantity").value(orderItemRequest1.quantity()),
                        jsonPath("$.data.orderItems[1].quantity").value(orderItemRequest2.quantity()),
                        jsonPath("$.data.userData.userId").value(order.getUserId()),
                        jsonPath("$.data.userData.name").value(getUserResponse(order.getUserId()).name())
                );

        var updatedOrder = orderRepository.findById(order.getId()).orElse(null);
        assertNotNull(updatedOrder);
        assertEquals(order.getUserId(), updatedOrder.getUserId());
        var orderItems = orderItemRepository.findAll();
        assertEquals(2, orderItems.size());
    }


    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return bad request when invalid request provided")
    void givenInvalidRequest_whenUpdate_thenReturnsBadRequest() throws Exception {
        var order = createOrderWithItems();

        var request = new OrderUpdateRequest(
                null
        );

        mockMvc.perform(patch("/api/v1/orders/{id}", order.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        var notUpdatedOrder = orderRepository.findById(order.getId()).orElse(null);
        assertNotNull(notUpdatedOrder);
        assertEquals(order.getUserId(), notUpdatedOrder.getUserId());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return not found when order does not exist")
    void givenNotExistingId_whenUpdate_thenReturnsNotFound() throws Exception {
        var item1 = createItem(1);
        var orderItemRequest1 = new OrderItemRequest(item1.getId(), 1);

        var request = new OrderUpdateRequest(
                List.of(orderItemRequest1)
        );

        mockMvc.perform(patch("/api/v1/orders/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should change order status")
    void givenValidRequest_whenChangeStatus_thenChangesStatus() throws Exception {
        var order = createOrderWithItems();

        var request = new ChangeOrderStatusRequest(
                OrderStatus.CANCELED
        );

        stubFor(WireMock.get(urlMatching("/api/v1/users/" + order.getUserId())).willReturn(
                aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody(objectMapper.writeValueAsString(
                                ApiResponse.success("User fetched", getUserResponse(order.getUserId()))))
        ));

        mockMvc.perform(patch("/api/v1/orders/{id}/status", order.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpectAll(
                        jsonPath("$.success").value(true),
                        jsonPath("$.data.id").value(order.getId().toString()),
                        jsonPath("$.data.userId").value(order.getUserId()),
                        jsonPath("$.data.status").value(request.status().name()),
                        jsonPath("$.data.userData.userId").value(order.getUserId()),
                        jsonPath("$.data.userData.name").value(getUserResponse(order.getUserId()).name())
                );

        var updatedOrder = orderRepository.findById(order.getId()).orElse(null);
        assertNotNull(updatedOrder);
        assertEquals(request.status(), updatedOrder.getStatus());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return bad request when invalid request provided")
    void givenInvalidRequest_whenChangeStatus_thenReturnsBadRequest() throws Exception {
        var order = createOrderWithItems();

        var request = new ChangeOrderStatusRequest(
                null
        );

        mockMvc.perform(patch("/api/v1/orders/{id}/status", order.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should delete order when valid id provided")
    void givenValidId_whenDelete_thenDeletesOrder() throws Exception {
        var order = createOrderWithItems();

        mockMvc.perform(delete("/api/v1/orders/{id}", order.getId()))
                .andExpect(status().isOk())
                .andExpectAll(
                        jsonPath("$.success").value(true)
                );

        var deletedOrder = orderRepository.findById(order.getId()).orElse(null);
        assertNull(deletedOrder);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return not found when invalid id provided")
    void givenInvalidId_whenDelete_thenReturnsNotFound() throws Exception {
        mockMvc.perform(delete("/api/v1/orders/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Nested
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Exception handling")
    class ExceptionHandlingTests {
        @Test
        @DisplayName("Should return bad request for invalid request body")
        void givenInvalidRequestBody_whenCreateOrder_thenReturnsBadRequest() throws Exception {
            var json = "invalid json";

            mockMvc.perform(post("/api/v1/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("Should return bad request for invalid request")
        void givenInvalidRequest_whenCreateOrder_thenReturnsBadRequest() throws Exception {
            var request = new OrderCreateRequest(null, null);

            mockMvc.perform(post("/api/v1/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("Should return bad request for type mismatch in path variable")
        void givenInvalidUuid_whenGetOrder_thenReturnsBadRequest() throws Exception {
            mockMvc.perform(get("/api/v1/orders/invalid-uuid"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("Should return not found for non-existent endpoint")
        void givenNonExistentEndpoint_whenAccess_thenReturnsNotFound() throws Exception {
            mockMvc.perform(get("/api/v1/non-existent"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("Should return bad request for missing required parameter")
        void givenMissingRequiredParameter_whenGetOrdersByIds_thenReturnsBadRequest() throws Exception {
            mockMvc.perform(get("/api/v1/orders")
                            .param("invalid-param", "value"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("Should return method not allowed for unsupported HTTP method")
        void givenUnsupportedHttpMethod_whenAccessOrders_thenReturnsMethodNotAllowed() throws Exception {
            mockMvc.perform(patch("/api/v1/orders"))
                    .andExpect(status().isMethodNotAllowed())
                    .andExpect(jsonPath("$.success").value(false));
        }
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

    private UserResponse getUserResponse(String userId) {
        return new UserResponse(
                userId,
                "TEST_NAME",
                "TEST_SURNAME",
                LocalDate.now().minusDays(1),
                "TEST@EMAIL"
        );
    }
}