package com.arsiwooqq.orderservice.controller;

import com.arsiwooqq.orderservice.dto.ApiResponse;
import com.arsiwooqq.orderservice.dto.ItemCreateRequest;
import com.arsiwooqq.orderservice.dto.ItemResponse;
import com.arsiwooqq.orderservice.dto.ItemUpdateRequest;
import com.arsiwooqq.orderservice.service.ItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/items")
public class ItemController {
    private final ItemService itemService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ItemResponse>> create(@RequestBody @Valid ItemCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Item created", itemService.create(request)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<ApiResponse<ItemResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Item found", itemService.getById(id)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<ApiResponse<Page<ItemResponse>>> getAll(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Items found", itemService.getAll(pageable)));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ItemResponse>> update(@PathVariable UUID id,
                                                            @RequestBody @Valid ItemUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Item updated", itemService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        itemService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Item deleted"));
    }
}
