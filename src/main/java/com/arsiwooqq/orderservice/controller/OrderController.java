package com.arsiwooqq.orderservice.controller;

import com.arsiwooqq.orderservice.dto.*;
import com.arsiwooqq.orderservice.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/orders")
public class OrderController {
    private final OrderService orderService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN') or @securityService.canCreateOrder(authentication.principal, #request)")
    public ResponseEntity<ApiResponse<OrderResponse>> create(@RequestBody @Valid OrderCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Order created", orderService.create(request)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @securityService.canAccessOrder(authentication.principal, #id)")
    public ResponseEntity<ApiResponse<OrderResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Order found", orderService.getById(id)));
    }

    @GetMapping(params = "ids")
    @PreAuthorize("hasRole('ADMIN') or @securityService.canAccessOrders(authentication.principal, #ids)")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getAllByIdsIn(
            @RequestParam(required = false) List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success("Orders found"));
        }
        return ResponseEntity.ok(ApiResponse.success("Orders found", orderService.getAllByIds(ids)));
    }

    @GetMapping(params = "statuses")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getAllByStatusesIn(
            @RequestParam(required = false) List<String> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success("Orders found"));
        }
        return ResponseEntity.ok(ApiResponse.success("Orders found", orderService.getAllByStatuses(statuses)));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @securityService.canManageOrder(authentication.principal, #id)")
    public ResponseEntity<ApiResponse<OrderResponse>> update(
            @PathVariable UUID id,
            @RequestBody @Valid OrderUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Order updated", orderService.update(id, request)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> changeOrderStatus(
            @PathVariable UUID id,
            @RequestBody @Valid ChangeOrderStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Order status changed", orderService.changeStatus(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @securityService.canManageOrder(authentication.principal, #id)")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        orderService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Order deleted"));
    }
}
