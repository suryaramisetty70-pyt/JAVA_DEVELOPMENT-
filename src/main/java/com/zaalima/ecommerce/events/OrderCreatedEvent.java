package com.zaalima.ecommerce.events;

public record OrderCreatedEvent(
        String orderId,
        String userId,
        String productId,
        int quantity,
        double price,
        long timestamp
) {}
