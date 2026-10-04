package com.zaalima.ecommerce.events;

public record OrderConfirmedEvent(
        String orderId,
        String userId,
        String paymentId,
        long timestamp
) {}
