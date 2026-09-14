CREATE TABLE ticket (
    id BIGINT PRIMARY KEY,
    status VARCHAR(20) NOT NULL,
    assignee VARCHAR(100) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    resolution VARCHAR(255)
);
