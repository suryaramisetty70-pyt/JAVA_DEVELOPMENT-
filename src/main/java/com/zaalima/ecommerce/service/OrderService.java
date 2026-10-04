package com.zaalima.ecommerce.service;

import com.zaalima.ecommerce.events.*;
import com.zaalima.ecommerce.model.Order;
import com.zaalima.ecommerce.model.OrderStatus;
import com.zaalima.ecommerce.repository.OrderRepository;
import com.zaalima.ecommerce.saga.SagaEventDispatcher;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final SagaEventDispatcher dispatcher;

    public OrderService(OrderRepository orderRepository, SagaEventDispatcher dispatcher) {
        this.orderRepository = orderRepository;
        this.dispatcher = dispatcher;
    }

    @PostConstruct
    public void init() {
        dispatcher.onPaymentProcessed(this::handlePaymentProcessed);
        dispatcher.onInventoryFailed(this::handleInventoryFailed);
        dispatcher.onPaymentFailed(this::handlePaymentFailed);
    }

    /**
     * Step 1: Create Order in PENDING status and initiate Saga.
     */
    @Transactional
    public Order createOrder(String userId, String productId, int quantity, double price) {
        String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8);
        Order order = new Order(orderId, userId, productId, quantity, price);
        orderRepository.save(order);

        log.info("[ORDER SERVICE] Created Order #{} in PENDING state for User {}", orderId, userId);

        // Initiate Saga
        dispatcher.publishOrderCreated(new OrderCreatedEvent(
                orderId, userId, productId, quantity, price, System.currentTimeMillis()
        ));

        return order;
    }

    /**
     * Saga Step Success: Payment processed -> Confirm Order.
     */
    @Transactional
    public void handlePaymentProcessed(PaymentProcessedEvent event) {
        orderRepository.findById(event.orderId()).ifPresent(order -> {
            order.setStatus(OrderStatus.CONFIRMED);
            order.setPaymentId(event.paymentId());
            orderRepository.save(order);

            log.info("[ORDER SERVICE - SAGA COMPLETED] Order #{} CONFIRMED successfully!", order.getOrderId());
            dispatcher.publishOrderConfirmed(new OrderConfirmedEvent(
                    order.getOrderId(), order.getUserId(), event.paymentId(), System.currentTimeMillis()
            ));
        });
    }

    /**
     * Saga Failure: Inventory out of stock -> Cancel Order.
     */
    @Transactional
    public void handleInventoryFailed(InventoryFailedEvent event) {
        orderRepository.findById(event.orderId()).ifPresent(order -> {
            order.setStatus(OrderStatus.CANCELLED);
            order.setFailureReason("Inventory reservation failed: " + event.reason());
            orderRepository.save(order);

            log.warn("[ORDER SERVICE - SAGA FAILED] Order #{} CANCELLED due to inventory failure", order.getOrderId());
            dispatcher.publishOrderCancelled(new OrderCancelledEvent(
                    order.getOrderId(), order.getUserId(), event.reason(), System.currentTimeMillis()
            ));
        });
    }

    /**
     * Saga Failure: Payment failed -> Trigger Compensating Rollback and Cancel Order.
     */
    @Transactional
    public void handlePaymentFailed(PaymentFailedEvent event) {
        orderRepository.findById(event.orderId()).ifPresent(order -> {
            order.setStatus(OrderStatus.CANCELLED);
            order.setFailureReason("Payment failed: " + event.reason());
            orderRepository.save(order);

            log.warn("[ORDER SERVICE - SAGA FAILED] Order #{} CANCELLED due to payment failure", order.getOrderId());
            dispatcher.publishOrderCancelled(new OrderCancelledEvent(
                    order.getOrderId(), order.getUserId(), event.reason(), System.currentTimeMillis()
            ));
        });
    }

    public Optional<Order> getOrder(String orderId) {
        return orderRepository.findById(orderId);
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }
}
