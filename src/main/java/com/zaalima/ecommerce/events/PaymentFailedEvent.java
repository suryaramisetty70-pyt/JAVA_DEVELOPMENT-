package com.zaalima.ecommerce.events;

public record PaymentFailedEvent(
        String orderId,
        String userId,
        double amount,
        String reason,
        long timestamp
) {}
