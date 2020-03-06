CREATE TABLE `user` (
    id VARCHAR NOT NULL,
    data TEXT NOT NULL,
    UNIQUE KEY unique_user_id (id)
);

CREATE TABLE booking (
    id VARCHAR NOT NULL,
    data TEXT NOT NULL,
    UNIQUE KEY unique_booking_id (id)
);

CREATE TABLE domain_event (
    id VARCHAR NOT NULL,
    topic TEXT NOT NULL,
    payload TEXT NOT NULL,
    published_at TIMESTAMP NOT NULL,
    trace VARCHAR NOT NULL,
    UNIQUE KEY unique_domain_event_id (id)
);