-- 1. Roles table
CREATE TABLE roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

-- 2. Users table
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    phone VARCHAR(20),
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 3. User Roles join table
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

-- 4. Airlines table
CREATE TABLE airlines (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(3) NOT NULL UNIQUE,
    country VARCHAR(100)
);

-- 5. Aircraft table
CREATE TABLE aircraft (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    model VARCHAR(100) NOT NULL,
    tail_number VARCHAR(20) NOT NULL UNIQUE,
    capacity INT NOT NULL,
    airline_id BIGINT NOT NULL,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    FOREIGN KEY (airline_id) REFERENCES airlines(id) ON DELETE CASCADE
);

-- 6. Routes table
CREATE TABLE routes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    origin VARCHAR(10) NOT NULL,
    destination VARCHAR(10) NOT NULL,
    distance_miles INT NOT NULL,
    duration_minutes INT NOT NULL,
    CONSTRAINT uq_route UNIQUE (origin, destination)
);

-- 7. Flights table
CREATE TABLE flights (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    flight_number VARCHAR(20) NOT NULL UNIQUE,
    route_id BIGINT NOT NULL,
    aircraft_id BIGINT NOT NULL,
    departure_time TIMESTAMP NOT NULL,
    arrival_time TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    base_price DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (route_id) REFERENCES routes(id) ON DELETE CASCADE,
    FOREIGN KEY (aircraft_id) REFERENCES aircraft(id) ON DELETE CASCADE
);

-- 8. Coupons table
CREATE TABLE coupons (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    discount_type VARCHAR(20) NOT NULL,
    discount_value DECIMAL(10,2) NOT NULL,
    max_discount DECIMAL(10,2),
    min_order_value DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    usage_limit INT,
    used_count INT DEFAULT 0,
    expiration_date TIMESTAMP
);

-- 9. Bookings table
CREATE TABLE bookings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    flight_id BIGINT NOT NULL,
    booking_reference VARCHAR(10) NOT NULL UNIQUE,
    booking_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    total_price DECIMAL(10,2) NOT NULL,
    coupon_id BIGINT,
    booking_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (flight_id) REFERENCES flights(id) ON DELETE CASCADE,
    FOREIGN KEY (coupon_id) REFERENCES coupons(id) ON DELETE SET NULL
);

-- 10. Passengers table
CREATE TABLE passengers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id BIGINT NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(100),
    passport_number VARCHAR(20),
    seat_number VARCHAR(10) NOT NULL,
    FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE
);

-- 11. Tickets table
CREATE TABLE tickets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    passenger_id BIGINT NOT NULL UNIQUE,
    ticket_number VARCHAR(20) NOT NULL UNIQUE,
    ticket_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    issued_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (passenger_id) REFERENCES passengers(id) ON DELETE CASCADE
);

-- 12. Payments table
CREATE TABLE payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id BIGINT NOT NULL UNIQUE,
    transaction_reference VARCHAR(50) NOT NULL UNIQUE,
    payment_method VARCHAR(20) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE
);

-- 13. Baggage table
CREATE TABLE baggage (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    passenger_id BIGINT NOT NULL,
    bag_tag VARCHAR(20) NOT NULL UNIQUE,
    weight_kg DECIMAL(5,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'CHECKED_IN',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (passenger_id) REFERENCES passengers(id) ON DELETE CASCADE
);

-- 14. Loyalty Points table
CREATE TABLE loyalty_points (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    current_balance INT DEFAULT 0,
    tier VARCHAR(20) DEFAULT 'BRONZE',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 15. Loyalty Transactions table
CREATE TABLE loyalty_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    loyalty_points_id BIGINT NOT NULL,
    points INT NOT NULL,
    type VARCHAR(20) NOT NULL,
    transaction_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (loyalty_points_id) REFERENCES loyalty_points(id) ON DELETE CASCADE
);

-- 16. Notifications table
CREATE TABLE notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(150) NOT NULL,
    message TEXT NOT NULL,
    channel VARCHAR(20) DEFAULT 'EMAIL',
    status VARCHAR(20) DEFAULT 'PENDING',
    sent_at TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 17. Audit Logs table
CREATE TABLE audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    action VARCHAR(100) NOT NULL,
    entity_name VARCHAR(50) NOT NULL,
    entity_id BIGINT,
    change_log TEXT,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

-- --- High-Traffic Composite Indexes ---

-- Composite index for Route searches (e.g. Origin -> Destination)
CREATE INDEX idx_routes_origin_dest ON routes(origin, destination);

-- Composite index for Flight searches (by Route, Departure Time, and Status)
CREATE INDEX idx_flights_route_time ON flights(route_id, departure_time, status);

-- Composite index for User Booking history
CREATE INDEX idx_bookings_user_status ON bookings(user_id, booking_status);

-- Composite index for Flight Occupancy checking
CREATE INDEX idx_bookings_flight_status ON bookings(flight_id, booking_status);

-- Composite index for Loyalty Transaction history
CREATE INDEX idx_loyalty_tx_points_date ON loyalty_transactions(loyalty_points_id, transaction_date);

-- Composite index for Passenger lists per Booking
CREATE INDEX idx_passengers_booking ON passengers(booking_id);

-- Composite index for audit checks
CREATE INDEX idx_audit_entity ON audit_logs(entity_name, entity_id);
