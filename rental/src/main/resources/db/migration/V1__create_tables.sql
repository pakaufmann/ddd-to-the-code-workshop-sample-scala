CREATE TABLE rental_user (
    id VARCHAR(255) NOT NULL,
    data TEXT NOT NULL,
--    UNIQUE KEY unique_user_id (id)
    CONSTRAINT unique_user_id UNIQUE(id)
);

CREATE TABLE booking (
    id VARCHAR(255) NOT NULL,
    data TEXT NOT NULL,
--    UNIQUE KEY unique_booking_id (id)
    CONSTRAINT unique_booking_id UNIQUE(id)
);

CREATE TABLE domain_event (
    id VARCHAR(255) NOT NULL,
    topic TEXT NOT NULL,
    payload TEXT NOT NULL,
    published_at DATETIME NOT NULL,
    trace TEXT NOT NULL,
--    UNIQUE KEY unique_domain_event_id (id)
    CONSTRAINT unique_domain_event_id UNIQUE(id)
);