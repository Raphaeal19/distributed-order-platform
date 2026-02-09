package tech.ayush.orderservice.dto;

import tech.ayush.orderservice.model.OrderStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderEvent {
    private Long orderId;
    private String customerId;
    private String productId;
    private Integer quantity;
    private Double totalPrice;
    private OrderStatus status;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
    
    private String eventType; // ORDER_CREATED, ORDER_UPDATED, ORDER_CANCELLED
}