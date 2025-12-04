
DROP TABLE IF EXISTS unit_events CASCADE;
DROP TABLE IF EXISTS payments CASCADE;
DROP TABLE IF EXISTS bookings CASCADE;
DROP TABLE IF EXISTS units CASCADE;
DROP TABLE IF EXISTS users CASCADE;

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE units (
    id BIGSERIAL PRIMARY KEY,
    rooms INT NOT NULL,
    type VARCHAR(50) NOT NULL,
    floor INT NOT NULL,
    description VARCHAR(1024) NOT NULL,
    base_cost NUMERIC(10,2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE bookings (
    id BIGSERIAL PRIMARY KEY,
    unit_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(50) NOT NULL,
    total_cost NUMERIC(10,2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL UNIQUE,
    status VARCHAR(50) NOT NULL,
    paid_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE unit_events (
    id BIGSERIAL PRIMARY KEY,
    unit_id BIGINT NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    details VARCHAR(1024),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE bookings
    ADD CONSTRAINT fk_bookings_unit FOREIGN KEY (unit_id) REFERENCES units(id),
    ADD CONSTRAINT fk_bookings_user FOREIGN KEY (user_id) REFERENCES users(id);

ALTER TABLE payments
    ADD CONSTRAINT fk_payments_booking FOREIGN KEY (booking_id) REFERENCES bookings(id);

ALTER TABLE unit_events
    ADD CONSTRAINT fk_unit_events_unit FOREIGN KEY (unit_id) REFERENCES units(id);

CREATE INDEX idx_units_type ON units(type);
CREATE INDEX idx_bookings_unit_dates ON bookings(unit_id, start_date, end_date);

-- seed users
INSERT INTO users (id, name, email) VALUES
 (1, 'Alice Booker', 'alice@example.com'),
 (2, 'Bob Planner', 'bob@example.com');

-- seed 10 fixed units
INSERT INTO units (id, rooms, type, floor, description, base_cost, created_at) VALUES
 (1, 2, 'HOME', 1, 'Cozy cottage with garden view', 120.00, CURRENT_TIMESTAMP),
 (2, 3, 'FLAT', 4, 'Modern flat near city center', 180.00, CURRENT_TIMESTAMP),
 (3, 1, 'APARTMENTS', 6, 'Studio with skyline', 90.00, CURRENT_TIMESTAMP),
 (4, 4, 'HOME', 2, 'Family house with backyard', 200.00, CURRENT_TIMESTAMP),
 (5, 2, 'FLAT', 8, 'Open space loft', 150.00, CURRENT_TIMESTAMP),
 (6, 3, 'APARTMENTS', 10, 'Penthouse with terrace', 260.00, CURRENT_TIMESTAMP),
 (7, 1, 'HOME', 1, 'Small guest house', 80.00, CURRENT_TIMESTAMP),
 (8, 2, 'FLAT', 5, 'Minimalist apartment', 140.00, CURRENT_TIMESTAMP),
 (9, 5, 'HOME', 3, 'Large villa with pool', 320.00, CURRENT_TIMESTAMP),
 (10, 2, 'APARTMENTS', 7, 'Two bedroom apartment', 170.00, CURRENT_TIMESTAMP);

INSERT INTO unit_events (unit_id, event_type, details, created_at) VALUES
 (1, 'CREATED', 'Seed data creation from changelog', CURRENT_TIMESTAMP),
 (2, 'CREATED', 'Seed data creation from changelog', CURRENT_TIMESTAMP),
 (3, 'CREATED', 'Seed data creation from changelog', CURRENT_TIMESTAMP),
 (4, 'CREATED', 'Seed data creation from changelog', CURRENT_TIMESTAMP),
 (5, 'CREATED', 'Seed data creation from changelog', CURRENT_TIMESTAMP),
 (6, 'CREATED', 'Seed data creation from changelog', CURRENT_TIMESTAMP),
 (7, 'CREATED', 'Seed data creation from changelog', CURRENT_TIMESTAMP),
 (8, 'CREATED', 'Seed data creation from changelog', CURRENT_TIMESTAMP),
 (9, 'CREATED', 'Seed data creation from changelog', CURRENT_TIMESTAMP),
 (10, 'CREATED', 'Seed data creation from changelog', CURRENT_TIMESTAMP);

-- seed 90 deterministic units and events (ids 11-100)
WITH seed_units AS (
    INSERT INTO units (id, rooms, type, floor, description, base_cost, created_at)
    SELECT
        i,
        (i % 5) + 1,
        CASE (i % 3) WHEN 0 THEN 'HOME' WHEN 1 THEN 'FLAT' ELSE 'APARTMENTS' END,
        (i % 15) + 1,
        'Auto-generated unit ' || i,
        ROUND((50 + (i * 3))::numeric, 2),
        CURRENT_TIMESTAMP
    FROM generate_series(11, 100) AS i
    RETURNING id
)
INSERT INTO unit_events (unit_id, event_type, details, created_at)
SELECT id, 'CREATED', 'Seeded at startup via changelog', CURRENT_TIMESTAMP
FROM seed_units;

-- reset sequences
SELECT setval('users_id_seq', (SELECT MAX(id) FROM users), true);
SELECT setval('units_id_seq', (SELECT MAX(id) FROM units), true);
SELECT setval('bookings_id_seq', 1, false);
SELECT setval('payments_id_seq', 1, false);
SELECT setval('unit_events_id_seq', (SELECT MAX(id) FROM unit_events), true);
