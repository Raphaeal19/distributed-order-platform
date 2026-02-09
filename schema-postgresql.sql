-- =====================================================
-- PRODUCT SERVICE SCHEMA (PostgreSQL)
-- =====================================================

-- Products table
CREATE TABLE IF NOT EXISTS products (
    product_id BIGSERIAL PRIMARY KEY,
    product_code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    category VARCHAR(50) NOT NULL,
    base_price DECIMAL(10,2) NOT NULL CHECK (base_price >= 0),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT true
);

-- Indexes
CREATE INDEX idx_product_category ON products(category, is_active);
CREATE INDEX idx_product_code ON products(product_code);

-- =====================================================
-- INVENTORY SERVICE SCHEMA
-- =====================================================

CREATE TABLE IF NOT EXISTS inventory (
    inventory_id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(product_id),
    warehouse_code VARCHAR(20) NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity >= 0),
    reserved_quantity INTEGER DEFAULT 0 CHECK (reserved_quantity >= 0),
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version INTEGER DEFAULT 0,
    UNIQUE(product_id, warehouse_code)
);

CREATE INDEX idx_inventory_product ON inventory(product_id);
CREATE INDEX idx_inventory_warehouse ON inventory(warehouse_code);

-- Inventory audit trail
CREATE TABLE IF NOT EXISTS inventory_audit (
    audit_id BIGSERIAL PRIMARY KEY,
    inventory_id BIGINT NOT NULL REFERENCES inventory(inventory_id),
    action VARCHAR(20) NOT NULL,
    old_quantity INTEGER,
    new_quantity INTEGER,
    changed_by VARCHAR(100),
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- ORDER SERVICE SCHEMA
-- =====================================================

CREATE TABLE IF NOT EXISTS orders (
    order_id BIGSERIAL PRIMARY KEY,
    order_number VARCHAR(50) UNIQUE NOT NULL,
    customer_id BIGINT NOT NULL,
    order_status VARCHAR(20) NOT NULL CHECK (order_status IN ('PENDING', 'CONFIRMED', 'SHIPPED', 'DELIVERED', 'CANCELLED')),
    total_amount DECIMAL(10,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS order_items (
    order_item_id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(order_id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products(product_id),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    unit_price DECIMAL(10,2) NOT NULL,
    subtotal DECIMAL(10,2) NOT NULL
);

CREATE INDEX idx_order_customer ON orders(customer_id, order_status);
CREATE INDEX idx_order_status ON orders(order_status, created_at);
CREATE INDEX idx_order_items_order ON order_items(order_id);

-- Saga pattern state tracking
CREATE TABLE IF NOT EXISTS order_saga_state (
    saga_id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(order_id),
    current_step VARCHAR(50) NOT NULL,
    saga_status VARCHAR(20) NOT NULL CHECK (saga_status IN ('STARTED', 'COMPLETED', 'COMPENSATING', 'FAILED')),
    compensation_required BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- FUNCTIONS (PostgreSQL version)
-- =====================================================

-- Function to calculate available inventory
CREATE OR REPLACE FUNCTION get_available_inventory(
    p_product_id BIGINT,
    p_warehouse_code VARCHAR
) RETURNS INTEGER AS $$
DECLARE
    v_available INTEGER;
BEGIN
    SELECT quantity - reserved_quantity
    INTO v_available
    FROM inventory
    WHERE product_id = p_product_id
    AND warehouse_code = p_warehouse_code;
    
    RETURN COALESCE(v_available, 0);
EXCEPTION
    WHEN NO_DATA_FOUND THEN
        RETURN 0;
END;
$$ LANGUAGE plpgsql;

-- Function to reserve inventory
CREATE OR REPLACE FUNCTION reserve_inventory(
    p_product_id BIGINT,
    p_warehouse_code VARCHAR,
    p_quantity INTEGER,
    p_order_id BIGINT
) RETURNS BOOLEAN AS $$
DECLARE
    v_available INTEGER;
    v_current_version INTEGER;
    v_inventory_id BIGINT;
BEGIN
    -- Lock row and get current state
    SELECT inventory_id, quantity - reserved_quantity, version
    INTO v_inventory_id, v_available, v_current_version
    FROM inventory
    WHERE product_id = p_product_id
    AND warehouse_code = p_warehouse_code
    FOR UPDATE;
    
    IF v_available >= p_quantity THEN
        -- Update with version check
        UPDATE inventory
        SET reserved_quantity = reserved_quantity + p_quantity,
            version = version + 1,
            last_updated = CURRENT_TIMESTAMP
        WHERE inventory_id = v_inventory_id
        AND version = v_current_version;
        
        IF FOUND THEN
            -- Log reservation
            INSERT INTO inventory_audit (inventory_id, action, old_quantity, new_quantity, changed_by)
            VALUES (v_inventory_id, 'RESERVE', v_available, v_available - p_quantity, 'ORDER_' || p_order_id);
            
            RETURN true;
        END IF;
    END IF;
    
    RETURN false;
END;
$$ LANGUAGE plpgsql;

-- Trigger to update order total
CREATE OR REPLACE FUNCTION update_order_total()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE orders
    SET total_amount = (
        SELECT COALESCE(SUM(subtotal), 0)
        FROM order_items
        WHERE order_id = COALESCE(NEW.order_id, OLD.order_id)
    ),
    updated_at = CURRENT_TIMESTAMP
    WHERE order_id = COALESCE(NEW.order_id, OLD.order_id);
    
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_update_order_total
AFTER INSERT OR UPDATE OR DELETE ON order_items
FOR EACH ROW
EXECUTE FUNCTION update_order_total();

-- =====================================================
-- MATERIALIZED VIEW
-- =====================================================

CREATE MATERIALIZED VIEW mv_product_sales_summary AS
SELECT 
    p.product_id,
    p.product_code,
    p.name,
    p.category,
    COUNT(DISTINCT o.order_id) as total_orders,
    COALESCE(SUM(oi.quantity), 0) as total_quantity_sold,
    COALESCE(SUM(oi.subtotal), 0) as total_revenue,
    MAX(o.created_at) as last_order_date
FROM products p
LEFT JOIN order_items oi ON p.product_id = oi.product_id
LEFT JOIN orders o ON oi.order_id = o.order_id
WHERE o.order_status IS NULL OR o.order_status != 'CANCELLED'
GROUP BY p.product_id, p.product_code, p.name, p.category;

CREATE INDEX idx_mv_category ON mv_product_sales_summary(category);

-- =====================================================
-- SAMPLE DATA
-- =====================================================

INSERT INTO products (product_code, name, description, category, base_price) VALUES
('ELEC-001', 'Laptop Pro 15', 'High-performance laptop', 'ELECTRONICS', 1299.99),
('ELEC-002', 'Wireless Mouse', 'Ergonomic wireless mouse', 'ELECTRONICS', 29.99),
('CLTH-001', 'Cotton T-Shirt', 'Comfortable cotton t-shirt', 'CLOTHING', 19.99);

INSERT INTO inventory (product_id, warehouse_code, quantity) VALUES 
(1, 'WH-US-EAST', 50),
(2, 'WH-US-EAST', 200),
(3, 'WH-US-WEST', 150);