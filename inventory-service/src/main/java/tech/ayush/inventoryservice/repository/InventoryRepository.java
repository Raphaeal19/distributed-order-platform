package tech.ayush.inventoryservice.repository;

import tech.ayush.inventoryservice.model.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    
    Optional<Inventory> findByProductId(String productId);
    
    boolean existsByProductId(String productId);
}