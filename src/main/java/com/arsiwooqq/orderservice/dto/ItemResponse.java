package com.arsiwooqq.orderservice.dto;

import java.util.UUID;

public record ItemResponse(
        UUID id,
        String name,
        Long price
) {
}
