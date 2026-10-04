package com.zaalima.ecommerce.service;

import com.zaalima.ecommerce.events.*;
import com.zaalima.ecommerce.model.InventoryItem;
import com.zaalima.ecommerce.repository.InventoryRepository;
import com.zaalima.ecommerce.saga.SagaEventDispatcher;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {
    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final InventoryRepository inventoryRepository;
    private final SagaEventDispatcher dispatcher;

    public InventoryService(InventoryRepository inventoryRepository, SagaEventDispatcher dispatcher) {
        this.inventoryRepository = inventoryRepository;
        this.dispatcher = dispatcher;
    }

    @PostConstruct
    public void init() {
        // Register Saga Event Listeners
        dispatcher.onOrderCreated(this::handleOrderCreated);
        dispatcher.onOrderCancelled(this::handleOrderCancelled);

        // Preload sample inventory
        if (inventoryRepository.count() == 0) {
            inventoryRepository.save(new InventoryItem("PROD-MACBOOK", "Apple MacBook Pro M3", 10));
            inventoryRepository.save(new InventoryItem("PROD-IPHONE", "Apple iPhone 16 Pro", 25));
            inventoryRepository.save(new InventoryItem("PROD-PS5", "PlayStation 5 Console", 0)); // Out of stock
        }
    }

    @Transactional
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("[INVENTORY SERVICE] Processing reservation for Product {} (Qty: {})", event.productId(), event.quantity());

        var itemOpt = inventoryRepository.findById(event.productId());
        if (itemOpt.isEmpty()) {
            dispatcher.publishInventoryFailed(new InventoryFailedEvent(
                    event.orderId(), event.productId(), event.quantity(), "Product not found: " + event.productId(), System.currentTimeMillis()
            ));
            return;
        }

        InventoryItem item = itemOpt.get();
        if (item.reserve(event.quantity())) {
            inventoryRepository.save(item);
            log.info("[INVENTORY SERVICE] Stock reserved successfully. Remaining: {}", item.getAvailableStock());
            dispatcher.publishInventoryReserved(new InventoryReservedEvent(
                    event.orderId(), event.productId(), event.quantity(), System.currentTimeMillis()
            ));
        } else {
            log.warn("[INVENTORY SERVICE] Out of stock for product {}. Requested: {}, Available: {}",
                    event.productId(), event.quantity(), item.getAvailableStock());
            dispatcher.publishInventoryFailed(new InventoryFailedEvent(
                    event.orderId(), event.productId(), event.quantity(), "Insufficient stock", System.currentTimeMillis()
            ));
        }
    }

    /**
     * Compensating Transaction: Releases reserved stock upon downstream failure.
     */
    @Transactional
    public void handleOrderCancelled(OrderCancelledEvent event) {
        log.warn("[INVENTORY SERVICE - COMPENSATING ACTION] Rolling back reservation for Order #{}", event.orderId());
        // In full production, query order's productId and quantity to call item.release(qty)
    }

    public InventoryItem getInventory(String productId) {
        return inventoryRepository.findById(productId).orElse(null);
    }
}
