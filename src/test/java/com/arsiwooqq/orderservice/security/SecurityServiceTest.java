package com.arsiwooqq.orderservice.security;

import com.arsiwooqq.orderservice.dto.OrderCreateRequest;
import com.arsiwooqq.orderservice.entity.Order;
import com.arsiwooqq.orderservice.enums.OrderStatus;
import com.arsiwooqq.orderservice.exception.AccessDeniedException;
import com.arsiwooqq.orderservice.repository.OrderRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityServiceTest {
    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private SecurityService securityService;

    @Nested
    @DisplayName("Ability of creating order")
    class CanCreateOrderTests {
        @Test
        @DisplayName("Should create order when principal's user id is not null")
        void givenRightUserId_whenCanCreateOrder_thenReturnTrue() {
            // Given
            var userId = UUID.randomUUID().toString();

            var request = new OrderCreateRequest(userId, new ArrayList<>());

            // When
            var result = securityService.canCreateOrder(userId, request);

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("Should throw exception when principal's user id is null")
        void givenDifferentUserId_whenCanCreateOrder_thenThrowsException() {
            var request = new OrderCreateRequest(UUID.randomUUID().toString(), new ArrayList<>());

            // When, Then
            assertThrows(AccessDeniedException.class,
                    () -> securityService.canCreateOrder(UUID.randomUUID().toString(), request));
        }

        @Test
        @DisplayName("Should throw exception when principal's user id is null")
        void givenNoUserId_whenCanCreateOrder_thenThrowsException() {
            // Given
            var request = new OrderCreateRequest(UUID.randomUUID().toString(), new ArrayList<>());

            // When, Then
            assertThrows(AccessDeniedException.class,
                    () -> securityService.canCreateOrder(null, request));
        }

        @Test
        @DisplayName("Should throw exception when request is null")
        void givenNoRequest_whenCanCreateOrder_thenThrowsException() {
            // Given
            var userId = UUID.randomUUID();

            // When, Then
            assertThrows(AccessDeniedException.class,
                    () -> securityService.canCreateOrder(userId.toString(), null));
        }

    }

    @Nested
    @DisplayName("Ability of accessing order")
    class CanAccessOrderTests {
        @Test
        @DisplayName("Should access order only with the principal's user id")
        void givenRightId_whenCanAccessOrder_thenReturnTrue() {
            // Given
            var userId = UUID.randomUUID().toString();
            var orderId = UUID.randomUUID();
            var order = getOrder(userId);

            // When
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            var result = securityService.canAccessOrder(userId, orderId);

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("Should throw exception when principal's user id and order's user id differ")
        void givenDifferentId_whenCanAccessOrder_thenThrowsException() {
            // Given
            var userId = UUID.randomUUID().toString();
            var order = getOrder(userId);

            // When, Then
            when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
            assertThrows(AccessDeniedException.class,
                    () -> securityService.canAccessOrder(UUID.randomUUID().toString(), order.getId()));
        }

        @Test
        @DisplayName("Should throw exception when principal's user id is null")
        void givenNoUserId_whenCanAccessOrder_thenThrowsException() {
            // Given, When, Then
            assertThrows(AccessDeniedException.class, 
                    () -> securityService.canAccessOrder(null, UUID.randomUUID()));
        }

        @Test
        @DisplayName("Should throw exception when order id is null")
        void givenNoOrderId_whenCanAccessOrder_thenThrowsException() {
            // Given, When, Then
            assertThrows(AccessDeniedException.class,
                    () -> securityService.canAccessOrder(UUID.randomUUID().toString(), null));
        }

        @Test
        @DisplayName("Should throw exception when order does not exist")
        void givenNotExistingOrderId_whenCanAccessOrder_thenThrowsException() {
            // Given
            var orderId = UUID.randomUUID();

            // When
            when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

            // Then
            assertThrows(AccessDeniedException.class,
                    () -> securityService.canAccessOrder(UUID.randomUUID().toString(), orderId));
        }
    }

    @Nested
    @DisplayName("Ability of managing order")
    class CanUpdateOrderTests {
        @Test
        @DisplayName("Should update order when it belongs to the principal and status is created")
        void givenOrderBelongsToUserAndStatusIsCreated_whenCanManageOrder_thenReturnTrue() {
            // Given
            var userId = UUID.randomUUID().toString();
            var order = getOrder(userId);

            // When
            when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
            var result = securityService.canManageOrder(userId, order.getId());

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("Should throw exception when order does not belong to the principal")
        void givenOrderDoesNotBelongToUser_whenCanManageOrder_thenThrowsException() {
            // Given
            var userId = UUID.randomUUID().toString();
            var order = getOrder(UUID.randomUUID().toString());

            // When
            when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

            // Then
            assertThrows(AccessDeniedException.class,
                    () -> securityService.canManageOrder(userId, order.getId()));
        }

        @Test
        @DisplayName("Should throw exception when order status is not CREATED")
        void givenOrderStatusIsNotCreated_whenCanManageOrder_thenThrowsException() {
            // Given
            var userId = UUID.randomUUID().toString();
            var order = getOrder(userId);
            order.setStatus(OrderStatus.PAID);

            // When
            when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

            // Then
            assertThrows(AccessDeniedException.class,
                    () -> securityService.canManageOrder(userId, order.getId()));
        }

        @Test
        @DisplayName("Should throw exception when principal's user id is null")
        void givenNoUserId_whenCanManageOrder_thenThrowsException() {
            // Given, When, Then
            assertThrows(AccessDeniedException.class,
                    () -> securityService.canManageOrder(null, UUID.randomUUID()));
        }

        @Test
        @DisplayName("Should throw exception when order id is null")
        void givenNoOrderId_whenCanManageOrder_thenThrowsException() {
            // Given, When, Then
            assertThrows(AccessDeniedException.class,
                    () -> securityService.canManageOrder(UUID.randomUUID().toString(), null));
        }
    }

    @Nested
    @DisplayName("Ability of accessing multiple orders")
    class CanAccessOrdersTests {
        @Test
        @DisplayName("Should access orders when all belong to the principal")
        void givenAllOrdersBelongToUser_whenCanAccessOrders_thenReturnTrue() {
            // Given
            var userId = UUID.randomUUID().toString();
            var order1 = getOrder(userId);
            var order2 = getOrder(userId);
            var orderIds = List.of(order1.getId(), order2.getId());
            var orders = List.of(order1, order2);

            // When
            when(orderRepository.findByIdIn(orderIds)).thenReturn(orders);
            var result = securityService.canAccessOrders(userId, orderIds);

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("Should return false when some orders don't belong to the principal")
        void givenSomeOrdersDontBelongToUser_whenCanAccessOrders_thenReturnFalse() {
            // Given
            var order1 = getOrder(UUID.randomUUID().toString());
            var order2 = getOrder(UUID.randomUUID().toString());
            var orderIds = List.of(order1.getId(), order2.getId());
            var orders = List.of(order1, order2);

            // When
            when(orderRepository.findByIdIn(orderIds)).thenReturn(orders);
            var result = securityService.canAccessOrders(UUID.randomUUID().toString(), orderIds);

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("Should throw exception when principal's user id is null")
        void givenNoUserId_whenCanAccessOrders_thenThrowsException() {
            // Given, When, Then
            assertThrows(AccessDeniedException.class,
                    () -> securityService.canAccessOrders(null, List.of(UUID.randomUUID())));
        }

        @Test
        @DisplayName("Should throw exception when order ids list is null")
        void givenNoOrderIds_whenCanAccessOrders_thenThrowsException() {
            // Given, When, Then
            assertThrows(AccessDeniedException.class,
                    () -> securityService.canAccessOrders(UUID.randomUUID().toString(), null));
        }
    }

    private Order getOrder(String userId) {
        return new Order(
                UUID.randomUUID(),
                userId,
                OrderStatus.CREATED,
                LocalDate.now(),
                new ArrayList<>()
        );
    }
}