CREATE TABLE customer_status (
    id   INT PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE
);
INSERT INTO customer_status (id, code) VALUES (1, 'ACTIVE'), (2, 'INACTIVE');

ALTER TABLE customer ADD COLUMN status_id INT REFERENCES customer_status (id);
