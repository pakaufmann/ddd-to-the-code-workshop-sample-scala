CREATE TABLE wallet (
  id VARCHAR PRIMARY KEY,
  data TEXT
);

CREATE TABLE domain_event (
    id VARCHAR NOT NULL,
    topic TEXT NOT NULL,
    payload TEXT NOT NULL,
    published_at TIMESTAMP NOT NULL,
    UNIQUE KEY unique_domain_event_id (id)
);