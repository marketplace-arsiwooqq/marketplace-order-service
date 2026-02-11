package com.arsiwooqq.orderservice.enums;

public enum OrderStatus {
    CREATED,
    PAID,
    SHIPPED,
    DELIVERED,
    RETURNED,
    REFUNDED,
    FAILED,
    CANCELED;

    public static OrderStatus fromString(String status) {
        for (OrderStatus value : OrderStatus.values()) {
            if (value.name().equalsIgnoreCase(status)) {
                return value;
            }
        }
        return null;
    }
}
