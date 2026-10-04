package com.zaalima.ecommerce.events;

public record PaymentProcessedEvent(
        String paymentId,
        String orderId,
        String userId,
        double amount,
        long timestamp
) {}
