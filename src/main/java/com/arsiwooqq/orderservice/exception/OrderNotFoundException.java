package com.arsiwooqq.orderservice.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class OrderNotFoundException extends ApiException {
    public OrderNotFoundException(UUID id) {
        super("Order with id " + id + " not found", HttpStatus.NOT_FOUND);
    }
}
