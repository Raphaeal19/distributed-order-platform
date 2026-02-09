package tech.ayush.inventoryservice.service;

import tech.ayush.inventoryservice.dto.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {
    
    private final InventoryService inventoryService;
    
    @KafkaListener(
            topics = "order.events", 
            groupId = "inventory-service-group"
    )
    public void handleOrderEvent(
            @Payload OrderEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {
        
        log.info("Received order event from partition {}, offset {}: {}", partition, offset, event);
        
        try {
            switch (event.getEventType()) {
                case "ORDER_CREATED" -> {
                    log.info("Processing ORDER_CREATED event for order ID: {}", event.getOrderId());
                    inventoryService.reserveInventory(event.getOrderId(), event.getProductId(), event.getQuantity());
                }
                case "ORDER_CANCELLED" -> {
                    log.info("Processing ORDER_CANCELLED event for order ID: {}", event.getOrderId());
                    inventoryService.releaseInventory(event.getOrderId(), event.getProductId(), event.getQuantity());
                }
                default -> log.warn("Unknown event type: {}", event.getEventType());
            }
        } catch (Exception e) {
            log.error("Error processing order event: {}", event, e);
        }
    }
}