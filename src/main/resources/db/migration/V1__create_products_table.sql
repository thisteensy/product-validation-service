CREATE TABLE products (
    id VARCHAR(36) NOT NULL,
    upc VARCHAR(20),
    isrc VARCHAR(12),
    title VARCHAR(255),
    contributors JSON,
    release_date DATE,
    genre VARCHAR(100),
    explicit BOOLEAN DEFAULT FALSE,
    language VARCHAR(10),
    ownership_splits JSON,
    audio_file_uri VARCHAR(500),
    artwork_uri VARCHAR(500),
    dsp_targets JSON,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE INDEX idx_products_status ON products(status);