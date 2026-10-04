package com.zaalima.ecommerce.model;

import jakarta.persistence.*;

@Entity
@Table(name = "inventory")
public class InventoryItem {
    @Id
    private String productId;
    private String productName;
    private int availableStock;
    private int reservedStock;

    public InventoryItem() {}

    public InventoryItem(String productId, String productName, int availableStock) {
        this.productId = productId;
        this.productName = productName;
        this.availableStock = availableStock;
        this.reservedStock = 0;
    }

    public synchronized boolean reserve(int qty) {
        if (availableStock >= qty) {
            availableStock -= qty;
            reservedStock += qty;
            return true;
        }
        return false;
    }

    public synchronized void release(int qty) {
        reservedStock = Math.max(0, reservedStock - qty);
        availableStock += qty;
    }

    public synchronized void confirm(int qty) {
        reservedStock = Math.max(0, reservedStock - qty);
    }

    public String getProductId() { return productId; }
    public String getProductName() { return productName; }
    public int getAvailableStock() { return availableStock; }
    public int getReservedStock() { return reservedStock; }
}
