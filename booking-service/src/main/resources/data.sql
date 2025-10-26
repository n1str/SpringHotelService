-- Admin user (password: admin123)
INSERT INTO users (id, username, password, role, email, full_name, created_at) VALUES
(1, 'admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ADMIN', 'admin@hotel.com', 'System Administrator', CURRENT_TIMESTAMP);

-- Regular users (password: password123)
INSERT INTO users (id, username, password, role, email, full_name, created_at) VALUES
(2, 'john_doe', '$2a$10$e0MYzXyjpJS7Pd0RVvHwHe1CV5z3XJp1nJp1HhKp1HhKp1HhKp1HhK', 'USER', 'john@example.com', 'John Doe', CURRENT_TIMESTAMP),
(3, 'jane_smith', '$2a$10$e0MYzXyjpJS7Pd0RVvHwHe1CV5z3XJp1nJp1HhKp1HhKp1HhKp1HhK', 'USER', 'jane@example.com', 'Jane Smith', CURRENT_TIMESTAMP);

-- Ensure identity continues after seeded IDs
ALTER TABLE users ALTER COLUMN id RESTART WITH 4;

