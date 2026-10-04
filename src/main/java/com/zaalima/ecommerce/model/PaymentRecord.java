package com.zaalima.ecommerce.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "payments")
public class PaymentRecord {
    @Id
    private String paymentId;
    private String orderId;
    private String userId;
    private double amount;
    private String status; // SUCCESS, REFUNDED, FAILED
    private Instant createdAt;

    public PaymentRecord() {}

    public PaymentRecord(String paymentId, String orderId, String userId, double amount, String status) {
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.userId = userId;
        this.amount = amount;
        this.status = status;
        this.createdAt = Instant.now();
    }

    public String getPaymentId() { return paymentId; }
    public String getOrderId() { return orderId; }
    public String getUserId() { return userId; }
    public double getAmount() { return amount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
}
