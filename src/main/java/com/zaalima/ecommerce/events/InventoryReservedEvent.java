package com.zaalima.ecommerce.events;

public record InventoryReservedEvent(
        String orderId,
        String productId,
        int quantity,
        long timestamp
) {}
