package tech.ayush.inventoryservice.repository;

import tech.ayush.inventoryservice.model.ProcessedOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessedOrderRepository extends JpaRepository<ProcessedOrder, Long> {
    boolean existsByOrderIdAndEventType(Long orderId, String eventType);
}