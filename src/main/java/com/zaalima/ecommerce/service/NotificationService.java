package com.zaalima.ecommerce.service;

import com.zaalima.ecommerce.events.OrderCancelledEvent;
import com.zaalima.ecommerce.events.OrderConfirmedEvent;
import com.zaalima.ecommerce.saga.SagaEventDispatcher;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

@Service
public class NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final SagaEventDispatcher dispatcher;
    private final AtomicInteger notificationCount = new AtomicInteger(0);

    public NotificationService(SagaEventDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @PostConstruct
    public void init() {
        dispatcher.onOrderConfirmed(this::sendOrderConfirmedNotification);
        dispatcher.onOrderCancelled(this::sendOrderCancelledNotification);
    }

    private void sendOrderConfirmedNotification(OrderConfirmedEvent event) {
        notificationCount.incrementAndGet();
        log.info("[NOTIFICATION SERVICE] 📲 SMS/Email sent to User {}: Your Order #{} is CONFIRMED! Payment ID: {}",
                event.userId(), event.orderId(), event.paymentId());
    }

    private void sendOrderCancelledNotification(OrderCancelledEvent event) {
        notificationCount.incrementAndGet();
        log.warn("[NOTIFICATION SERVICE] 📲 SMS/Email sent to User {}: Order #{} was CANCELLED. Reason: {}",
                event.userId(), event.orderId(), event.reason());
    }

    public int getNotificationCount() {
        return notificationCount.get();
    }
}
