package com.zaalima.ecommerce.saga;

import com.zaalima.ecommerce.events.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Event-Driven Message Dispatcher simulating Apache Kafka topic channels
 * for the Choreography-based Saga pattern.
 */
@Component
public class SagaEventDispatcher {
    private static final Logger log = LoggerFactory.getLogger(SagaEventDispatcher.class);

    private final List<Consumer<OrderCreatedEvent>> orderCreatedListeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<InventoryReservedEvent>> inventoryReservedListeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<InventoryFailedEvent>> inventoryFailedListeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<PaymentProcessedEvent>> paymentProcessedListeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<PaymentFailedEvent>> paymentFailedListeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<OrderConfirmedEvent>> orderConfirmedListeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<OrderCancelledEvent>> orderCancelledListeners = new CopyOnWriteArrayList<>();

    public void publishOrderCreated(OrderCreatedEvent event) {
        log.info("[KAFKA: orders-topic] Publishing OrderCreatedEvent for Order #{}", event.orderId());
        orderCreatedListeners.forEach(listener -> listener.accept(event));
    }

    public void publishInventoryReserved(InventoryReservedEvent event) {
        log.info("[KAFKA: inventory-topic] Publishing InventoryReservedEvent for Order #{}", event.orderId());
        inventoryReservedListeners.forEach(listener -> listener.accept(event));
    }

    public void publishInventoryFailed(InventoryFailedEvent event) {
        log.warn("[KAFKA: inventory-topic] Publishing InventoryFailedEvent for Order #{}: {}", event.orderId(), event.reason());
        inventoryFailedListeners.forEach(listener -> listener.accept(event));
    }

    public void publishPaymentProcessed(PaymentProcessedEvent event) {
        log.info("[KAFKA: payments-topic] Publishing PaymentProcessedEvent for Order #{} (Payment #{})", event.orderId(), event.paymentId());
        paymentProcessedListeners.forEach(listener -> listener.accept(event));
    }

    public void publishPaymentFailed(PaymentFailedEvent event) {
        log.warn("[KAFKA: payments-topic] Publishing PaymentFailedEvent for Order #{}: {}", event.orderId(), event.reason());
        paymentFailedListeners.forEach(listener -> listener.accept(event));
    }

    public void publishOrderConfirmed(OrderConfirmedEvent event) {
        log.info("[KAFKA: orders-topic] Publishing OrderConfirmedEvent for Order #{}", event.orderId());
        orderConfirmedListeners.forEach(listener -> listener.accept(event));
    }

    public void publishOrderCancelled(OrderCancelledEvent event) {
        log.warn("[KAFKA: orders-topic] Publishing OrderCancelledEvent for Order #{}: {}", event.orderId(), event.reason());
        orderCancelledListeners.forEach(listener -> listener.accept(event));
    }

    // Listener Registration
    public void onOrderCreated(Consumer<OrderCreatedEvent> listener) { orderCreatedListeners.add(listener); }
    public void onInventoryReserved(Consumer<InventoryReservedEvent> listener) { inventoryReservedListeners.add(listener); }
    public void onInventoryFailed(Consumer<InventoryFailedEvent> listener) { inventoryFailedListeners.add(listener); }
    public void onPaymentProcessed(Consumer<PaymentProcessedEvent> listener) { paymentProcessedListeners.add(listener); }
    public void onPaymentFailed(Consumer<PaymentFailedEvent> listener) { paymentFailedListeners.add(listener); }
    public void onOrderConfirmed(Consumer<OrderConfirmedEvent> listener) { orderConfirmedListeners.add(listener); }
    public void onOrderCancelled(Consumer<OrderCancelledEvent> listener) { orderCancelledListeners.add(listener); }
}
