# Distributed Event-Driven Order & Analytics Platform

A production-ready microservices platform demonstrating event-driven architecture, asynchronous communication, and distributed systems design patterns using Spring Boot, Apache Kafka, and PostgreSQL.

## Architecture
```
Client → Order Service → Kafka → Inventory Service
            ↓                        ↓
        PostgreSQL              PostgreSQL
```

## Features

- **Event-Driven Architecture**: Asynchronous communication via Apache Kafka
- **Microservices**: Independently deployable services with separate databases
- **RESTful APIs**: Clean, well-documented endpoints
- **Transaction Management**: ACID compliance with Spring @Transactional
- **Docker Support**: Containerized infrastructure (Kafka, Zookeeper, PostgreSQL)
- **Database-Per-Service Pattern**: Service autonomy and fault isolation

## Tech Stack

- **Backend**: Java 17+, Spring Boot 3.4.0
- **Messaging**: Apache Kafka 7.4.0
- **Database**: PostgreSQL 18
- **ORM**: Spring Data JPA (Hibernate)
- **Build Tool**: Maven
- **Containerization**: Docker & Docker Compose

## Prerequisites

- JDK 17 or higher
- Docker Desktop
- Maven 3.6+
- PostgreSQL 18 (or use Docker)

## Quick Start

### 1. Start Infrastructure
```bash
docker-compose up -d
```

### 2. Run Order Service
```bash
cd order-service
mvn spring-boot:run
```

### 3. Run Inventory Service
```bash
cd inventory-service
mvn spring-boot:run
```

### 4. Test the System
```bash
# Create an order
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST001",
    "productId": "PROD123",
    "quantity": 5,
    "totalPrice": 249.99
  }'

# Check inventory
curl http://localhost:8081/api/inventory/PROD123
```

## API Endpoints

### Order Service (Port 8080)

- `POST /api/orders` - Create new order
- `GET /api/orders` - Get all orders
- `GET /api/orders/{id}` - Get order by ID
- `GET /api/orders/customer/{customerId}` - Get orders by customer
- `PATCH /api/orders/{id}/status?status=CONFIRMED` - Update order status

### Inventory Service (Port 8081)

- `GET /api/inventory/{productId}` - Get inventory for product
- `GET /api/inventory` - Get all inventory
- `POST /api/inventory?productId=X&initialStock=100` - Create inventory

## Event Flow

1. Client sends POST request to Order Service
2. Order Service persists order to PostgreSQL
3. Order Service publishes `ORDER_CREATED` event to Kafka topic `order.events`
4. Inventory Service consumes event from Kafka
5. Inventory Service updates inventory in its PostgreSQL database


## Monitoring Kafka
```bash
# List topics
docker exec -it kafka kafka-topics --list --bootstrap-server localhost:9092

# View messages
docker exec -it kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic order.events \
  --from-beginning

# Check consumer groups
docker exec -it kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --describe \
  --group inventory-service-group
```

## Testing
```bash
# Run tests
mvn test

# Build
mvn clean install
```

## License

MIT

## Author

Ayush Pathak

## Contributing

Contributions welcome! Please open an issue or PR.