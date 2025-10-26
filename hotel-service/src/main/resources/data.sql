-- Hotels
INSERT INTO hotels (id, name, address, description, star_rating) VALUES
(1, 'Grand Plaza Hotel', '123 Main Street, Downtown', 'Luxurious hotel in the heart of the city', 5),
(2, 'Seaside Resort', '456 Beach Road, Coastal Area', 'Beautiful beachfront resort with amazing views', 4),
(3, 'Mountain Lodge', '789 Forest Path, Mountain Region', 'Cozy lodge surrounded by nature', 3);

-- Rooms for Grand Plaza Hotel
INSERT INTO rooms (id, hotel_id, number, available, times_booked, room_type, price_per_night, capacity, version) VALUES
(1, 1, '101', true, 0, 'Standard', 120.00, 2, 0),
(2, 1, '102', true, 0, 'Standard', 120.00, 2, 0),
(3, 1, '201', true, 0, 'Deluxe', 180.00, 3, 0),
(4, 1, '202', true, 0, 'Deluxe', 180.00, 3, 0),
(5, 1, '301', true, 0, 'Suite', 300.00, 4, 0);

-- Rooms for Seaside Resort
INSERT INTO rooms (id, hotel_id, number, available, times_booked, room_type, price_per_night, capacity, version) VALUES
(6, 2, 'A101', true, 0, 'Ocean View', 200.00, 2, 0),
(7, 2, 'A102', true, 0, 'Ocean View', 200.00, 2, 0),
(8, 2, 'B201', true, 0, 'Beachfront Suite', 350.00, 4, 0),
(9, 2, 'B202', true, 0, 'Beachfront Suite', 350.00, 4, 0);

-- Rooms for Mountain Lodge
INSERT INTO rooms (id, hotel_id, number, available, times_booked, room_type, price_per_night, capacity, version) VALUES
(10, 3, 'C1', true, 0, 'Standard', 80.00, 2, 0),
(11, 3, 'C2', true, 0, 'Standard', 80.00, 2, 0),
(12, 3, 'D1', true, 0, 'Family Room', 150.00, 5, 0),
(13, 3, 'D2', true, 0, 'Family Room', 150.00, 5, 0);

-- Ensure identities continue after seeded IDs
ALTER TABLE hotels ALTER COLUMN id RESTART WITH 4;
ALTER TABLE rooms ALTER COLUMN id RESTART WITH 14;

