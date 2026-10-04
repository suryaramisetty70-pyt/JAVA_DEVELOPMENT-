package com.zaalima.ecommerce.events;

public record OrderCancelledEvent(
        String orderId,
        String userId,
        String reason,
        long timestamp
) {}
