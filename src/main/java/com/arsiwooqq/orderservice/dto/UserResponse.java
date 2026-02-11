package com.arsiwooqq.orderservice.dto;

import java.time.LocalDate;

public record UserResponse(
        String userId,
        String name,
        String surname,
        LocalDate birthDate,
        String email
) {
}
