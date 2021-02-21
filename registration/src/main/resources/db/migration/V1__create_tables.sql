CREATE TABLE user_registration (
  id VARCHAR(255) PRIMARY KEY,
  user_handle VARCHAR(255) UNIQUE,
  data TEXT NOT NULL
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