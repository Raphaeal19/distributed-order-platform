package tech.ayush.orderservice.service;

import tech.ayush.orderservice.dto.InventoryEvent;
import tech.ayush.orderservice.model.OrderStatus;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryEventConsumer {

  private final OrderService orderService;

  @KafkaListener(topics = "inventory.events", groupId = "order-service-group")
  public void handleInventoryEvent(@Payload InventoryEvent event) {
    log.info("Received inventory event: {}", event);

    if ("INVENTORY_RESERVED".equals(event.getEventType())) {
      orderService.updateOrderStatus(
          event.getOrderId(),
          OrderStatus.CONFIRMED);
    } else if ("INVENTORY_INSUFFICIENT".equals(event.getEventType())) {
      orderService.updateOrderStatus(
          event.getOrderId(),
          OrderStatus.CANCELLED);
    }
  }
}
