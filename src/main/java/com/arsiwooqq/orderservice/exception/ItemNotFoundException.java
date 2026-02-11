package com.arsiwooqq.orderservice.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class ItemNotFoundException extends ApiException {
    public ItemNotFoundException(UUID id) {
        super("Item with id " + id + " not found", HttpStatus.NOT_FOUND);
    }
}
