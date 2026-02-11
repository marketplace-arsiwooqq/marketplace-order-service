package com.arsiwooqq.orderservice.service.impl;

import com.arsiwooqq.orderservice.dto.*;
import com.arsiwooqq.orderservice.entity.Item;
import com.arsiwooqq.orderservice.entity.Order;
import com.arsiwooqq.orderservice.entity.OrderItem;
import com.arsiwooqq.orderservice.enums.OrderStatus;
import com.arsiwooqq.orderservice.event.OrderCreatedEvent;
import com.arsiwooqq.orderservice.event.publisher.OrderEventPublisher;
import com.arsiwooqq.orderservice.exception.OrderNotFoundException;
import com.arsiwooqq.orderservice.mapper.OrderMapper;
import com.arsiwooqq.orderservice.repository.OrderRepository;
import com.arsiwooqq.orderservice.service.OrderItemService;
import com.arsiwooqq.orderservice.service.UserDataService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {
    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private OrderItemService orderItemService;

    @Mock
    private UserDataService userDataService;

    @Mock
    private OrderEventPublisher orderEventPublisher;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Nested
    @DisplayName("Create order")
    class CreateOrderTests {
        @Test
        @DisplayName("Should create order")
        void givenOrder_whenCreate_thenCreatesOrder() {
            // Given
            var request = getOrderCreateRequest();
            var order = getOrder(request);
            var orderItem1 = request.orderItems().get(0);
            var orderItem2 = request.orderItems().get(1);

            order.addOrderItem(getOrderItem(orderItem1.itemId(), orderItem1.quantity()));
            order.addOrderItem(getOrderItem(orderItem2.itemId(), orderItem2.quantity()));

            var userData = getUserData(request.userId());

            var paymentAmount = order.getOrderItems().stream()
                    .mapToLong(orderItem -> orderItem.getItem().getPrice() * orderItem.getQuantity())
                    .sum();
            var event = new OrderCreatedEvent(order.getId(), order.getUserId(), paymentAmount);

            // When
            when(orderMapper.toEntity(request)).thenReturn(order);
            when(orderItemService.create(request.orderItems().get(0))).thenReturn(
                    getOrderItem(orderItem1.itemId(), orderItem1.quantity()));
            when(orderItemService.create(request.orderItems().get(1))).thenReturn(
                    getOrderItem(orderItem2.itemId(), orderItem2.quantity()));
            when(userDataService.fetchUserData(request.userId())).thenReturn(userData);
            when(orderRepository.save(order)).thenReturn(order);
            when(orderMapper.toResponse(order, userData)).thenReturn(getOrderResponse(order));
            when(orderMapper.toOrderCreatedEvent(order, paymentAmount)).thenReturn(event);

            var orderResponse = orderService.create(request);

            assertAll(
                    () -> assertEquals(request.userId(), orderResponse.userId()),
                    () -> assertEquals(request.orderItems().size(), orderResponse.orderItems().size()),
                    () -> assertEquals(request.orderItems().get(0).itemId(),
                            orderResponse.orderItems().get(0).item().getId()),
                    () -> assertEquals(request.orderItems().get(0).quantity(),
                            orderResponse.orderItems().get(0).quantity()),
                    () -> assertEquals(request.orderItems().get(1).itemId(),
                            orderResponse.orderItems().get(1).item().getId()),
                    () -> assertEquals(request.orderItems().get(1).quantity(),
                            orderResponse.orderItems().get(1).quantity()),
                    () -> assertEquals(userData, orderResponse.userResponse())
            );

            verify(orderMapper, times(1)).toEntity(request);
            verify(orderItemService, times(2)).create(any(OrderItemRequest.class));
            verify(userDataService, times(1)).fetchUserData(request.userId());
            verify(orderRepository, times(1)).save(order);
            verify(orderMapper, times(1)).toResponse(order, userData);
            verify(orderMapper, times(1)).toOrderCreatedEvent(order, paymentAmount);
            verify(orderEventPublisher, times(1)).publishOrderCreated(event);
        }
    }

    @Nested
    @DisplayName("Get order by id")
    class GetByIdTests {
        @Test
        @DisplayName("Should return order response")
        void givenId_whenGetById_thenReturnsOrderResponse() {
            // Given
            var order = createOrderWithItems();
            var userData = getUserData(order.getUserId());

            // When
            when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
            when(userDataService.fetchUserData(order.getUserId())).thenReturn(userData);
            when(orderMapper.toResponse(order, userData)).thenReturn(getOrderResponse(order));

            var result = orderService.getById(order.getId());

            // Then
            assertAll(
                    () -> assertEquals(order.getId(), result.id()),
                    () -> assertEquals(order.getUserId(), result.userId()),
                    () -> assertEquals(order.getStatus(), result.status()),
                    () -> assertEquals(order.getCreationDate(), result.creationDate()),
                    () -> assertEquals(order.getOrderItems().get(0).getItem(), result.orderItems().get(0).item()),
                    () -> assertEquals(userData, result.userResponse())
            );

            verify(orderRepository, times(1)).findById(order.getId());
            verify(userDataService, times(1)).fetchUserData(any(String.class));
            verify(orderMapper, times(1)).toResponse(any(Order.class), any(UserResponse.class));
        }

        @Test
        @DisplayName("Should throw OrderNotFoundException when order not found")
        void givenNotExistingId_whenUpdate_thenThrowsException() {
            // Given
            var id = UUID.randomUUID();

            // When
            when(orderRepository.findById(id)).thenReturn(Optional.empty());

            // Then
            assertThrows(OrderNotFoundException.class, () -> orderService.getById(id));

            verify(orderRepository, times(1)).findById(id);
            verify(userDataService, never()).fetchUserData(any(String.class));
            verify(orderMapper, never()).toResponse(any(Order.class), any(UserResponse.class));
        }
    }

    @Nested
    @DisplayName("Get all orders by ids")
    class GetAllByIdsTests {
        @Test
        @DisplayName("Should return all orders by ids")
        void givenIds_whenGetAllByIds_thenReturnOrders() {
            // Given
            var order1 = createOrderWithItems();
            var order2 = createOrderWithItems();
            var userData1 = getUserData(order1.getUserId());
            var userData2 = getUserData(order2.getUserId());

            var request = List.of(order1.getId(), order2.getId());

            // When
            when(orderRepository.findByIdIn(request)).thenReturn(List.of(order1, order2));
            when(userDataService.fetchUserData(order1.getUserId())).thenReturn(userData1);
            when(userDataService.fetchUserData(order2.getUserId())).thenReturn(userData2);
            when(orderMapper.toResponse(order1, userData1)).thenReturn(getOrderResponse(order1));
            when(orderMapper.toResponse(order2, userData2)).thenReturn(getOrderResponse(order2));

            var result = orderService.getAllByIds(request);

            // Then
            assertAll(
                    () -> assertEquals(request.size(), result.size()),
                    () -> assertEquals(order1.getId(), result.get(0).id()),
                    () -> assertEquals(order1.getUserId(), result.get(0).userId()),
                    () -> assertEquals(order1.getStatus(), result.get(0).status()),
                    () -> assertEquals(order1.getCreationDate(), result.get(0).creationDate()),
                    () -> assertEquals(order1.getOrderItems().get(0).getItem(),
                            result.get(0).orderItems().get(0).item()),
                    () -> assertEquals(userData1, result.get(0).userResponse()),
                    () -> assertEquals(order2.getId(), result.get(1).id()),
                    () -> assertEquals(order2.getUserId(), result.get(1).userId()),
                    () -> assertEquals(order2.getStatus(), result.get(1).status()),
                    () -> assertEquals(order2.getCreationDate(), result.get(1).creationDate()),
                    () -> assertEquals(order2.getOrderItems().get(0).getItem(),
                            result.get(1).orderItems().get(0).item()),
                    () -> assertEquals(userData2, result.get(1).userResponse())
            );

            verify(orderRepository, times(1)).findByIdIn(request);
            verify(userDataService, times(2)).fetchUserData(any(String.class));
            verify(orderMapper, times(2)).toResponse(any(Order.class), any(UserResponse.class));
        }

        @Test
        @DisplayName("Should return empty list when no orders found")
        void givenNoOrders_whenGetAllByIds_thenReturnEmptyList() {
            // Given
            var request = List.of(UUID.randomUUID(), UUID.randomUUID());

            // When
            when(orderRepository.findByIdIn(request)).thenReturn(List.of());

            // Then
            assertTrue(orderService.getAllByIds(request).isEmpty());

            verify(orderRepository, times(1)).findByIdIn(request);
            verify(userDataService, never()).fetchUserData(any());
            verify(orderMapper, never()).toResponse(any(), any());
        }
    }

    @Nested
    @DisplayName("Get all orders by statuses")
    class GetALlByStatusesTests {
        @Test
        @DisplayName("Should return all orders by statuses")
        void givenStatuses_whenGetAllByStatuses_thenReturnOrders() {
            // Given
            var order1 = createOrderWithItems();
            var order2 = createOrderWithItems();
            var userData1 = getUserData(order1.getUserId());
            var userData2 = getUserData(order2.getUserId());

            var request = List.of(OrderStatus.CREATED.toString(), OrderStatus.CANCELED.toString());

            var orders = List.of(order1, order2);

            // When
            when(orderRepository.findByStatusIn(any())).thenReturn(orders);
            when(userDataService.fetchUserData(order1.getUserId())).thenReturn(userData1);
            when(userDataService.fetchUserData(order2.getUserId())).thenReturn(userData2);
            when(orderMapper.toResponse(order1, userData1)).thenReturn(getOrderResponse(order1));
            when(orderMapper.toResponse(order2, userData2)).thenReturn(getOrderResponse(order2));

            var result = orderService.getAllByStatuses(request);

            // Then
            assertAll(
                    () -> assertEquals(orders.size(), result.size()),
                    () -> assertEquals(order1.getId(), result.get(0).id()),
                    () -> assertEquals(order1.getUserId(), result.get(0).userId()),
                    () -> assertEquals(order1.getStatus(), result.get(0).status()),
                    () -> assertEquals(order1.getCreationDate(), result.get(0).creationDate()),
                    () -> assertEquals(order1.getOrderItems().get(0).getItem(),
                            result.get(0).orderItems().get(0).item()),
                    () -> assertEquals(userData1, result.get(0).userResponse()),
                    () -> assertEquals(order2.getId(), result.get(1).id()),
                    () -> assertEquals(order2.getUserId(), result.get(1).userId()),
                    () -> assertEquals(order2.getStatus(), result.get(1).status()),
                    () -> assertEquals(order2.getCreationDate(), result.get(1).creationDate()),
                    () -> assertEquals(order2.getOrderItems().get(0).getItem(),
                            result.get(1).orderItems().get(0).item()),
                    () -> assertEquals(userData2, result.get(1).userResponse())
            );

            verify(orderRepository, times(1)).findByStatusIn(any());
            verify(userDataService, times(2)).fetchUserData(any(String.class));
            verify(orderMapper, times(2)).toResponse(any(Order.class), any(UserResponse.class));
        }

        @Test
        @DisplayName("Should return empty list when no orders found")
        void givenNoOrders_whenGetAllByStatuses_thenReturnEmptyList() {
            // Given
            var request = List.of(OrderStatus.CREATED.toString(), OrderStatus.CANCELED.toString());

            // When
            when(orderRepository.findByStatusIn(any())).thenReturn(List.of());

            var result = orderService.getAllByStatuses(request);

            // Then
            assertTrue(result.isEmpty());

            verify(orderRepository, times(1)).findByStatusIn(any());
            verify(userDataService, never()).fetchUserData(any());
            verify(orderMapper, never()).toResponse(any(), any());
        }
    }

    @Nested
    @DisplayName("Update order")
    class UpdateOrderTests {
        @Test
        @DisplayName("Should update order")
        void givenOrder_whenUpdate_thenReturnOrder() {
            // Given
            var order = createOrderWithItems();
            var userData = getUserData(order.getUserId());

            var request = getOrderUpdateRequest();

            var orderItemRequest1 = request.orderItems().get(0);
            var orderItemRequest2 = request.orderItems().get(1);

            var updatedOrderItem1 = getOrderItem(orderItemRequest1.itemId(), orderItemRequest1.quantity());
            var updatedOrderItem2 = getOrderItem(orderItemRequest2.itemId(), orderItemRequest2.quantity());

            var updatedOrder = new Order(order.getId(), order.getUserId(), order.getStatus(), order.getCreationDate(),
                    new ArrayList<>());
            updatedOrder.addOrderItem(updatedOrderItem1);
            updatedOrder.addOrderItem(updatedOrderItem2);

            // When
            when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
            when(orderItemService.create(orderItemRequest1)).thenReturn(updatedOrderItem1);
            when(orderItemService.create(orderItemRequest2)).thenReturn(updatedOrderItem2);
            when(orderRepository.save(any())).thenReturn(updatedOrder);
            when(userDataService.fetchUserData(updatedOrder.getUserId())).thenReturn(userData);
            when(orderMapper.toResponse(updatedOrder, userData)).thenReturn(getOrderResponse(updatedOrder));

            var result = orderService.update(order.getId(), request);

            assertAll(
                    () -> assertEquals(order.getId(), result.id()),
                    () -> assertEquals(order.getUserId(), result.userId()),
                    () -> assertEquals(2, result.orderItems().size()),
                    () -> assertEquals(orderItemRequest1.itemId(), result.orderItems().get(0).item().getId()),
                    () -> assertEquals(orderItemRequest1.quantity(), result.orderItems().get(0).quantity()),
                    () -> assertEquals(orderItemRequest2.itemId(), result.orderItems().get(1).item().getId()),
                    () -> assertEquals(orderItemRequest2.quantity(), result.orderItems().get(1).quantity())
            );

            verify(orderRepository, times(1)).findById(order.getId());
            verify(orderRepository, times(1)).save(order);
            verify(orderItemService, times(2)).create(any());
            verify(userDataService, times(1)).fetchUserData(any());
            verify(orderMapper, times(1)).toResponse(any(), any());
        }

        @Test
        @DisplayName("Should throw OrderNotFoundException when order not found")
        void givenOrder_whenUpdate_thenThrowOrderNotFoundException() {
            // Given
            var order = createOrderWithItems();

            var request = getOrderUpdateRequest();

            // When
            when(orderRepository.findById(order.getId())).thenReturn(Optional.empty());

            // Then
            assertThrows(OrderNotFoundException.class, () -> orderService.update(order.getId(), request));

            verify(orderRepository, times(1)).findById(order.getId());
            verify(orderRepository, never()).save(order);
            verify(orderItemService, never()).create(any());
            verify(userDataService, never()).fetchUserData(any());
            verify(orderMapper, never()).toResponse(any(), any());
        }
    }

    @Nested
    @DisplayName("Change order status")
    class ChangeOrderStatusTests {
        @Test
        @DisplayName("Should change order status")
        void givenOrder_whenChangeStatus_thenChangeStatus() {
            // Given
            var order = createOrderWithItems();
            var userData = getUserData(order.getUserId());
            var request = new ChangeOrderStatusRequest(OrderStatus.DELIVERED);

            // When
            when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
            when(userDataService.fetchUserData(order.getUserId())).thenReturn(userData);
            when(orderRepository.save(order)).thenReturn(order);
            when(orderMapper.toResponse(order, userData)).thenReturn(getOrderResponse(order));

            var result = orderService.changeStatus(order.getId(), request);

            // Then
            assertAll(
                    () -> assertEquals(order.getId(), result.id()),
                    () -> assertEquals(request.status(), order.getStatus()),
                    () -> assertEquals(order.getUserId(), result.userId()),
                    () -> assertEquals(2, result.orderItems().size())
            );

            verify(orderRepository, times(1)).findById(order.getId());
            verify(orderRepository, times(1)).save(order);
            verify(userDataService, times(1)).fetchUserData(order.getUserId());
            verify(orderMapper, times(1)).toResponse(order, userData);
        }

        @Test
        @DisplayName("Should throw OrderNotFoundException when order not found")
        void givenOrder_whenChangeStatus_thenThrowOrderNotFoundException() {
            // Given
            var order = createOrderWithItems();
            var request = new ChangeOrderStatusRequest(OrderStatus.DELIVERED);

            // When
            when(orderRepository.findById(order.getId())).thenReturn(Optional.empty());

            // Then
            assertThrows(OrderNotFoundException.class, () -> orderService.changeStatus(order.getId(), request));
        }
    }

    @Nested
    @DisplayName("Delete order")
    class DeleteOrderTests {
        @Test
        @DisplayName("Should delete order")
        void givenOrder_whenDelete_thenDeleteOrder() {
            // Given
            var order = createOrderWithItems();

            // When
            when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

            // Then
            orderService.delete(order.getId());

            verify(orderRepository, times(1)).findById(order.getId());
            verify(orderRepository, times(1)).delete(order);
        }

        @Test
        @DisplayName("Should throw OrderNotFoundException when order not found")
        void givenOrder_whenDelete_thenThrowOrderNotFoundException() {
            // Given
            var order = createOrderWithItems();

            // When
            when(orderRepository.findById(order.getId())).thenReturn(Optional.empty());

            // Then
            assertThrows(OrderNotFoundException.class, () -> orderService.delete(order.getId()));

            verify(orderRepository, times(1)).findById(order.getId());
            verify(orderRepository, never()).delete(order);
        }
    }

    private Order createOrderWithItems() {
        var orderItem1 = getOrderItem(UUID.randomUUID(), 1);
        var orderItem2 = getOrderItem(UUID.randomUUID(), 2);
        var order = getOrder(new OrderCreateRequest(UUID.randomUUID().toString(), null));
        order.addOrderItem(orderItem1);
        order.addOrderItem(orderItem2);
        return order;
    }

    private OrderCreateRequest getOrderCreateRequest() {
        var orderItem1 = new OrderItemRequest(
                UUID.randomUUID(),
                1
        );

        var orderItem2 = new OrderItemRequest(
                UUID.randomUUID(),
                3
        );

        return new OrderCreateRequest(
                UUID.randomUUID().toString(),
                List.of(orderItem1, orderItem2)
        );
    }

    private OrderUpdateRequest getOrderUpdateRequest() {
        var orderItem1 = new OrderItemRequest(
                UUID.randomUUID(),
                5
        );

        var orderItem2 = new OrderItemRequest(
                UUID.randomUUID(),
                7
        );

        return new OrderUpdateRequest(
                List.of(orderItem1, orderItem2)
        );
    }

    private Order getOrder(OrderCreateRequest request) {
        return new Order(
                UUID.randomUUID(),
                request.userId(),
                OrderStatus.CREATED,
                LocalDate.now(),
                new ArrayList<>()
        );
    }

    private Item getItem(UUID id) {
        return new Item(
                id,
                "ITEM_NAME",
                100L
        );
    }

    private OrderItem getOrderItem(UUID itemId, Integer quantity) {
        var orderItem = new OrderItem();
        orderItem.setId(UUID.randomUUID());
        orderItem.setQuantity(quantity);
        orderItem.setItem(getItem(itemId));
        return orderItem;
    }

    private UserResponse getUserData(String userId) {
        return new UserResponse(
                userId,
                "TEST_NAME",
                "TEST_SURNAME",
                LocalDate.now().minusDays(1),
                "TEST@EMAIL"
        );
    }

    private OrderResponse getOrderResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                order.getStatus(),
                order.getCreationDate(),
                order.getOrderItems()
                        .stream()
                        .map(orderItem -> new OrderItemResponse(
                                orderItem.getItem(),
                                orderItem.getQuantity()
                        ))
                        .toList(),
                getUserData(order.getUserId())
        );
    }
}
