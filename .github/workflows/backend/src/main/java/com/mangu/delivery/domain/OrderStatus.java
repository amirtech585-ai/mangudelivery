package com.mangu.delivery.domain;

/** Order lifecycle (Documentation Section 8). Digital payment states are added with the payment phase. */
public enum OrderStatus {
    PAID,
    SELLER_CONFIRMED,
    READY_FOR_PICKUP,
    RIDER_ASSIGNED,
    PICKED_UP,
    ON_THE_WAY,
    DELIVERED,
    COMPLETED,
    SELLER_REJECTED,
    BUYER_CANCELLED,
    ADMIN_CANCELLED,
    DELIVERY_FAILED
}
