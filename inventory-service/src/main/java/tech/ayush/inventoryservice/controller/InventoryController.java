package tech.ayush.inventoryservice.controller;

import tech.ayush.inventoryservice.model.Inventory;
import tech.ayush.inventoryservice.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {
    
    private final InventoryService inventoryService;
    
    @GetMapping("/{productId}")
    public ResponseEntity<Inventory> getInventory(@PathVariable String productId) {
        Inventory inventory = inventoryService.getInventoryByProduct(productId);
        return ResponseEntity.ok(inventory);
    }
    
    @GetMapping
    public ResponseEntity<List<Inventory>> getAllInventory() {
        List<Inventory> inventories = inventoryService.getAllInventory();
        return ResponseEntity.ok(inventories);
    }
    
    @PostMapping
    public ResponseEntity<Inventory> createInventory(
            @RequestParam String productId,
            @RequestParam Integer initialStock) {
        Inventory inventory = inventoryService.createInventory(productId, initialStock);
        return ResponseEntity.status(HttpStatus.CREATED).body(inventory);
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Inventory Service is running");
    }
}