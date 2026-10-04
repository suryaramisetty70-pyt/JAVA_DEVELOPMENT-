package com.zaalima.ecommerce;

import com.zaalima.ecommerce.model.Order;
import com.zaalima.ecommerce.model.OrderStatus;
import com.zaalima.ecommerce.repository.OrderRepository;
import com.zaalima.ecommerce.repository.PaymentRepository;
import com.zaalima.ecommerce.service.InventoryService;
import com.zaalima.ecommerce.service.NotificationService;
import com.zaalima.ecommerce.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class SagaIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private NotificationService notificationService;

    @Test
    @DisplayName("Saga Scenario 1: Successful Order -> Inventory Reserved -> Payment Processed -> Order Confirmed")
    void testSuccessfulSagaTransaction() {
        // Order: 1 MacBook @ $2,500
        Order order = orderService.createOrder("USER-101", "PROD-MACBOOK", 1, 2500.0);

        // Fetch updated state from DB
        Order updated = orderRepository.findById(order.getOrderId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(updated.getPaymentId()).isNotNull();

        // Verify Payment record
        var payment = paymentRepository.findByOrderId(order.getOrderId());
        assertThat(payment).isPresent();
        assertThat(payment.get().getStatus()).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("Saga Scenario 2: Out of Stock -> Inventory Fails -> Order Cancelled (Compensating rollback)")
    void testInventoryFailureSagaRollback() {
        // PS5 is initialized with 0 stock
        Order order = orderService.createOrder("USER-102", "PROD-PS5", 1, 500.0);

        Order updated = orderRepository.findById(order.getOrderId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(updated.getFailureReason()).contains("Insufficient stock");

        // No payment should have been created
        var payment = paymentRepository.findByOrderId(order.getOrderId());
        assertThat(payment).isEmpty();
    }

    @Test
    @DisplayName("Saga Scenario 3: Exceeds Payment Limit -> Payment Fails -> Compensating Action -> Order Cancelled")
    void testPaymentFailureSagaRollback() {
        // 5 MacBooks @ $2,500 = $12,500 (exceeds $10,000 threshold)
        Order order = orderService.createOrder("USER-103", "PROD-MACBOOK", 5, 2500.0);

        Order updated = orderRepository.findById(order.getOrderId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(updated.getFailureReason()).contains("Payment failed");

        // Payment marked as FAILED
        var payment = paymentRepository.findByOrderId(order.getOrderId());
        assertThat(payment).isPresent();
        assertThat(payment.get().getStatus()).isEqualTo("FAILED");
    }
}
