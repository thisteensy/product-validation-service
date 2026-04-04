CREATE TABLE tracks (
    id               VARCHAR(36)   NOT NULL,
    product_id       VARCHAR(36)   NOT NULL,
    isrc             VARCHAR(12)   NOT NULL,
    title            VARCHAR(255)  NOT NULL,
    track_number     INT           NOT NULL,
    audio_file_uri   VARCHAR(500)  NOT NULL,
    duration         INT           NOT NULL,
    explicit         BOOLEAN       NOT NULL DEFAULT FALSE,
    contributors     JSON          NOT NULL,
    ownership_splits JSON          NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_track_product FOREIGN KEY (product_id) REFERENCES products (id)
);

ALTER TABLE products
    DROP COLUMN isrc,
    DROP COLUMN audio_file_uri,
    DROP COLUMN contributors,
    DROP COLUMN explicit;