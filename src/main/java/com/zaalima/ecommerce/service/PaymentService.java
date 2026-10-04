package com.zaalima.ecommerce.service;

import com.zaalima.ecommerce.events.*;
import com.zaalima.ecommerce.model.PaymentRecord;
import com.zaalima.ecommerce.repository.OrderRepository;
import com.zaalima.ecommerce.repository.PaymentRepository;
import com.zaalima.ecommerce.saga.SagaEventDispatcher;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PaymentService {
    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final SagaEventDispatcher dispatcher;

    public PaymentService(PaymentRepository paymentRepository, OrderRepository orderRepository, SagaEventDispatcher dispatcher) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.dispatcher = dispatcher;
    }

    @PostConstruct
    public void init() {
        dispatcher.onInventoryReserved(this::handleInventoryReserved);
        dispatcher.onOrderCancelled(this::handleOrderCancelled);
    }

    @Transactional
    @CircuitBreaker(name = "paymentGatewayCircuitBreaker", fallbackMethod = "paymentFallback")
    public void handleInventoryReserved(InventoryReservedEvent event) {
        log.info("[PAYMENT SERVICE] Ingested InventoryReservedEvent for Order #{}. Initiating charge...", event.orderId());

        var orderOpt = orderRepository.findById(event.orderId());
        if (orderOpt.isEmpty()) {
            dispatcher.publishPaymentFailed(new PaymentFailedEvent(
                    event.orderId(), "UNKNOWN", 0.0, "Order not found", System.currentTimeMillis()
            ));
            return;
        }

        var order = orderOpt.get();

        // Business rule simulation: Reject payment if amount > $10,000 (exceeds credit limit)
        if (order.getTotalAmount() > 10_000.0) {
            log.warn("[PAYMENT SERVICE] Payment rejected: Amount ${} exceeds card limit", order.getTotalAmount());
            PaymentRecord failedPayment = new PaymentRecord(
                    "PAY-" + UUID.randomUUID().toString().substring(0, 8),
                    order.getOrderId(),
                    order.getUserId(),
                    order.getTotalAmount(),
                    "FAILED"
            );
            paymentRepository.save(failedPayment);

            dispatcher.publishPaymentFailed(new PaymentFailedEvent(
                    order.getOrderId(), order.getUserId(), order.getTotalAmount(), "Card limit exceeded", System.currentTimeMillis()
            ));
            return;
        }

        // Process successful payment
        String paymentId = "PAY-" + UUID.randomUUID().toString().substring(0, 8);
        PaymentRecord payment = new PaymentRecord(paymentId, order.getOrderId(), order.getUserId(), order.getTotalAmount(), "SUCCESS");
        paymentRepository.save(payment);

        log.info("[PAYMENT SERVICE] Payment #{} successful for Order #{} ($%.2f)", paymentId, order.getOrderId(), order.getTotalAmount());
        dispatcher.publishPaymentProcessed(new PaymentProcessedEvent(
                paymentId, order.getOrderId(), order.getUserId(), order.getTotalAmount(), System.currentTimeMillis()
        ));
    }

    public void paymentFallback(InventoryReservedEvent event, Throwable t) {
        log.error("[PAYMENT SERVICE] Circuit breaker opened or payment gateway error: {}", t.getMessage());
        dispatcher.publishPaymentFailed(new PaymentFailedEvent(
                event.orderId(), "UNKNOWN", 0.0, "Payment gateway unavailable", System.currentTimeMillis()
        ));
    }

    /**
     * Compensating Transaction: Executes refund if Saga fails in subsequent steps.
     */
    @Transactional
    public void handleOrderCancelled(OrderCancelledEvent event) {
        paymentRepository.findByOrderId(event.orderId()).ifPresent(payment -> {
            if ("SUCCESS".equals(payment.getStatus())) {
                payment.setStatus("REFUNDED");
                paymentRepository.save(payment);
                log.warn("[PAYMENT SERVICE - COMPENSATING ACTION] Refunded ${} for Order #{} (Payment #{})",
                        payment.getAmount(), event.orderId(), payment.getPaymentId());
            }
        });
    }
}
