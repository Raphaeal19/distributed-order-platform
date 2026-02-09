package tech.ayush.orderservice.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryEvent {
  private Long orderId;
  private String productId;
  private Integer quantity;
  @JsonFormat(pattern = "yyy-MM-dd'T'HH:mm:ss")
  private LocalDateTime timestamp;

  private String eventType; // INVENTORY_RESERVED, INVENTORY_INSUFFICIENT
}
