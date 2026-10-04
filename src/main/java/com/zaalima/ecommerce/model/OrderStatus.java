package com.zaalima.ecommerce.model;

public enum OrderStatus {
    PENDING,
    INVENTORY_RESERVED,
    INVENTORY_FAILED,
    PAYMENT_PROCESSED,
    PAYMENT_FAILED,
    CONFIRMED,
    CANCELLED
}
