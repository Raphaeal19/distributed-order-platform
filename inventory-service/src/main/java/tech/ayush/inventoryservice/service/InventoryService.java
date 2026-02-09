package tech.ayush.inventoryservice.service;

import tech.ayush.inventoryservice.dto.InventoryEvent;
import tech.ayush.inventoryservice.model.Inventory;
import tech.ayush.inventoryservice.model.ProcessedOrder;
import tech.ayush.inventoryservice.repository.InventoryRepository;
import tech.ayush.inventoryservice.repository.ProcessedOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

  private final InventoryRepository inventoryRepository;
  private static final Integer DEFAULT_STOCK = 100;
  private final KafkaTemplate<String, InventoryEvent> kafkaTemplate;
  private final ProcessedOrderRepository processedOrderRepository;

  @Transactional
  public void reserveInventory(Long orderId, String productId, Integer quantity) {
    log.info("Attempting to reserve {} units of product: {}", quantity, productId);

    if (processedOrderRepository.existsByOrderIdAndEventType(orderId, "ORDER_CREATED")) {
      log.info("Order {} already processed, skipping to ensure idempotency", orderId);
      return;
    }

    Inventory inventory = inventoryRepository.findByProductId(productId)
        .orElseGet(() -> createDefaultInventory(productId));

    if (inventory.getAvailableQuantity() >= quantity) {
      inventory.setAvailableQuantity(inventory.getAvailableQuantity() - quantity);
      inventory.setReservedQuantity(inventory.getReservedQuantity() + quantity);
      inventoryRepository.save(inventory);

      ProcessedOrder processedOrder = ProcessedOrder.builder()
          .orderId(orderId)
          .eventType("ORDER_CREATED")
          .build();
      processedOrderRepository.save(processedOrder);

      publishInventoryEvent(orderId, productId, quantity, "INVENTORY_RESERVED");

      log.info("Successfully reserved {} units of product {}. Available: {}, Reserved: {}",
          quantity, productId, inventory.getAvailableQuantity(), inventory.getReservedQuantity());
    } else {
      log.warn("Insufficient inventory for product {}. Available: {}, Requested: {}",
          productId, inventory.getAvailableQuantity(), quantity);
    }
  }

  @Transactional
  public void releaseInventory(Long orderId, String productId, Integer quantity) {
    Inventory inventory = inventoryRepository.findByProductId(productId)
        .orElseThrow(() -> new RuntimeException("Product not found: " + productId));

    inventory.setReservedQuantity(Math.max(0, inventory.getReservedQuantity() - quantity));
    inventory.setAvailableQuantity(inventory.getAvailableQuantity() + quantity);
    inventoryRepository.save(inventory);

      ProcessedOrder processedOrder = ProcessedOrder.builder()
          .orderId(orderId)
          .eventType("ORDER_CANCELLED")
          .build();
      processedOrderRepository.save(processedOrder);

    log.info("Released {} units of product {}", quantity, productId);
  }

  public Inventory getInventoryByProduct(String productId) {
    return inventoryRepository.findByProductId(productId)
        .orElseThrow(() -> new RuntimeException("Product not found: " + productId));
  }

  public List<Inventory> getAllInventory() {
    return inventoryRepository.findAll();
  }

  @Transactional
  public Inventory createInventory(String productId, Integer initialStock) {
    if (inventoryRepository.existsByProductId(productId)) {
      throw new RuntimeException("Inventory already exists for product: " + productId);
    }

    Inventory inventory = Inventory.builder()
        .productId(productId)
        .availableQuantity(initialStock)
        .reservedQuantity(0)
        .build();

    return inventoryRepository.save(inventory);
  }

  private Inventory createDefaultInventory(String productId) {
    log.info("Creating default inventory for product: {}", productId);

    Inventory inventory = Inventory.builder()
        .productId(productId)
        .availableQuantity(DEFAULT_STOCK)
        .reservedQuantity(0)
        .build();

    return inventoryRepository.save(inventory);
  }

  private void publishInventoryEvent(Long orderId, String productId, Integer quantity, String eventType) {
    InventoryEvent event = InventoryEvent.builder()
        .orderId(orderId)
        .productId(productId)
        .quantity(quantity)
        .eventType(eventType)
        .timestamp(LocalDateTime.now())
        .build();
    kafkaTemplate.send("inventory.events", orderId.toString(), event);
    log.info("Published {} event for order {}", eventType, orderId);
  }
}