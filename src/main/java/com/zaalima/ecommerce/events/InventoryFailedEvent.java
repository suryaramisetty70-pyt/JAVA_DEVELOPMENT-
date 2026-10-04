package com.zaalima.ecommerce.events;

public record InventoryFailedEvent(
        String orderId,
        String productId,
        int requestedQuantity,
        String reason,
        long timestamp
) {}
